/*
 * Copyright 2015-2020 52°North Initiative for Geospatial Open Source
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
import java.util.Objects;

import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonToBuilderMapper implements Mapper {

    private static final Logger log = LoggerFactory.getLogger(JsonToBuilderMapper.class);

    private final LightweightYamlMappingConfiguration mapper;

    public JsonToBuilderMapper(LightweightYamlMappingConfiguration mapper) {
        this.mapper = mapper;
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
                id = metadataNode.path(JsonConstants.FIELDNAME_ID).asText();
            } catch (Exception e) {
                log.error("Could not fetch id.", e);
            }
            Collection<LightweightMappingEntry> mappingEntries = mapper.getLightweightEntries();
            for (LightweightMappingEntry lightweightMappingEntry : mappingEntries) {
                mapEntry((ObjectNode) metadataNode, lightweightMappingEntry, (ObjectNode) metadataNode);
            }
            byte[] bytes = new ObjectMapper().writer().writeValueAsBytes(metadataNode);
            parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY, new ByteArrayInputStream(bytes));
            XContentBuilder xContentBuilder = JsonXContent.contentBuilder().copyCurrentStructure(parser);
            return new BuilderRecord(id, xContentBuilder);
        } catch (IOException e) {
            log.error("Could not create XContentBuilder from InputStream.", e);
            return null;
        }
    }

    private void mapEntry(ObjectNode recordNode,
            LightweightMappingEntry mappingEntry, ObjectNode sourceNode) {
        recordNode.set(mappingEntry.getFieldName(), resolveEntry(mappingEntry, sourceNode));
    }

    private JsonNode resolveEntry(LightweightMappingEntry mappingEntry, ObjectNode sourceNode) {
        JsonNode result = MissingNode.getInstance();
        String expression = mappingEntry.getExpression();
        MappingType type = mappingEntry.getType();
        switch (type) {
        case DATE:
            if(expression.equals("current-dateTime()")) {
                result = new TextNode(DateTime.now().toString());
            }
            return result;
        case NODE:
            String[] pathArray = expression.substring(1, expression.length()).split("/");
            JsonNode currentNode = sourceNode.path(pathArray[0]);
            for (int i = 1; i < pathArray.length; i++) {
                currentNode = currentNode.get(pathArray[i]);
            }
            result = currentNode;
            return result;
        case STRING:
            result = new TextNode(expression);
            return result;
        default:
            log.warn("Entry could not be resolved." + mappingEntry.toString());
            return result;
        }
    }

    private String extractString(String expression) {
        expression = expression.replace("string", "");
        expression = expression.replace("'", "");
        return expression;
    }

    private void mapKeywords(JsonNode metadataNode) {

        JsonNode keywordsAsObjectsNode = metadataNode.path(JsonConstants.FIELDNAME_KEYWORDS_AS_OBJECTS);

        JsonNode keywordNode = keywordsAsObjectsNode.path(JsonConstants.FIELDNAME_KEYWORD);

        ArrayNode keywordArrayNode = (ArrayNode) keywordNode;

        ObjectNode metadataNodeObjectNode = (ObjectNode) metadataNode;

        metadataNodeObjectNode.set(JsonConstants.FIELDNAME_KEYWORDS, keywordArrayNode);

        metadataNodeObjectNode.set(JsonConstants.FIELDNAME_KEYWORDS_TYPES, keywordsAsObjectsNode.path(JsonConstants.FIELDNAME_TYPE));
    }

}
