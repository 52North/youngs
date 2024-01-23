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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.n52.youngs.harvest.DirectorySource;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.ReportImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.load.impl.ElasticsearchClientSink;
import org.n52.youngs.load.impl.ElasticsearchRemoteHttpSink;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import static org.n52.youngs.util.JsonMatchers.hasJsonPath;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SpatialSearchIT {

    private static MappingConfiguration mapping;

    private static Sink sink;

    @BeforeClass
    public static void prepareAndStoreSink() throws Exception {
        mapping = new YamlMappingConfiguration(Resources.asByteSource(
                Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper());
//        sink = new ElasticsearchRemoteHttpSink("localhost", 9300, "elasticsearch", mapping.getIndex(), mapping.getType());
        sink = new ElasticsearchRemoteHttpSink("localhost", 9300, "elasticsearch", mapping.getIndex(), mapping.getType());
        sink.prepare(mapping);
        Mapper mapper = new CswToBuilderMapper(mapping);

        DirectorySource source = new DirectorySource(
                Paths.get(Resources.getResource("records").toURI()).resolve("csw"));
        List<SinkRecord> mappedRecords = source.getRecords(new ReportImpl()).stream().map(mapper::map).collect(Collectors.toList());
        boolean stored = sink.store(mappedRecords);

        Thread.sleep(1000);
        assertThat("all records stored", stored);
    }

    @AfterClass
    public static void clearSink() throws IOException {
        boolean result = sink.clear(mapping);
        assertThat("sink is cleared", result, is(true));
    }

    /** records with bounding boxes:

     urn:uuid:9a669547-b69b-469f-a11f-2d875366bbdc:
     <ows:BoundingBox crs="urn:x-ogc:def:crs:EPSG:6.11:4326">
     <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>
     <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>
     </ows:BoundingBox>

     urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63:
     <ows:BoundingBox crs="urn:x-ogc:def:crs:EPSG:6.11:4326">
     <ows:LowerCorner>47.595 -4.097</ows:LowerCorner>
     <ows:UpperCorner>51.217 0.889</ows:UpperCorner>
     </ows:BoundingBox>

     urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd:
     <ows:BoundingBox crs="urn:x-ogc:def:crs:EPSG:6.11:4326">
     <ows:LowerCorner>60.042 13.754</ows:LowerCorner>
     <ows:UpperCorner>68.410 17.920</ows:UpperCorner>
     </ows:BoundingBox>
     */
    @Test
    public void spatialQueryPointSearch() throws Exception {
        String endpoint = "http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType()
                + "/_search?pretty";

        String pointInOneRecordQuery = "{"
                + "    \"query\":{"
                + "        \"filtered\":{"
                + "            \"query\":{"
                + "                \"match_all\":{"
                + ""
                + "                }"
                + "            },"
                + "            \"filter\":{"
                + "                \"geo_shape\":{"
                + "                    \"location\":{"
                + "                        \"shape\":{"
                + "                            \"type\":\"point\","
                + "                            \"coordinates\":["
                + "                                62.0,"
                + "                                15.0"
                + "                            ]"
                + "                        }"
                + "                    }"
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}";

        String searchWithPointResponse = Request
                .Post(endpoint).bodyString(pointInOneRecordQuery, ContentType.APPLICATION_JSON).execute()
                .returnContent().asString();
        assertThat("correct number of records found", searchWithPointResponse, hasJsonPath("hits.total", is(1)));
        assertThat("ids are contained", searchWithPointResponse, allOf(
                containsString("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd"),
                not(containsString("urn:uuid:9a669547-b69b-469f-a11f-2d875366bbdc")),
                not(containsString("urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63"))));
    }

    @Test
    public void spatialQueryEnvelopeSearch() throws Exception {
        String endpoint = "http://localhost:9200/" + mapping.getIndex() + "/" + mapping.getType()
                + "/_search?pretty";

        String bboxCoveringTwoRecordsQuery = "{"
                + "    \"query\":{"
                + "        \"filtered\":{"
                + "            \"query\":{"
                + "                \"match_all\":{"
                + "                }"
                + "            },"
                + "            \"filter\":{"
                + "                \"geo_shape\":{"
                + "                    \"location\":{"
                + "                        \"shape\":{"
                + "                            \"type\":\"envelope\","
                + "                            \"coordinates\":["
                + "                                ["
                + "                                    52.0,"
                + "                                    -5.0"
                + "                                ],"
                + "                                ["
                + "                                    40.0,"
                + "                                    6.5"
                + "                                ]"
                + "                            ]"
                + "                        }"
                + "                    }"
                + "                }"
                + "            }"
                + "        }"
                + "    }"
                + "}";

        String searchWithEnvelopeResponse = Request
                .Post(endpoint).bodyString(bboxCoveringTwoRecordsQuery, ContentType.APPLICATION_JSON).execute()
                .returnContent().asString();
        assertThat("correct number of records found", searchWithEnvelopeResponse, hasJsonPath("hits.total", is(2)));
        assertThat("ids are contained", searchWithEnvelopeResponse, allOf(
                not(containsString("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd")),
                containsString("urn:uuid:9a669547-b69b-469f-a11f-2d875366bbdc"),
                containsString("urn:uuid:94bc9c83-97f6-4b40-9eb8-a8e8787a5c63")));
    }

}
