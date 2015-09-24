/*
 * Copyright 2015-${currentYearDynamic} 52°North Initiative for Geospatial Open Source
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
import java.util.Collection;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.client.IndicesAdminClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.n52.youngs.api.Record;
import org.n52.youngs.harvest.NamespaceContextImpl;
import org.n52.youngs.load.impl.ElasticsearchRemoteHttpSink;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchSinkIT {

    private ElasticsearchRemoteHttpSink sink;

    private YamlMappingConfiguration mapping;

    private final String index = "testindex";

    private final String type = "record";

    @ClassRule
    public static ElasticsearchServer server = new ElasticsearchServer();
    
    @Before
    public void createSink() {
        sink = new ElasticsearchRemoteHttpSink("localhost", 9300, "elasticsearch", index, type);
    }

    @Before
    public void loadMapping() throws IOException {
        mapping = new YamlMappingConfiguration(Resources.asByteSource(Resources.getResource("mappings/testmapping.yml")).openStream(), NamespaceContextImpl.create());
    }

    @Test
    public void insertSchema() throws IOException {
        sink.prepare(mapping);

        // TODO add assertions reading mapping
        IndicesAdminClient indicesClient = sink.getClient().admin().indices();
        GetMappingsRequestBuilder builder = new GetMappingsRequestBuilder(indicesClient, index)
                .addTypes(type);
        GetMappingsResponse response = indicesClient.getMappings(builder.request()).actionGet();
        System.out.println(response);
    }

    @Test
    public void store() throws Exception {
        sink.prepare(mapping);

        Collection<Record> records = Util.loadGetRecordsResponse(Resources.asByteSource(Resources.getResource("responses/dab-getrecords.xml")).openStream());

        sink.store(records);
    }

}
