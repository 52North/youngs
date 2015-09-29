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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.youngs.test;

import java.util.Optional;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.ExternalResource;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ElasticsearchServer extends ExternalResource {

    private static final Boolean START_ES_DEFAULT = false;

    public static final String INDEX = "elasticsearch";

    public static final String TYPE = "testrecord";

    public static String cluster = null;

    private Node embeddedNode = null;

    private Client client;

    private static final String SYSTEM_PROPERTY_START_ES = "test.start.elasticsearch";

    private Boolean startElasticsearch;

    public ElasticsearchServer() {
        this(START_ES_DEFAULT);
    }

    public ElasticsearchServer(boolean startEs) {
        this.startElasticsearch = startEs;
    }

    @Override
    protected void before() throws Throwable {
        boolean startEs = Boolean.valueOf(
                Optional.ofNullable(
                        System.getProperty(SYSTEM_PROPERTY_START_ES))
                .orElse(startElasticsearch.toString()));
        if (startEs) {
            Settings settings = ImmutableSettings.settingsBuilder().loadFromClasspath("elasticsearch-it.yml").build();
            cluster = settings.get("cluster.name");

            embeddedNode = NodeBuilder.nodeBuilder().settings(settings).build();
            embeddedNode.start();
            client = embeddedNode.client();
            System.out.println(String.format("### Elasticsearch server '%s' started ###", cluster));
        }
    }

    @Override
    protected void after() {
        if (Optional.ofNullable(embeddedNode).isPresent()) {
            embeddedNode.close();
            System.out.println("### Elasticsearch server closed ###");
        }
    }

    public Client getClient() {
        return client;
    }

}