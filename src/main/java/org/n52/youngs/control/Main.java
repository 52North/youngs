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
package org.n52.youngs.control;

import com.google.common.io.Resources;
import java.util.Collections;
import org.n52.youngs.load.impl.ElasticsearchRemoteHttpSink;
import org.n52.youngs.api.Report;
import org.n52.youngs.control.impl.SingleThreadBulkRunner;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.harvest.KvpCswSource;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.Sink;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

//        Report report = dabCsw();
        Report report = dabGmd();

        log.info("Done:\n{}", report);
    }

    public static Report dabGmd() throws Exception {
        Source source = new KvpCswSource("http://api.eurogeoss-broker.eu/dab/services/cswiso",
                Collections.singleton("http://www.isotc211.org/2005/gmd"),
                NamespaceContextImpl.create(),
                "gmd:MD_Metadata",
                "http://www.isotc211.org/2005/gmd");

        MappingConfiguration configuration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/gmd-metadata.yml")).openStream(),
                new XPathHelper());
        Mapper mapper = new CswToBuilderMapper(configuration);

        String host = "localhost";
        String cluster = "ConnectinGEO";
        String index = "geodab";
        String type = "isorecord";
        int port = 9301;
        Sink sink = new ElasticsearchRemoteHttpSink(host, port, cluster, index, type);

        Runner runner = new SingleThreadBulkRunner()
                .setBulkSize(10)
                .setRecordsLimit(10000)
                .setStartPosition(10000)
                .harvest(source)
                .transform(mapper);
        Report report = runner.load(sink);
        return report;
    }

    public static Report dabCsw() throws Exception {
        // http://api.eurogeoss-broker.eu/dab/services/cswiso?service=CSW&version=2.0.2&request=GetCapabilities
        Source source = new KvpCswSource("http://api.eurogeoss-broker.eu/dab/services/cswiso",
                Collections.singleton("http://www.opengis.net/cat/csw/2.0.2"),
                NamespaceContextImpl.create(),
                "csw:Record",
                "http://www.opengis.net/cat/csw/2.0.2");

        MappingConfiguration configuration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper());
        Mapper mapper = new CswToBuilderMapper(configuration);

        String host = "localhost";
        String cluster = "ConnectinGEO";
        String index = "geodab";
        String type = "dcrecord";
        int port = 9301;
        Sink sink = new ElasticsearchRemoteHttpSink(host, port, cluster, index, type);

        Runner runner = new SingleThreadBulkRunner()
                .setBulkSize(20)
                .setRecordsLimit(10)
                .setStartPosition(1)
                .harvest(source)
                .transform(mapper);
        Report report = runner.load(sink);
        return report;
    }

}
