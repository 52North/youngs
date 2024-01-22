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
package org.n52.youngs.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.n52.youngs.impl.SourceRecordHelper;
import com.google.common.io.Resources;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.containsString;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import static org.hamcrest.CoreMatchers.allOf;
import org.junit.Assert;
import static org.junit.Assert.assertThat;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswMappingNestedObjectTest {

    private YamlMappingConfiguration cswConfiguration;

    private CswToBuilderMapper cswMapper;

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-nested-object.yml")).openStream(),
                new XPathHelper());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void nestedOne() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/gmd/metadata_1000.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getData().toString();

        assertThat("Mapped record does not contain xml snippets", mappedRecordString,
                allOf(containsString("EO:EUM:CM:MULT:SARAH_V001")
                ));

        JsonNode json = new ObjectMapper().readTree(mappedRecordString);
        Assert.assertThat(json.has("format"), CoreMatchers.is(true));
        JsonNode format = json.get("format");
        Assert.assertThat(format.has("name"), CoreMatchers.is(true));
        Assert.assertThat(format.has("version"), CoreMatchers.is(true));
        Assert.assertThat(format.get("name").textValue(), CoreMatchers.equalTo("netCDF"));
        Assert.assertThat(format.get("version").textValue(), CoreMatchers.equalTo("-"));
    }

    @Test
    public void nestedTwo() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/gmd/metadata_1000b.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getData().toString();

        assertThat("Mapped record does not contain xml snippets", mappedRecordString,
                allOf(containsString("EO:EUM:CM:MULT:SARAH_V001")
                ));

        JsonNode json = new ObjectMapper().readTree(mappedRecordString);
        Assert.assertThat(json.has("format"), CoreMatchers.is(true));
        JsonNode format = json.get("format");
        Assert.assertThat(format, CoreMatchers.instanceOf(ArrayNode.class));
        ArrayNode formatArray = (ArrayNode) format;
        Assert.assertThat(formatArray.get(0).has("name"), CoreMatchers.is(true));
        Assert.assertThat(formatArray.get(0).has("version"), CoreMatchers.is(true));
        Assert.assertThat(formatArray.get(0).get("name").textValue(), CoreMatchers.equalTo("netCDF"));
        Assert.assertThat(formatArray.get(0).get("version").textValue(), CoreMatchers.equalTo("-"));
        Assert.assertThat(formatArray.get(1).has("name"), CoreMatchers.is(true));
        Assert.assertThat(formatArray.get(1).has("version"), CoreMatchers.is(true));
        Assert.assertThat(formatArray.get(1).get("name").textValue(), CoreMatchers.equalTo("FTP"));
        Assert.assertThat(formatArray.get(1).get("version").textValue(), CoreMatchers.equalTo("-"));
        Assert.assertThat(formatArray.get(1).has("typicalFilename"), CoreMatchers.is(true));
        Assert.assertThat(formatArray.get(1).get("typicalFilename").textValue(), CoreMatchers.equalTo("DNIdm200506150000002231000101MH.nc"));

    }

}
