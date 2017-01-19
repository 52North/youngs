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

import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Map;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsAction;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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

    // set to (true); to run focussed test methods from Netbeans
    @ClassRule
    public static ElasticsearchServer server = new ElasticsearchServer(); // (true);

    @Before
    public void createMappingAndSink() throws IOException {
        mapping = new YamlMappingConfiguration(Resources.asByteSource(
                Resources.getResource("mappings/testmapping.yml")).openStream(),
                new XPathHelper());

        sink = new ElasticsearchRemoteHttpSink("localhost", 9300, "elasticsearch", mapping.getIndex(), mapping.getType());
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

        IndicesAdminClient indicesClient = sink.getClient().admin().indices();
        GetMappingsRequestBuilder builder = new GetMappingsRequestBuilder(indicesClient, GetMappingsAction.INSTANCE, mapping.getIndex())
                .addTypes(mapping.getType());
        GetMappingsResponse response = indicesClient.getMappings(builder.request()).actionGet();
        ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> mappings = response.getMappings();

        Map<String, Object> recordTypeMap = mappings.get(mapping.getIndex()).get(mapping.getType()).getSourceAsMap();
        assertThat("dynamic value is correct", Boolean.valueOf(recordTypeMap.get("dynamic").toString()), is(mapping.isDynamicMappingEnabled()));

        Thread.sleep(1000);

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-mapping.html
        String allMappingsResponse = Request
                .Get("http://localhost:9200/_mapping/_all?pretty").execute()
                .returnContent().asString();
        assertThat("record type is provided (checking id property type", allMappingsResponse, hasJsonPath(mapping.getIndex() + ".mappings." + mapping.getType() + ".properties.id.type", is("string")));
        assertThat("metadata type is provided (checking mt_update-time property type)", allMappingsResponse, hasJsonPath(mapping.getIndex() + ".mappings.mt.properties.mt-update-time.type", is("date")));

        // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-types-exists.html
        StatusLine recordsStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType()).execute()
                .returnResponse().getStatusLine();
        assertThat("records type is available", recordsStatus.getStatusCode(), is(200));
        StatusLine mtStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex() + "/mt").execute()
                .returnResponse().getStatusLine();
        assertThat("metadata type is available", mtStatus.getStatusCode(), is(200));
    }

    @Test
    public void clear() throws Exception {
        sink.prepare(mapping);
        boolean clearResult = sink.clear(mapping);
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
        assertThat("records type is not available", recordsStatus.getStatusCode(), is(404));
        StatusLine mtStatus = Request
                .Head("http://localhost:9200/" + mapping.getIndex() + "/mt").execute()
                .returnResponse().getStatusLine();
        assertThat("metadata type is not available", mtStatus.getStatusCode(), is(404));
    }

}
