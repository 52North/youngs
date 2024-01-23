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
package org.n52.youngs.load.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import co.elastic.clients.transport.rest_client.RestClientTransport;
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

    private final ElasticsearchClient client;

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type) {
        this(host, port, cluster, index, type, Mode.TRANSPORT);
    }

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type,
            Mode mode) {
        super(cluster, index, type);
        this.host = host;
        this.port = port;
        String serverUrl = "http://" + this.host + ":" + this.port;

        RestClient restClient = RestClient
                .builder(HttpHost.create(serverUrl))
                .build();

        try {
            switch (mode) {
                case TRANSPORT:
                    // Create the transport with a Jackson mapper
                    ElasticsearchTransport transport = new RestClientTransport(
                            restClient, new JacksonJsonpMapper());

                    // And create the API client
                    this.client = new ElasticsearchClient(transport);
                    break;
                case NODE:
                    throw new SinkError("Node mode is deprecated: %s", mode);
                default:
                    throw new SinkError("Unsupported mode %s", mode);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        log.info("Created new client with settings {}:\n{}", serverUrl, client);
    }

    @Override
    public ElasticsearchClient getClient() {
        return this.client;
    }

}
