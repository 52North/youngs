/*
 * Copyright 2015-2019 52°North Initiative for Geospatial Open Source
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchClientSink extends ElasticsearchSink {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchClientSink.class);

    private final Client client;

    public ElasticsearchClientSink(Client client, String cluster, String index, String type) {
        super(cluster, index, type);
        this.client = client;
        log.info("Created new client with client {}", client);
    }

    @Override
    public Client getClient() {
        return this.client;
    }

}
