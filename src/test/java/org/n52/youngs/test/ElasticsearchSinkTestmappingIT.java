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

import co.elastic.clients.elasticsearch._types.SearchTransform;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Map;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.number.OrderingComparison;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.ElasticsearchRemoteHttpSink;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import static org.n52.youngs.util.JsonMatchers.hasJsonPath;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchSinkTestmappingIT {

    private ElasticsearchRemoteHttpSink sink;

    private YamlMappingConfiguration mapping;

    @Before
    public void createMappingAndSink() throws IOException {
        mapping = new YamlMappingConfiguration(Resources.asByteSource(
                Resources.getResource("mappings/testmapping.yml")).openStream(),
                new XPathHelper());

        sink = new ElasticsearchRemoteHttpSink("localhost", 9200, "elasticsearch", mapping.getIndex(), mapping.getType());
        sink.clear(mapping);
    }

    @After
    public void clearSink() throws IOException {
        if (sink != null) {
            boolean result = sink.clear(mapping);
            assertThat("sink is cleared", result, is(true));
        }
    }

    @Test
    public void insertSchema() throws Exception {
        sink.prepare(mapping);

        ElasticsearchIndicesClient indicesClient = sink.getClient().indices();
        GetMappingRequest.Builder builder = new GetMappingRequest.Builder().index(mapping.getIndex());
        GetMappingResponse resp = indicesClient.getMapping(builder.build());

        Map<String, IndexMappingRecord> mappings = resp.result();

        TypeMapping recordTypeMap = mappings.get(mapping.getIndex()).mappings();
        assertThat("dynamic value is correct", Boolean.valueOf(recordTypeMap.dynamic().jsonValue()), is(mapping.isDynamicMappingEnabled()));

        Thread.sleep(1000);

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html
        String allMappingsResponse = Request
                .Get("http://localhost:9200/_mapping?pretty").execute()
                .returnContent().asString();
        assertThat("record type is provided (checking id property type", allMappingsResponse, hasJsonPath(mapping.getIndex() + ".mappings.properties.id.type", is("text")));
        assertThat("metadata type is provided (checking mt_update-time property type)", allMappingsResponse, hasJsonPath(mapping.getIndex() + "-meta.mappings.properties.mt-update-time.type", is("date")));

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-types-exists.html
        StatusLine recordsStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex()).execute()
                .returnResponse().getStatusLine();
        assertThat("records type is available", recordsStatus.getStatusCode(), is(200));
        StatusLine mtStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex() + "-meta").execute()
                .returnResponse().getStatusLine();
        assertThat("metadata type is available", mtStatus.getStatusCode(), is(200));
    }

    @Test
    public void clear() throws Exception {
        sink.prepare(mapping);
        boolean clearResult = sink.clear(mapping, true);
        Thread.sleep(1000);

        assertThat("clear result is OK", clearResult, is(true));
        assertEmptyNode();
    }

    @Test
    public void createIndexDisabled() throws Exception {
        mapping = new YamlMappingConfiguration("mappings/testmapping-creation-disabled-but-dynamic.yml", new XPathHelper());
        sink.prepare(mapping);
        Thread.sleep(1000);

        assertEmptyNode();
    }

    private void assertEmptyNode() throws IOException {
        String allMappingsResponse = Request
                .Get("http://localhost:9200/_mapping/_all?pretty").execute()
                .returnContent().asString();
        assertThat("response is empty", allMappingsResponse, not(containsString("mappings")));

        StatusLine recordsStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType()).execute()
                .returnResponse().getStatusLine();
        assertThat("records type is not available", recordsStatus.getStatusCode(), OrderingComparison.greaterThanOrEqualTo(404));
        StatusLine mtStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex() + "/mt").execute()
                .returnResponse().getStatusLine();
        assertThat("metadata type is not available", mtStatus.getStatusCode(), OrderingComparison.greaterThanOrEqualTo(404));
    }

}
