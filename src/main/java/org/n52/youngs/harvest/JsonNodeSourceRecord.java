/*
 * Copyright 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.harvest;

import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;

public class JsonNodeSourceRecord implements SourceRecord {

    private final JsonNode record;
    private final String protocolIdentifier;

    public JsonNodeSourceRecord(JsonNode record, String protocolIdentifier) {
        this.record = record;
        this.protocolIdentifier = protocolIdentifier;
    }

    public JsonNode getRecord() {
        return this.record;
    }

    @Override
    public String getProtocolIdentifier() {
        return protocolIdentifier;
    }


}
