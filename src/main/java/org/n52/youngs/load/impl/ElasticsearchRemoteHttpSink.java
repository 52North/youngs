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
package org.n52.youngs.load.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestHighLevelClientBuilder;
import org.elasticsearch.common.settings.Settings;
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

    private final RestHighLevelClient client;

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type) {
        this(host, port, cluster, index, type, Mode.TRANSPORT);
    }

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type,
            Mode mode) {
        super(cluster, index, type);
        this.host = host;
        this.port = port;

        Settings.Builder settings = Settings.builder()
                .put("cluster.name", getCluster());

        try {
            switch (mode) {
                case TRANSPORT:
                    RestClient restClient = RestClient.builder(new HttpHost(InetAddress.getByName(this.host), this.port)).build();
                    RestHighLevelClient highLevelClient = new RestHighLevelClientBuilder(restClient).setApiCompatibilityMode(true).build(); //compatibilty mode for ES 8.x
                    this.client = highLevelClient;
                    break;

                case NODE:
                throw new SinkError("Node mode is deprecated: %s", mode);
                default:
                    throw new SinkError("Unsupported mode %s", mode);
            }
        } catch (UnknownHostException ex) {
            throw new RuntimeException(ex);
        }
        log.info("Created new client with settings {}:\n{}", settings, client);
    }

    @Override
    public RestHighLevelClient getClient() {
        return this.client;
    }

}
