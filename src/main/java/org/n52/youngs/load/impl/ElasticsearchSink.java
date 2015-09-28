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
package org.n52.youngs.load.impl;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Objects;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.n52.youngs.api.Record;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.load.Sink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public abstract class ElasticsearchSink implements Sink {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSink.class);

    private final String cluster;

    private final String index;

    private final String type;

    protected SchemaGenerator schemaGenerator = new SchemaGeneratorImpl();

    public ElasticsearchSink(String cluster, String index, String type) {
        this.cluster = cluster;
        this.index = index;
        this.type = type;
    }

    public abstract Client getClient();

    protected String getCluster() {
        return cluster;
    }

    @Override
    public boolean store(Record record) {
        Objects.nonNull(record);

        if (record instanceof BuilderRecord) {
            BuilderRecord builderRecord = (BuilderRecord) record;

            XContentBuilder builder = builderRecord.getBuilder();

            Client client = getClient();

            log.trace("Indexing record: {}", record);
            IndexResponse response = client.prepareIndex(index, type).setSource(builder).execute().actionGet();
            log.trace("Got index reponse: {}", response);

            return true;
        } else {
            throw new InvalidParameterException("The provided record is not supported");
        }
    }

    @Override
    public void store(Collection<Record> records) {
        // TODO evaluate parallelStream()
        records.stream().forEach(this::store);
    }

    public ElasticsearchSink setSchemaGenerator(SchemaGenerator sg) {
        this.schemaGenerator = sg;
        return this;
    }

}
