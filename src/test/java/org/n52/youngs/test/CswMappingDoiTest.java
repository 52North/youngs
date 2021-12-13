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
package org.n52.youngs.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.n52.youngs.impl.SourceRecordHelper;
import com.google.common.io.Resources;
import java.io.IOException;
import org.elasticsearch.common.Strings;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import org.junit.Assert;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswMappingDoiTest {

    private YamlMappingConfiguration cswConfiguration;

    private CswToBuilderMapper cswMapper;

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-doi.yml")).openStream(),
                new XPathHelper());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void doi() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/gmd/metadata_1000.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        JsonNode parent = new ObjectMapper().readTree(mappedRecordString);

        Assert.assertThat(parent.has("doi"), CoreMatchers.is(true));
        JsonNode json = parent.get("doi");
        Assert.assertThat(json.has("url"), CoreMatchers.is(true));
        Assert.assertThat(json.get("url").asText(), CoreMatchers.equalTo("http://dx.doi.org/10.5676/EUM_SAF_CM/SARAH/V001"));

        Assert.assertThat(json.has("identifier"), CoreMatchers.is(true));
        Assert.assertThat(json.get("identifier").asText(), CoreMatchers.equalTo("10.5676/EUM_SAF_CM/SARAH/V001"));
    }

}
