/*
 * Copyright 2015-2022 52°North Initiative for Geospatial Open Source
 * Software GmbH
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

package org.n52.youngs.transform.impl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.json.JsonXContent;
import org.joda.time.DateTime;
import org.n52.youngs.harvest.JsonNodeSourceRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.json.JsonConstants;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.util.function.Supplier;
import org.elasticsearch.xcontent.XContentLocation;

public class JsonToBuilderMapper implements Mapper {

    private static final Logger log = LoggerFactory.getLogger(JsonToBuilderMapper.class);

    private final LightweightYamlMappingConfiguration mapper;
    private final DeprecationHandler deprecationHandler;

    private ObjectWriter objectWriter;

    public JsonToBuilderMapper(LightweightYamlMappingConfiguration mapper) {
        this.mapper = mapper;
        objectWriter = new ObjectMapper().writer();
        this.deprecationHandler = new DeprecationHandler() {
            /*
            @Override
            public void usedDeprecatedName(String usedName, String modernName) {
                log.info("Deprecated name: {}, modernName: {}", usedName, modernName);
            }

            @Override
            public void usedDeprecatedField(String usedName, String replacedWith) {
                log.info("Deprecated field: {}, replacedWith: {}", usedName, replacedWith);
            }

            @Override
            public void deprecated(String message, Object... params) {
                // TODO Auto-generated method stub

            }*/

            @Override
            public void usedDeprecatedName(String parserName, Supplier<XContentLocation> location, String usedName, String modernName) {
                log.info("Deprecated name: {}, modernName: {}", usedName, modernName);
            }

            @Override
            public void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName, String replacedWith) {
                log.info("Deprecated field: {}, replacedWith: {}", usedName, replacedWith);
            }

            @Override
            public void usedDeprecatedField(String parserName, Supplier<XContentLocation> location, String usedName) {
               log.info("Deprecated field: {}", usedName);
            }

        };
    }

    @Override
    public MappingConfiguration getMapper() {
        return this.mapper;
    }

    @Override
    public SinkRecord map(SourceRecord sourceRecord) {
        Objects.nonNull(sourceRecord);
        if (!(sourceRecord instanceof JsonNodeSourceRecord)) {
            log.error("Record not instance of JsonNodeSourceRecord. Instead is of class: " + sourceRecord.getClass());
            return null;
        }
        XContentParser parser;
        try {
            JsonNode metadataNode = ((JsonNodeSourceRecord) sourceRecord).getRecord();
            String id = "";
            try {
                id = metadataNode.path(JsonConstants.FIELDNAME_ID)
                                 .asText();
            } catch (Exception e) {
                log.error("Could not fetch id.", e);
            }
            Collection<LightweightMappingEntry> mappingEntries = mapper.getLightweightEntries();
            for (LightweightMappingEntry lightweightMappingEntry : mappingEntries) {
                try {
                    mapEntry((ObjectNode) metadataNode, lightweightMappingEntry, (ObjectNode) metadataNode);
                } catch (Exception e) {
                    log.error("Could not map entry.", e);
                    log.trace("Metadata node: \n" + metadataNode);
                }
            }
            addFulltext((ObjectNode) metadataNode, objectWriter.writeValueAsString(metadataNode));
            byte[] bytes = objectWriter.writeValueAsBytes(metadataNode);
            parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY,
                                                            this.deprecationHandler,
                                                            new ByteArrayInputStream(bytes));
            XContentBuilder xContentBuilder = JsonXContent.contentBuilder()
                                                          .copyCurrentStructure(parser);
            return new BuilderRecord(id, xContentBuilder);
        } catch (IOException e) {
            log.error("Could not create XContentBuilder from InputStream.", e);
            return null;
        }
    }

    private void addFulltext(ObjectNode metadataNode, String fulltext) {
        metadataNode.put(JsonConstants.FIELDNAME_FULL_TEXT, fulltext);
    }

    private void mapEntry(ObjectNode recordNode,
            LightweightMappingEntry mappingEntry, ObjectNode sourceNode) {
        JsonNode mappedNode = resolveEntry(mappingEntry, sourceNode);
        if (mappedNode != null && !(mappedNode instanceof MissingNode)) {
            recordNode.set(mappingEntry.getFieldName(), mappedNode);
        }
    }

    private JsonNode resolveEntry(LightweightMappingEntry mappingEntry, ObjectNode sourceNode) {
        JsonNode result = MissingNode.getInstance();
        String expression = mappingEntry.getExpression();
        MappingType type = mappingEntry.getType();
        switch (type) {
            case DATE:
                if (expression.equals("current-dateTime()")) {
                    result = new TextNode(DateTime.now()
                                                  .toString());
                }
                return result;
            case NODE:
                expression = stripPreceedingSlash(expression);
                result = resolveValue(sourceNode, expression);
                return result;
            case STRING:
                result = new TextNode(expression);
                return result;
            case LIST:
                expression = stripPreceedingSlash(expression);
                String[] pathArrayForList = expression.split("//");
                if (pathArrayForList.length == 2) {
                    JsonNode arrayNode = sourceNode.path(pathArrayForList[0]);
                    ArrayNode valuesArrayNode = new ArrayNode(JsonNodeFactory.instance);
                    for (JsonNode itemNode : arrayNode) {
                        JsonNode valueNode = resolveValue(itemNode, pathArrayForList[1]);
                        if (!valueNode.isMissingNode()) {
                            valuesArrayNode.add(valueNode);
                        }
                    }
                    if (valuesArrayNode.size() > 0) {
                        result = valuesArrayNode;
                    }
                }

                if (pathArrayForList.length > 2) {
                    throw new IllegalStateException("At maximum one list indicator (//) is supported: " + expression);
                }
                return result;
            default:
                log.warn("Entry could not be resolved." + mappingEntry.toString());
                return result;
        }
    }

    private String stripPreceedingSlash(String expression) {
        return expression != null
                && expression.matches("^/{1}.*$")
                        ? expression.substring(1, expression.length())
                        : expression;
    }

    private JsonNode resolveValue(JsonNode sourceNode, String expression) {
        JsonNode result;
        String[] pathArray = expression.split("/");
        JsonNode currentNode = sourceNode.path(pathArray[0]);
        for (int i = 1; i < pathArray.length; i++) {
            if (currentNode instanceof ArrayNode) {
                currentNode = ((ArrayNode) currentNode).path(0);// TODO check if applicable
            }
            currentNode = currentNode.path(pathArray[i]);
        }
        result = currentNode;
        return result;
    }
}
