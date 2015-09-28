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

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.n52.youngs.api.Record;
import org.n52.youngs.api.Report;
import org.n52.youngs.control.Runner;
import org.n52.youngs.control.impl.SingleThreadBulkRunner;
import org.n52.youngs.harvest.DirectorySource;
import org.n52.youngs.impl.NamespaceContextImpl;
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
public class DirectorySourceIT {

    private static Path baseDirectory;

    private static Mapper cswMapper;

    @ClassRule
    public static ElasticsearchServer server = new ElasticsearchServer(true);

    @BeforeClass
    public static void baseDir() throws URISyntaxException, IOException {
        baseDirectory = Paths.get(Resources.getResource("records").toURI());

        MappingConfiguration cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/csw-record.yml")).openStream(),
                NamespaceContextImpl.create());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void testCswRecordsDirectory() throws IOException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));

        String host = "localhost";
        String cluster = "elasticsearch";
        String index = "csw";
        String type = "record";
        int port = 9300;
        Sink sink = new ElasticsearchRemoteHttpSink(host, port, cluster, index, type);

        Runner runner = new SingleThreadBulkRunner()
                .setBulkSize(12)
                .setRecordsLimit(50)
                .harvest(source)
                .transform(cswMapper);
        Report report = runner.load(sink);

        assertThat("all records added", report.getNumberOfRecordsAdded(), is(equalTo(12)));
    }

}
