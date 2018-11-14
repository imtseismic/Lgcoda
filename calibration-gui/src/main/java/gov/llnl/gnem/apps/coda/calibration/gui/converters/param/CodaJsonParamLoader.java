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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.param;

import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.BAND_FIELD;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.MDAC_FI_FIELD;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.MDAC_PS_FIELD;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.REFERENCE_EVENTS_FIELD;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.SCHEMA_FIELD;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.SCHEMA_VALUE;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.TYPE_FIELD;
import static gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants.TYPE_VALUE;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToParameterConverter;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SharedFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import reactor.core.publisher.Flux;

@Service
public class CodaJsonParamLoader implements FileToParameterConverter<Object> {

    private final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.json");

    private final ObjectMapper mapper;

    public CodaJsonParamLoader() {
        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.addMixIn(SharedFrequencyBandParameters.class, SharedFrequencyBandParametersFileMixin.class);
    }

    @Override
    public Flux<Result<Object>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
            return Flux.fromIterable(convertJsonParamFile(file));
        }
        return Flux.empty();
    }

    @Override
    public Flux<Result<Object>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    public List<Result<Object>> convertJsonParamFile(File file) {
        if (file == null) {
            return Collections.singletonList(
                    exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable. %s", "NULL", "File reference is null"))));
        }

        List<Result<Object>> results = new ArrayList<>();

        try {
            JsonNode node = mapper.readTree(file);

            if (node.has(SCHEMA_FIELD) && node.get(SCHEMA_FIELD).asText().equalsIgnoreCase(SCHEMA_VALUE) && node.has(TYPE_FIELD) && node.get(TYPE_FIELD).asText().equalsIgnoreCase(TYPE_VALUE)) {
                results.addAll(convertJsonFields(node, BAND_FIELD, x -> sharedFrequenyBandFromJsonNode(x)));
                results.addAll(convertJsonFields(node, MDAC_PS_FIELD, x -> mdacPsFromJsonNode(x)));
                results.addAll(convertJsonFields(node, MDAC_FI_FIELD, x -> mdacFiFromJsonNode(x)));
                results.addAll(convertJsonFields(node, REFERENCE_EVENTS_FIELD, x -> refEventsFromJsonNode(x)));
            }
        } catch (IOException e) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): %s", file.getName(), e.getMessage()), e)));
        }

        return results;
    }

    protected List<Result<Object>> convertJsonFields(JsonNode node, String field, Function<JsonNode, Result<Object>> func) {
        List<Result<Object>> results = new ArrayList<>();
        List<JsonNode> nodes = node.findValues(field);
        for (JsonNode found : nodes) {
            if (found.isArray()) {
                for (JsonNode entry : found) {
                    results.add(func.apply(entry));
                }
            } else {
                results.add(func.apply(found));
            }
        }
        return results;
    }

    protected Result<Object> refEventsFromJsonNode(JsonNode node) {
        ObjectReader reader = mapper.readerFor(ReferenceMwParameters.class);
        try {
            //TODO: Validate all fields
            ReferenceMwParameters ref = reader.readValue(node);
            return new Result<Object>(true, ref);
        } catch (IOException e) {
            return exceptionalResult(e);
        }
    }

    protected Result<Object> mdacPsFromJsonNode(JsonNode node) {
        ObjectReader reader = mapper.readerFor(MdacParametersPS.class);
        try {
            //TODO: Validate all fields
            MdacParametersPS ps = reader.readValue(node);
            return new Result<Object>(true, ps);
        } catch (IOException e) {
            return exceptionalResult(e);
        }
    }

    protected Result<Object> mdacFiFromJsonNode(JsonNode node) {
        ObjectReader reader = mapper.readerFor(MdacParametersFI.class);
        try {
            //TODO: Validate all fields
            MdacParametersFI fi = reader.readValue(node);
            return new Result<Object>(true, fi);
        } catch (IOException e) {
            return exceptionalResult(e);
        }
    }

    protected Result<Object> sharedFrequenyBandFromJsonNode(JsonNode band) {
        SharedFrequencyBandParameters sfb = new SharedFrequencyBandParameters();
        if (band.get("lowFreqHz").isNull() || band.get("highFreqHz").isNull()) {
            return exceptionalResult(new LightweightIllegalStateException("Unable to parse frequency band " + sfb.toString() + "; received a empty frequency band."));
        } else {
            try {
                ObjectReader reader = mapper.readerFor(SharedFrequencyBandParameters.class);
                sfb = reader.readValue(band);
            } catch (IOException e) {
                return new Result<Object>(false, null).setErrors(Collections.singletonList(e));
            }
            return new Result<Object>(true, sfb);
        }
    }

    private Result<Object> exceptionalResult(Exception error) {
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        return exceptionalResult(exceptions);
    }

    private Result<Object> exceptionalResult(List<Exception> errors) {
        return new Result<>(false, errors, null);
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return filter;
    }
}
