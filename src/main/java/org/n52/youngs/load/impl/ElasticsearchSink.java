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
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.SinkRecord;
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
    public boolean store(SinkRecord record) {
        log.trace("Storing record: {}", record);
        Objects.nonNull(record);

        if (record instanceof BuilderRecord) {
            BuilderRecord builderRecord = (BuilderRecord) record;
            Client client = getClient();

            log.trace("Indexing record: {}", record);
            IndexRequestBuilder request = client.prepareIndex(index, type)
                    .setSource(builderRecord.getBuilder());
            if (record.hasId()) {
                request.setId(builderRecord.getId());
            }

            IndexResponse response = request.execute().actionGet();
            log.trace("Created [{}] with id {} @ {}/{}, version {}", response.isCreated(),
                    response.getId(), response.getIndex(), response.getType(), response.getVersion());

            return true;
        } else {
            throw new InvalidParameterException(
                    String.format("The provided record class '%s' is not supported", record.getClass()));
        }
    }

    @Override
    public boolean store(Collection<SinkRecord> records) {
        // TODO evaluate parallelStream()
        long addedRecords = records.stream().map(this::store).filter(b -> b).count();
        return addedRecords == records.size();
    }

    public ElasticsearchSink setSchemaGenerator(SchemaGenerator sg) {
        this.schemaGenerator = sg;
        return this;
    }

}
