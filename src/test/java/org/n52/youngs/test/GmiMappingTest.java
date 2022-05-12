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
package org.n52.youngs.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.n52.youngs.impl.SourceRecordHelper;
import com.google.common.io.Resources;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
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
public class GmiMappingTest {


    @Test
    public void splitting() throws Exception {
        YamlMappingConfiguration configuration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-split.yml")).openStream(),
                new XPathHelper());
        CswToBuilderMapper mapper = new CswToBuilderMapper(configuration);

        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/gmi/metadata_10.xml");
        BuilderRecord mappedRecord = mapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        ObjectMapper mapperJson = new ObjectMapper();
        mapperJson.disable(SerializationFeature.INDENT_OUTPUT);
        mappedRecordString = mapperJson.readTree(mappedRecordString).toString();

        assertThat("Mapped record contains extend timestamps", mappedRecordString,
                allOf(containsString("\"splitter\":[\"theme:Atmospheric conditions\","),
                        containsString("\"socialBenefitArea:Weather"),
                        //                        containsString("\"socialBenefitArea:Climate\""), // FIXME come up with magic XPath or nested splitting
                        containsString("\"theme:Atmosphere\""),
                        containsString("\"place:SAF Archive & FTPEUMETSAT Data Centre\""),
                        containsString("\"theme:Cloud\"")));
    }

}
