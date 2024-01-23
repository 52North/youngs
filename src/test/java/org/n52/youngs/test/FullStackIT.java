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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.n52.youngs.api.Report;
import org.n52.youngs.control.Runner;
import org.n52.youngs.control.impl.SingleThreadBulkRunner;
import org.n52.youngs.harvest.CswSource;
import org.n52.youngs.harvest.DirectorySource;
import org.n52.youngs.harvest.PoxCswSource;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.impl.ElasticsearchRemoteHttpSink;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class FullStackIT {

    private static Path baseDirectory;

    private static Mapper cswMapper;


    @BeforeClass
    public static void baseDir() throws URISyntaxException, IOException {
        baseDirectory = Paths.get(Resources.getResource("records").toURI());

        MappingConfiguration cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void testCswRecordsDirectoryToElasticsearchSink() throws IOException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));

        String host = "localhost";
        String cluster = "elasticsearch";
        String index = "csw";
        String type = "record";
        int port = 9300;
        Sink sink = new ElasticsearchRemoteHttpSink(host, port, cluster, index, type);

        Runner runner = new SingleThreadBulkRunner()
                .setBulkSize(1000)
                .setRecordsLimit(50)
                .harvest(source)
                .transform(cswMapper);
        Report report = runner.load(sink);

        assertThat("all records added", report.getNumberOfRecordsAdded(), is(equalTo(50)));
    }

    @Test
    public void testPoxClientToElasticsearchSink() throws Exception {
        CswSource source = new PoxCswSource(new URL("http://api.eurogeoss-broker.eu/dab/services/cswiso"),
                (Collection<String>) ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2"), NamespaceContextImpl.create(),
                "csw:Record", "http://www.opengis.net/cat/csw/2.0.2");

        String host = "localhost";
        String cluster = "elasticsearch";
        String index = "csw";
        String type = "record";
        int port = 9300;
        Sink sink = new ElasticsearchRemoteHttpSink(host, port, cluster, index, type);

        Runner runner = new SingleThreadBulkRunner()
                .setBulkSize(1000)
                .setRecordsLimit(50)
                .harvest(source)
                .transform(cswMapper);
        Report report = runner.load(sink);

        assertThat("all records added", report.getNumberOfRecordsAdded(), is(equalTo(50)));
    }

}
