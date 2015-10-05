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
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Ignore;
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
public class GmdMappingTest {

    private YamlMappingConfiguration cswConfiguration;

    private CswToBuilderMapper cswMapper;

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/gmd-metadata.yml")).openStream(),
                new XPathHelper().newXPathFactory());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Ignore
    @Test
    public void keywordTypeConcatenation() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record.iterator().next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains type", mappedRecordString,
                allOf(containsString("keywords"), containsString("theme:GEOSS"), containsString("theme:HelioClim"),
                        containsString("place:Europe")));
    }

    @Test
    public void temporalExtent() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record.iterator().next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains extend timestamps", mappedRecordString,
                allOf(containsString("extend_begin"), containsString("extend_end"), containsString("1985-01-01T00:00:00"),
                        containsString("2005-12-31T23:45:00")));
    }

    @Test
    public void id() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record.iterator().next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains extent timestamps", mappedRecordString,
                allOf(containsString("\"id\" : \"5a716d99-afac-47e0-9de9-14cf707be994\"")));
    }

    @Test
    public void bbox() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record.iterator().next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains envelope", mappedRecordString,
                allOf(containsString("location"), containsString("envelope"),
                        containsString("[ [ 14.0, -11.1 ], [ -13.0, 12.22 ] ]")));
    }

}
