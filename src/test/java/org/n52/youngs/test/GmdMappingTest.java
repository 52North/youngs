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
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.elasticsearch.common.Strings;
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

    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void load() throws IOException {
        cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/gmd-metadata.yml")).openStream(),
                new XPathHelper());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Ignore
    @Test
    public void keywordTypeConcatenation() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = cswMapper.map(record.iterator().next());
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        assertThat("Mapped record contains type", mappedRecordString,
                allOf(containsString("keywords"), containsString("theme:GEOSS"), containsString("theme:HelioClim"),
                        containsString("place:Europe")));
    }

    @Test
    public void temporalExtentWithPositions() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = cswMapper.map(record.iterator().next());
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        assertThat("Mapped record contains extend timestamps", mappedRecordString,
                allOf(containsString("\"extent_begin\" : \"1985-01-01T00:00:00\""),
                        containsString("\"extent_end\" : \"2005-12-31T23:45:00\"")));
    }

    @Test
    public void temporalExtentWithBeginEnd() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso-2.xml")).openStream());
        Iterator<SourceRecord> iter = record.iterator();
        // 7th record has end extent != "unknown"
        iter.next();
        iter.next();
        iter.next();
        iter.next();
        iter.next();
        iter.next();

        BuilderRecord mappedRecord = cswMapper.map(iter.next());
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        assertThat("Mapped record contains extend timestamps", mappedRecordString,
                allOf(containsString("\"extent_begin\" : \"1991-08-22\""),
                        containsString("\"extent_end\" : \"1994-11-24\"")));
    }

    @Test
    public void id() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = cswMapper.map(record.iterator().next());
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        assertThat("Mapped record contains extent timestamps", mappedRecordString,
                allOf(containsString("\"id\" : \"5a716d99-afac-47e0-9de9-14cf707be994\"")));
    }

    @Test
    public void bbox() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml")).openStream());
        BuilderRecord mappedRecord = cswMapper.map(record.iterator().next());
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        mapper.disable(SerializationFeature.INDENT_OUTPUT);
        mappedRecordString = mapper.readTree(mappedRecordString).toString();

        assertThat("Mapped record contains envelope", mappedRecordString,
                allOf(containsString("location"), containsString("envelope"),
                        containsString("[[-11.1,14.0],[12.22,-13.0]]")));
    }

    @Test
    public void maintenanceFrequency() throws Exception {
        Collection<SourceRecord> record = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-iso-2.xml")).openStream());
        BuilderRecord mappedRecord = cswMapper.map(record.iterator().next());
        String mappedRecordString = Strings.toString(mappedRecord.getBuilder());

        assertThat("Mapped record contains update frequency", mappedRecordString,
                allOf(containsString("\"metadata_maintenance\" : \"asNeeded\"")));
        assertThat("Mapped record contains next update", mappedRecordString,
                allOf(containsString("\"metadata_next_update\" : \"2015-01-01\"")));
    }

}
