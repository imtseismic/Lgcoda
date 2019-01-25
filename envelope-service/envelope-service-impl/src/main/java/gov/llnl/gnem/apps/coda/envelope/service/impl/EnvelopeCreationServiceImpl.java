/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool. 
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.envelope.service.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.common.repository.WaveformRepository;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeCreationService;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeParamsService;
import llnl.gnem.core.util.Passband;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
public class EnvelopeCreationServiceImpl implements EnvelopeCreationService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeCreationServiceImpl.class);

    private WaveformToTimeSeriesConverter converter;

    private WaveformRepository waveformSvc;

    private NotificationService notification;

    private EnvelopeParamsService params;

    @Autowired
    public EnvelopeCreationServiceImpl(WaveformToTimeSeriesConverter converter, WaveformRepository waveformSvc, EnvelopeParamsService params, NotificationService notification) {
        this.converter = converter;
        this.waveformSvc = waveformSvc;
        this.params = params;
        this.notification = notification;
    }

    @Override
    public Result<List<Waveform>> createEnvelopes(Long sessionId, Collection<Waveform> waveforms, EnvelopeJobConfiguration envConf) {
        if (waveforms == null || waveforms.isEmpty()) {
            // TODO: Propagate warning to the status API            
            return new Result<List<Waveform>>(false, Collections.singletonList(new LightweightIllegalStateException("No waveforms provided; unable to compute envelopes.")), Collections.emptyList());
        }

        if (envConf == null) {
            envConf = params.getConfiguration();
        }

        if (envConf == null) {
            // TODO: Propagate warning to the status API            
            return new Result<List<Waveform>>(false,
                                              Collections.singletonList(
                                                      new LightweightIllegalStateException("No configuration specified but is required for this endpoint; unable to compute envelopes.")),
                                              Collections.emptyList());
        }

        List<Waveform> results = generateEnvelopesForBands(waveforms.stream().filter(Objects::nonNull).collect(Collectors.toList()), envConf);
        //        CompletableFuture.runAsync(() -> waveformSvc.saveAll(results));
        return new Result<List<Waveform>>(true, results);
    }

    private List<Waveform> generateEnvelopesForBands(List<Waveform> rawWaveforms, EnvelopeJobConfiguration envConf) {
        return rawWaveforms.parallelStream().map(wave -> {

            TimeT origintime = new TimeT(wave.getEvent().getOriginTime());
            TimeT startcut = origintime.add(-150.0); // cut the traces -150 to 1500 seconds relative to the origin time
            TimeT endcut = origintime.add(1500.0);

            return envConf.getFrequencyBandConfiguration().parallelStream().map(bandConfig -> {
                try {
                    Waveform seisWave = new Waveform().mergeNonNullOrEmptyFields(wave);

                    TimeSeries seis = converter.convert(wave);

                    //FIXME: Get from table
                    seis.interpolate(4d);

                    if (startcut.ge(endcut)) {
                        log.info("Start time of cut is >= end time of cut.");
                        return null;
                    }
                    if (startcut.ge(seis.getEndtime())) {
                        log.info("Start time of cut is >= end time of Seismogram.");
                        return null;
                    }
                    if (endcut.le(seis.getTime())) {
                        log.info("End time of cut is <= start time of Seismogram.");
                        return null;
                    }

                    //Note this mutates series as a whole            
                    seis.cut(startcut, endcut);
                    seis.RemoveMean();
                    seis.removeTrend();
                    seis.Taper(1);

                    seis.filter(4, Passband.BAND_PASS, bandConfig.getLowFrequency(), bandConfig.getHighFrequency(), true);

                    seis.Envelope();
                    seis.Log10();

                    int smoothing = bandConfig.getSmoothing();

                    //Convert it to samples
                    smoothing = (int) (smoothing * seis.getSamprate());
                    seis.Smooth(smoothing);

                    // final cut to eliminate smoothing edge effects
                    double trimlength = 2 * smoothing / seis.getSamprate();
                    seis.cut(seis.getTime().add(trimlength), seis.getEndtime().add(-1 * trimlength));

                    seisWave.setSampleRate(seis.getSamprate());
                    seisWave.setSegment(WaveformUtils.floatsToDoubles(seis.getData()));
                    seisWave.setLowFrequency(bandConfig.getLowFrequency());
                    seisWave.setHighFrequency(bandConfig.getHighFrequency());
                    seisWave.setBeginTime(seis.getTime().getDate());
                    seisWave.setEndTime(seis.getEndtime().getDate());

                    return seisWave;
                } catch (Exception e) {
                    log.info(e.getMessage(), e);
                    return null;
                }
            }).filter(Objects::nonNull);
        }).flatMap(Function.identity()).collect(Collectors.toList());
    }
}
