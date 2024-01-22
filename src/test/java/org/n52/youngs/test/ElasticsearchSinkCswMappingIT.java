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

import com.google.common.io.Resources;
import org.n52.youngs.impl.SourceRecordHelper;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.client.fluent.Request;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.load.impl.ElasticsearchClientSink;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import static org.n52.youngs.util.JsonMatchers.hasJsonPath;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchSinkCswMappingIT {

    private Sink sink;

    private MappingConfiguration mapping;

    // set to (true); to run focussed test methods from Netbeans
    @ClassRule
    public static ElasticsearchServer server = new ElasticsearchServer(); // FIXME (true);

    private CswToBuilderMapper mapper;

    @Before
    public void createMappingAndSink() throws IOException {
        mapping = new YamlMappingConfiguration(Resources.asByteSource(
                Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper());
        sink = new ElasticsearchClientSink(server.getClient(), "elasticsearch", mapping.getIndex(), mapping.getType());

        boolean prepare = sink.prepare(mapping);
        assertThat("sink is prepared", prepare, is(true));
        mapper = new CswToBuilderMapper(mapping);
    }

    @After
    public void clearSink() throws IOException {
        boolean result = sink.clear(mapping);
        assertThat("sink is cleared", result, is(true));
    }

    @Test
    public void store() throws Exception {
        Collection<SourceRecord> records = SourceRecordHelper.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-records-csw.xml")).openStream());
        List<SinkRecord> mappedRecords = records.stream().map(mapper::map).collect(Collectors.toList());

        boolean stored = sink.store(mappedRecords);
        Thread.sleep(1000);
        assertThat("all records stored", stored);

        String query = "http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType()
                + "/_search?q=*&size=100";
        String searchAllResponse = Request
                .Get(query).execute()
                .returnContent().asString();
        assertThat("all records were added to the index", searchAllResponse,
                hasJsonPath("hits.total", is(mappedRecords.size())));
        assertThat("ids are contains", searchAllResponse, allOf(
                containsString("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:METOP:NRP"),
                containsString("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:METOP:SEM"),
                containsString("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:MFG:CDS-IODC"),
                containsString("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:MULT:FDN"),
                containsString("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:METEOSAT:OSIDSSI")));

        String id = "urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:METOP:ORBITVIEW";
        String recordResponse = Request
                .Get("http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType()
                        + "/" + id).execute()
                .returnContent().asString();
        assertThat("record is in index", recordResponse, hasJsonPath("_index", is(equalTo(mapping.getIndex()))));
        assertThat("record is found", recordResponse, hasJsonPath("_id", is(id)));
        assertThat("record is found", recordResponse, hasJsonPath("_source.type", is("series")));
    }

    @Test
    public void mapAndStoreFile() throws Exception {
        SourceRecord sourceRecord = SourceRecordHelper.getSourceRecordFromFile("records/csw/Record_94bc9c83-97f6-4b40-9eb8-a8e8787a5c63.xml");
        SinkRecord sinkRecord = mapper.map(sourceRecord);

        boolean stored = sink.store(sinkRecord);
        assertThat("record added", stored);

        String response = Request.Get("http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType() + "/_search?pretty&q=*")
                .setHeader("Accept", "application/json").execute().returnContent().asString();
        assertThat(response, containsString("\"total\" : 1"));
        assertThat(response, containsString("urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63"));
        assertThat(response, containsString("xmldoc"));
        assertThat(response, containsString("<dc:subject scheme=\"http://www.digest.org/2.1\">Vegetation-Cropland</dc:subject>"));
    }

}
