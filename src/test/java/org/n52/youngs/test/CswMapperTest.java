/*
 * Copyright 2015-2015 52°North Initiative for Geospatial Open Source
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

import com.google.common.io.Resources;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import static org.n52.youngs.util.JsonMatchers.hasJsonPath;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswMapperTest {

    private YamlMappingConfiguration cswConfiguration;

    private CswToBuilderMapper cswMapper;

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper().newXPathFactory());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void singleElementsAreParsed() throws Exception {
        SourceRecord record = Util.getSourceRecordFromFile("records/csw/Record_ab42a8c4-95e8-4630-bf79-33e59241605a.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains identifier", mappedRecordString,
                hasJsonPath("id", is("urn:uuid:ab42a8c4-95e8-4630-bf79-33e59241605a")));
        assertThat("Mapped record contains type", mappedRecordString,
                hasJsonPath("type", is("http://purl.org/dc/dcmitype/Service")));
    }

    @Test
    public void multipleElementsAreParsed() throws Exception {
        SourceRecord record = Util.getSourceRecordFromFile("records/csw/Record_ab42a8c4-95e8-4630-bf79-33e59241605a.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

//        assertThat("Mapped record contains all subjects", mappedRecordString,
//                equalToIgnoringWhiteSpace("[ \"Otherography\", \"Physiography\", \"Morography\" ]"));
        assertThat("Mapped record contains all subjects", mappedRecordString,
                containsString("[ \"Otherography\", \"Physiography\", \"Morography\" ]"));
    }

}