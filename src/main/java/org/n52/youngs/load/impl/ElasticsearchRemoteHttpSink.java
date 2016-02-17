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

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchRemoteHttpSink extends ElasticsearchSink {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchRemoteHttpSink.class);

    private final String host;

    private final int port;

    private final TransportClient client;

    public ElasticsearchRemoteHttpSink(String host, int port, String cluster, String index, String type) {
        super(cluster, index, type);
        this.host = host;
        this.port = port;

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", getCluster()).build();
        this.client = new TransportClient(settings).addTransportAddress(
                new InetSocketTransportAddress(this.host, this.port));
        log.info("Created new client with settings {}", settings);
    }

    @Override
    public Client getClient() {
        return this.client;
    }

}
