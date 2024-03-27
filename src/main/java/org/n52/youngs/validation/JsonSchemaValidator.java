/*
 * Copyright 2015-2024 52°North Spatial Information Research GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.youngs.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidatorTypeCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author bpr
 *
 */
public class JsonSchemaValidator implements org.n52.youngs.validation.Validator {

    private static final Logger LOG = LoggerFactory.getLogger(JsonSchemaValidator.class);
    private ObjectMapper mapper = new ObjectMapper();
    private JsonSchema schema;

    public JsonSchemaValidator(URI schemaLocation) {

        JsonNode schemaNode;
        File jsonSchemaFile = new File(schemaLocation);
//        try {
//            jsonSchemaFile = new File(schemaLocation);
//        } catch (Exception e) {
//            LOG.error("Coul: " + schemaLocation.toString());
//            return;
//        }
        try {
            schemaNode = mapper.readTree(jsonSchemaFile );
        } catch (IOException e) {
            LOG.error("Could not create JsonNode for JsonSchema: " + jsonSchemaFile.toString());
            return;
        }
        schema = getJsonSchemaFromJsonNode(schemaNode);
    }

    public List<String> validate(JsonNode node) {
        Set<ValidationMessage> validationMessages = schema.validate(node);
        if(validationMessages == null || validationMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>(validationMessages.size());
        for (ValidationMessage validationMessage : validationMessages) {
            result.add(validationMessage.toString());
        }

        // TODO: add more possible breaking error codes
        List<String> fatalCodes = Arrays.asList(ValidatorTypeCode.REQUIRED.getErrorCode(),
                ValidatorTypeCode.TYPE.getErrorCode(),
                ValidatorTypeCode.ENUM.getErrorCode());

        String fatalErrors = validationMessages.stream()
                .filter(vm -> fatalCodes.contains(vm.getCode()))
                .map(vm -> vm.getMessage()).collect(Collectors.joining("; "));

        if (fatalErrors != null && fatalErrors.length() > 0) {
            String id = "";
            try {
                id = node.get("id").asText();
            } catch (Exception e) {
                // eat
            }

            LOG.error("Fatal validation errors for record with id: " + id);
            throw new JsonValidationException(String.format("The validation of the record with id %s encountered fatal errors: ", id) + fatalErrors);
        }

        return result;
    }

    private JsonSchema getJsonSchemaFromJsonNode(JsonNode jsonNode) {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setTypeLoose(false);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        return factory.getSchema(jsonNode, config);
    }

}
