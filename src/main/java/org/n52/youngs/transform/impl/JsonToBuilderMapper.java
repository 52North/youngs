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
import java.util.Objects;
import java.util.UUID;

import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.n52.youngs.harvest.JsonNodeSourceRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonToBuilderMapper implements Mapper {

    private static final Logger log = LoggerFactory.getLogger(JsonToBuilderMapper.class);

    private final MappingConfiguration mapper;

    public JsonToBuilderMapper(MappingConfiguration mapper) {
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

            byte[] bytes = new ObjectMapper().writer().writeValueAsBytes(((JsonNodeSourceRecord) sourceRecord).getRecord());

            parser = JsonXContent.jsonXContent.createParser(NamedXContentRegistry.EMPTY, new ByteArrayInputStream(bytes));

            XContentBuilder xContentBuilder = JsonXContent.contentBuilder().copyCurrentStructure(parser);

            return new BuilderRecord(UUID.randomUUID().toString().substring(0, 8), xContentBuilder);

        } catch (IOException e) {
            log.error("Could not create XContentBuilder from InputStream.", e);
            return null;
        }

    }

}
