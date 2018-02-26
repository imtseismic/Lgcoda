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
package gov.llnl.gnem.apps.coda.calibration.standalone.data.client;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ReferenceEventClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class ReferenceEventLocalClient implements ReferenceEventClient {

    private ReferenceMwParametersService service;
    private MeasuredMwsService measureService;

    @Autowired
    public ReferenceEventLocalClient(ReferenceMwParametersService service, MeasuredMwsService measureService) {
        this.service = service;
        this.measureService = measureService;
    }

    @Override
    public Flux<ReferenceMwParameters> getReferenceEvents() {
        return Flux.fromIterable(service.findAll()).filter(Objects::nonNull).onErrorReturn(new ReferenceMwParameters());
    }

    @Override
    public Mono<String> postReferenceEvents(List<ReferenceMwParameters> refEvents) throws JsonProcessingException {
        return Mono.just(service.save(refEvents).toString());
    }

    @Override
    public Flux<MeasuredMwParameters> getMeasuredEvents() {
        return Flux.fromIterable(measureService.findAll()).filter(Objects::nonNull).onErrorReturn(new MeasuredMwParameters());
    }

}
