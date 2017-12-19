/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.application.web;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Stream;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.service.api.WaveformService;

@RestController
@RequestMapping(value = "/api/v1/waveforms", name = "WaveformsCollectionJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class WaveformsCollectionJsonController {

    /**
     * 
     * @param waveformService
     */
    @Autowired
    public WaveformsCollectionJsonController(WaveformService waveformService) {
        this.waveformService = waveformService;
    }

    /**
     * 
     * @param waveform
     * @param result
     * @return ResponseEntity
     */
    @PostMapping(value = "/query/all", name = "getByExampleAllMatching")
    public ResponseEntity<?> getByExampleAllMatching(@RequestBody Waveform waveform, BindingResult result) {

        List<Waveform> waveforms = getWaveformService().getByExampleAllMatching(waveform);
        return ResponseEntity.ok(waveforms);
    }

    @GetMapping(value = "/query/stacks", name = "getAllStacks")
    public ResponseEntity<?> getAllStacks() {

        List<Waveform> waveforms = getWaveformService().getAllStacks();
        return ResponseEntity.ok(waveforms);
    }

    @GetMapping(value = "/query/unique-by-event-station", name = "getAllStacks")
    public ResponseEntity<?> getUniqueEventStationStacks() {

        List<Object[]> eventStations = getWaveformService().getUniqueEventStationStacks();
        List<Waveform> waveforms = eventStations.parallelStream()
                                                .filter(evSta -> evSta.length >= 2)
                                                .map(evSta -> new Waveform().setEvent((Event) evSta[0]).setStream((Stream) evSta[1]))
                                                .collect(Collectors.toList());
        return ResponseEntity.ok(waveforms);
    }

    /**
     * 
     * @param waveforms
     * @param result
     * @return ResponseEntity
     */
    @PostMapping(value = "/batch/{sessionId}", name = "createBatch")
    public ResponseEntity<?> createBatch(@PathVariable Long sessionId, @Valid @RequestBody Collection<Waveform> waveforms, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getWaveformService().update(sessionId, waveforms);

        return ResponseEntity.ok().build();
    }

    /**
     * 
     * @param waveforms
     * @param result
     * @return ResponseEntity
     */
    @PutMapping(value = "/batch/{sessionId}", name = "updateBatch")
    public ResponseEntity<?> updateBatch(@PathVariable Long sessionId, @Valid @RequestBody Collection<Waveform> waveforms, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getWaveformService().update(sessionId, waveforms);

        return ResponseEntity.ok().build();
    }

    /**
     * 
     * @param ids
     * @return ResponseEntity
     */
    @DeleteMapping(value = "/batch/{ids}", name = "deleteBatch")
    public ResponseEntity<?> deleteBatch(@PathVariable("ids") Collection<Long> ids) {

        getWaveformService().delete(ids);

        return ResponseEntity.ok().build();
    }

    /**
     * 
     */
    private WaveformService waveformService;

    /**
     * 
     * @return WaveformService
     */
    public WaveformService getWaveformService() {
        return waveformService;
    }

    /**
     *
     * @param waveformService
     */
    public void setWaveformService(WaveformService waveformService) {
        this.waveformService = waveformService;
    }
}
