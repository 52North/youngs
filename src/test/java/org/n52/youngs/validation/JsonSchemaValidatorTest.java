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
package org.n52.youngs.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.youngs.harvest.InMemoryStreamSource;
import org.n52.youngs.harvest.JsonNodeSourceRecord;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.harvest.SourceException;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.ReportImpl;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class JsonSchemaValidatorTest {

    @BeforeClass
    public static void setup() {
        Locale.setDefault(Locale.ENGLISH);
    }


    @Test(expected = JsonValidationException.class)
    public void testInvalidFile() throws SAXException, SourceException, IOException, URISyntaxException {
        JsonSchemaValidator val = new JsonSchemaValidator(getClass().getResource("/schemas/PNModel4_Schema_V10.json").toURI());
        ObjectMapper om = new ObjectMapper();

        JsonNodeSourceRecord source = new JsonNodeSourceRecord(om.readTree(getClass().getResourceAsStream("/records/json/record_enum_invalid.json")), "json");
        List<String> result = val.validate(source.getRecord());
    }

    @Test
    public void testValidFile() throws SAXException, SourceException, IOException, URISyntaxException {
        JsonSchemaValidator val = new JsonSchemaValidator(getClass().getResource("/schemas/PNModel4_Schema_V10.json").toURI());
        ObjectMapper om = new ObjectMapper();

        JsonNodeSourceRecord source = new JsonNodeSourceRecord(om.readTree(getClass().getResourceAsStream("/records/json/record_enum.json")), "json");
        List<String> result = val.validate(source.getRecord());
        Assert.assertTrue("valid document expected", result.isEmpty());
    }


}
