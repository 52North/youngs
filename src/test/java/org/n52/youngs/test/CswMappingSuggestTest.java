/*
 * Copyright 2015-2017 52°North Initiative for Geospatial Open Source
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.n52.youngs.impl.SourceRecordHelper;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
public class CswMappingSuggestTest {

    private YamlMappingConfiguration cswConfiguration;

    private CswToBuilderMapper cswMapper;

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-suggest.yml")).openStream(),
                new XPathHelper());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void suggest() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/gmd/metadata_1000.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        JsonNode json = new ObjectMapper().readTree(mappedRecordString);
        Assert.assertThat(json.has("title"), CoreMatchers.is(true));
        Assert.assertThat(json.get("title").asText(), CoreMatchers.equalTo("Surface Solar Radiation Data Set - Heliosat (SARAH) - Edition 1"));

        Assert.assertThat(json.has("suggest"), CoreMatchers.is(true));
        JsonNode suggest = json.get("suggest");

        Assert.assertThat(suggest.has("output"), CoreMatchers.is(true));
        Assert.assertThat(suggest.get("output").asText(),
                CoreMatchers.equalTo("Surface Solar Radiation Data Set - Heliosat (SARAH) - Edition 1"));

        Assert.assertThat(suggest.has("input"), CoreMatchers.is(true));
        JsonNode input = suggest.get("input");
        Assert.assertThat(input, CoreMatchers.instanceOf(ArrayNode.class));
        ArrayNode inputArray = (ArrayNode) input;

        Iterable<JsonNode> iterable = () -> inputArray.iterator();
        List<String> inputs = StreamSupport.stream(iterable.spliterator(), false)
                .map(n -> n.asText())
                .collect(Collectors.toList());
        Assert.assertThat(inputs, CoreMatchers.hasItems("Surface", "Solar", "Radiation",
                "Heliosat", "SARAH"));
    }

}
