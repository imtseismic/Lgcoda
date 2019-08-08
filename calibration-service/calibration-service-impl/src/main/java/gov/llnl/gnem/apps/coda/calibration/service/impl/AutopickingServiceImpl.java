/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.service.api.AutopickingService;
import gov.llnl.gnem.apps.coda.calibration.service.api.EndTimePicker;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
public class AutopickingServiceImpl implements AutopickingService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private EndTimePicker endTimePicker;
    private WaveformToTimeSeriesConverter converter = new WaveformToTimeSeriesConverter();
    private static final double BAD = 0d;

    public AutopickingServiceImpl(EndTimePicker endTimePicker) {
        this.endTimePicker = endTimePicker;
    }

    @Override
    public Collection<PeakVelocityMeasurement> autoPickVelocityMeasuredWaveforms(final Collection<PeakVelocityMeasurement> velocityMeasurements,
            final Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters) {
        return velocityMeasurements.parallelStream().filter(vel -> vel.getWaveform() != null).map(vel -> {
            SharedFrequencyBandParameters params = frequencyBandParameters.get(new FrequencyBand(vel.getWaveform().getLowFrequency(), vel.getWaveform().getHighFrequency()));
            Optional<WaveformPick> pick = Optional.empty();
            Optional<WaveformPick> endPick = Optional.empty();

            if (vel.getWaveform().getAssociatedPicks() != null) {
                pick = vel.getWaveform().getAssociatedPicks().stream().filter(p -> p.getPickType() != null && PICK_TYPES.AP.name().equalsIgnoreCase(p.getPickType().trim())).findFirst();

                endPick = vel.getWaveform().getAssociatedPicks().stream().filter(p -> p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim())).findFirst();
            } else {
                vel.getWaveform().setAssociatedPicks(new ArrayList<>());
            }

            if ((!endPick.isPresent() || pick.isPresent()) && params != null) {
                log.trace("Starting autopick");
                vel.getWaveform().getAssociatedPicks().forEach(p -> p.setWaveform(null));
                vel.getWaveform().getAssociatedPicks().clear();

                double minlength = params.getMinLength();
                double maxlength = params.getMaxLength();

                double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + vel.getDistance());
                if (vr == 0.0) {
                    vr = 1.0;
                }
                TimeT originTime = new TimeT(vel.getWaveform().getEvent().getOriginTime());
                TimeT startTime;
                TimeT trimTime = originTime.add(vel.getDistance() / vr);
                TimeT beginTime = new TimeT(vel.getWaveform().getBeginTime());
                TimeSeries trimmedWaveform = converter.convert(vel.getWaveform());
                try {
                    trimmedWaveform.cutBefore(trimTime);
                    trimmedWaveform.cutAfter(trimTime.add(30.0));

                    startTime = new TimeT(trimTime.getEpochTime() + trimmedWaveform.getMaxTime()[0]);
                } catch (IllegalArgumentException e) {
                    startTime = trimTime;
                }

                TimeSeries segment = converter.convert(vel.getWaveform());
                segment.interpolate(1.0);

                double stopTime = endTimePicker.getEndTime(
                        segment.getData(),
                            segment.getSamprate(),
                            startTime.getEpochTime(),
                            segment.getIndexForTime(startTime.getEpochTime()),
                            minlength,
                            maxlength,
                            params.getMinSnr(),
                            vel.getNoiseLevel());
                log.trace("Proposed end pick time {}", stopTime);

                if (new TimeT(stopTime).gt(startTime)) {
                    stopTime = stopTime + beginTime.subtractD(originTime);
                    log.trace("End pick time adjusted by begintime {}", stopTime);
                }
                stopTime = new TimeT(stopTime).subtractD(startTime);
                log.trace("End pick time minus starttime {}", stopTime);

                double offset = stopTime - startTime.subtractD(originTime);
                if (offset < minlength) {
                    stopTime = BAD;
                } else if (offset > maxlength) {
                    stopTime = maxlength;
                }

                WaveformPick autoPick = new WaveformPick().setPickType(PICK_TYPES.F.name())
                                                          .setPickName(PICK_TYPES.F.getPhase())
                                                          .setWaveform(vel.getWaveform())
                                                          .setPickTimeSecFromOrigin((float) stopTime);

                WaveformPick startPick = new WaveformPick().setPickType(PICK_TYPES.AP.name())
                                                           .setPickName(PICK_TYPES.AP.getPhase())
                                                           .setWaveform(vel.getWaveform())
                                                           .setPickTimeSecFromOrigin((float) startTime.subtractD(originTime));

                vel.getWaveform().getAssociatedPicks().add(autoPick);
                vel.getWaveform().getAssociatedPicks().add(startPick);
                log.trace("Ending autopick: autoPick {}, startPick {}, startTime {}, stopTime {}", autoPick, startPick, startTime, stopTime);
            }

            return vel;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
