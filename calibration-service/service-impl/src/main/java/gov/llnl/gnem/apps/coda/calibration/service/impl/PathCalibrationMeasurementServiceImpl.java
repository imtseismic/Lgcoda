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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PathCalibrationMeasurement;
import gov.llnl.gnem.apps.coda.calibration.repository.PathCalibrationMeasurementRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationMeasurementService;

@Service
public class PathCalibrationMeasurementServiceImpl implements PathCalibrationMeasurementService {

    private PathCalibrationMeasurementRepository pathCalibrationMeasurementRepository;

    @Autowired
    public PathCalibrationMeasurementServiceImpl(PathCalibrationMeasurementRepository pathCalibrationMeasurementRepository) {
        this.pathCalibrationMeasurementRepository = pathCalibrationMeasurementRepository;
    }

    @Override
    public void delete(PathCalibrationMeasurement value) {
        pathCalibrationMeasurementRepository.delete(value);
    }

    @Override
    public List<PathCalibrationMeasurement> save(Iterable<PathCalibrationMeasurement> entities) {
        return pathCalibrationMeasurementRepository.saveAll(entities);
    }

    @Override
    public void delete(Iterable<Long> ids) {
        pathCalibrationMeasurementRepository.deleteAll(findAll(ids));
    }

    @Override
    public PathCalibrationMeasurement save(PathCalibrationMeasurement entity) {
        return pathCalibrationMeasurementRepository.save(entity);
    }

    @Override
    public PathCalibrationMeasurement findOne(Long id) {
        return pathCalibrationMeasurementRepository.findOneDetached(id);
    }

    @Override
    public PathCalibrationMeasurement findOneForUpdate(Long id) {
        return pathCalibrationMeasurementRepository.findOneDetached(id);
    }

    @Override
    public List<PathCalibrationMeasurement> findAll(Iterable<Long> ids) {
        return pathCalibrationMeasurementRepository.findAllById(ids);
    }

    @Override
    public List<PathCalibrationMeasurement> findAll() {
        return pathCalibrationMeasurementRepository.findAll();
    }

    @Override
    public long count() {
        return pathCalibrationMeasurementRepository.count();
    }

    @Override
    public void deleteAll() {
        pathCalibrationMeasurementRepository.deleteAllInBatch();
    }
}
