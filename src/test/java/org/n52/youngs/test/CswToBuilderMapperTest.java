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

import org.n52.youngs.impl.SourceRecordHelper;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Collection;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import static org.n52.youngs.util.JsonMatchers.hasJsonPath;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswToBuilderMapperTest {

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
    public void singleElementsAreParsedWithoutSlashText() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_829babb0-b2f1-49e1-8cd5-7b489fe71a1e.xml");
        BuilderRecord mappedRecord = cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains identifier", mappedRecordString,
                hasJsonPath("id", is("urn:uuid:829babb0-b2f1-49e1-8cd5-7b489fe71a1e")));
        assertThat("Mapped record contains type", mappedRecordString,
                hasJsonPath("type", is("http://purl.org/dc/dcmitype/Image")));
    }

    @Test
    public void singleElementsAreParsedWithSlashText() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_829babb0-b2f1-49e1-8cd5-7b489fe71a1e.xml");
        BuilderRecord mappedRecord = cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains format", mappedRecordString,
                hasJsonPath("format", is("image/jp2")));
    }

    @Test
    public void multipleElementsAreParsed() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_ab42a8c4-95e8-4630-bf79-33e59241605a.xml");
        BuilderRecord mappedRecord = cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

//        assertThat("Mapped record contains all subjects", mappedRecordString,
//                equalToIgnoringWhiteSpace("[ \"Otherography\", \"Physiography\", \"Morography\" ]"));
        assertThat("Mapped record contains all subjects", mappedRecordString,
                containsString("[ \"Otherography\", \"Physiography\", \"Morography\" ]"));
    }

    @Test
    public void stringResultsAdded() throws Exception {
        YamlMappingConfiguration c = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-xpath-values.yml")).openStream(),
                new XPathHelper().newXPathFactory());
        CswToBuilderMapper m = new CswToBuilderMapper(c);

        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_ab42a8c4-95e8-4630-bf79-33e59241605a.xml");
        BuilderRecord mappedRecord = m.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

//        assertThat("Mapped record contains all subjects", mappedRecordString,
//                equalToIgnoringWhiteSpace("[ \"Otherography\", \"Physiography\", \"Morography\" ]"));
        assertThat("Mapped record contains used_mapping field", mappedRecordString,
                containsString("used_mapping"));
        assertThat("Mapped record contains used_mapping field value", mappedRecordString,
                containsString("testmapping 1"));
    }

    @Test
    public void datetimeResultsAdded() throws Exception {
        YamlMappingConfiguration c = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-xpath-values.yml")).openStream(),
                new XPathHelper().newXPathFactory());
        CswToBuilderMapper m = new CswToBuilderMapper(c);

        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_ab42a8c4-95e8-4630-bf79-33e59241605a.xml");
        BuilderRecord mappedRecord = m.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains created_on field", mappedRecordString,
                containsString("created_on"));
    }

    @Test
    public void fulltextResultsAdded() throws Exception {
        YamlMappingConfiguration c = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-xpath-values.yml")).openStream(),
                new XPathHelper().newXPathFactory());
        CswToBuilderMapper m = new CswToBuilderMapper(c);

        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_ab42a8c4-95e8-4630-bf79-33e59241605a.xml");
        BuilderRecord mappedRecord = m.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains full_text field", mappedRecordString,
                containsString("full_text"));
        assertThat("Mapped record contains fields that are not covered by mapping", mappedRecordString,
                allOf(containsString("Physiography"), containsString("molestie lorem")));
    }

    @Test
    public void bbox() throws Exception {
        YamlMappingConfiguration c = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/testmapping-gmd-bbox.yml")).openStream(),
                new XPathHelper().newXPathFactory());
        CswToBuilderMapper m = new CswToBuilderMapper(c);

        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = m.map(record.iterator().next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains envelope", mappedRecordString,
                allOf(containsString("location"), containsString("envelope"),
                        containsString("[ [ 14.0, -11.1 ], [ -13.0, 12.22 ] ]")));
    }

}
