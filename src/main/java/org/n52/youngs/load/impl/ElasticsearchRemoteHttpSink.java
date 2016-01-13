/*
 * Copyright 2015-2016 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.load.impl;

import com.google.common.io.Files;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.n52.youngs.exception.SinkError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchRemoteHttpSink extends ElasticsearchSink {

    public static enum Mode {

        NODE, TRANSPORT;
    }

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchRemoteHttpSink.class);

    private final String host;

    private final int port;

    private final Client client;

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type) {
        this(host, port, cluster, index, type, Mode.TRANSPORT);
    }

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type,
            Mode mode) {
        super(cluster, index, type);
        this.host = host;
        this.port = port;

        Settings.Builder settings = Settings.settingsBuilder()
                .put("cluster.name", getCluster());

        try {
            switch (mode) {
                case TRANSPORT:
                    TransportClient tClient = TransportClient.builder().settings(settings).build();
                    tClient.addTransportAddress(
                            new InetSocketTransportAddress(InetAddress.getByName(this.host), this.port));
                    this.client = tClient;
                    break;
                case NODE:
                    settings.put("http.enabled", false);
                    settings.put("path.home", Files.createTempDir());
                    Node node = NodeBuilder.nodeBuilder()
                            .settings(settings)
                            .data(false)
                            .client(true)
                            .node();
                    this.client = node.client();
                    break;
                default:
                    throw new SinkError("Unsupported mode %s", mode);
            }
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        log.info("Created new client with settings {}:\n{}", settings, client);
    }

    @Override
    public Client getClient() {
        return this.client;
    }

}
