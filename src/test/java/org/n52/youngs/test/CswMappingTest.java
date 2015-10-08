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
public class CswMappingTest {

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
    public void bbox() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_1ef30a8b-876d-4828-9246-c37ab4510bbd.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-shape-type.html#_envelope
        // lat lon as array > GeoJSON conform: [lon, lat]
        assertThat("Mapped record contains envelope location", mappedRecordString,
                allOf(containsString("location"), containsString("envelope")));
        assertThat("Mapped record contains correct coordinate string", mappedRecordString,
                containsString("[ [ 68.41, 13.754 ], [ 60.042, 17.92 ] ]"));
    }

    @Test
    public void fullXml() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_1ef30a8b-876d-4828-9246-c37ab4510bbd.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains xmldoc field name", mappedRecordString, containsString("xmldoc"));
        assertThat("Mapped record contains xml snippets", mappedRecordString,
                allOf(containsString("<dc:type>http://purl.org/dc/dcmitype/Service</dc:type>"),
                        containsString("tifier>urn:uuid:1ef3"),
                        containsString("<ows:LowerCorner>60.042 13.754</ows:LowerCorner>\\n")));
    }

    @Test
    public void xmlSubelement() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_1ef30a8b-876d-4828-9246-c37ab4510bbd.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains xmldoc field name", mappedRecordString, containsString("bbox_xmldoc"));
        assertThat("Mapped record contains xml snippets", mappedRecordString,
                JsonMatchers.hasJsonPath("bbox_xmldoc",
                        allOf(containsString("<ows:BoundingBox xmlns:ows="),
                                containsString("<ows:LowerCorner>60.042 13.754</ows:LowerCorner>"),
                                not(containsString("csw:Record")),
                                not(containsString("urn:uuid")))));
    }

    @Test
    public void replace() throws Exception {
        SourceRecord record = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_1ef30a8b-876d-4828-9246-c37ab4510bbd.xml");
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map(record);
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("Mapped record contains field", mappedRecordString,
                allOf(containsString("replacer")));
        assertThat("Mapped record contains replaced coordinates as string", mappedRecordString,
                containsString(" [ \"60_042 13_754\", \"68_410 17_920\" ]"));
    }
}
