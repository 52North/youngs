/*
 * Copyright 2015-2016 52°North Initiative for Geospatial Open Source
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

import org.n52.youngs.impl.SourceRecordHelper;
import com.google.common.io.Resources;
import java.io.IOException;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import org.n52.youngs.util.JsonMatchers;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswMappingFullTextTest {

    private YamlMappingConfiguration cswConfiguration;

    private CswToBuilderMapper cswMapper;

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping_fulltext.yml")).openStream(),
                new XPathHelper());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void fullXml() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/gmd/metadata_fulltext_oneline.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains xml snippets", mappedRecordString,
                allOf(containsString("EO:EUM:CM:MULT:SARAH_V001 "),
                        containsString(" yummy "),
                        containsString(" sat.MFG ")));
    }

}
