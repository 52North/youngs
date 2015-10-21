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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.indices.IndexMissingException;
import org.joda.time.DateTimeZone;
import org.n52.iceland.statistics.api.mappings.MetadataDataMapping;
import org.n52.iceland.statistics.api.parameters.AbstractEsParameter;
import org.n52.iceland.statistics.api.parameters.ObjectEsParameter;
import org.n52.iceland.statistics.api.parameters.SingleEsParameter;
import org.n52.youngs.exception.SinkError;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.transform.MappingConfiguration;
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

            try {
                IndexResponse response = request.execute().actionGet();
                log.trace("Created [{}] with id {} @ {}/{}, version {}", response.isCreated(),
                        response.getId(), response.getIndex(), response.getType(), response.getVersion());

                return response.isCreated() || (!response.isCreated() && (response.getVersion() > 1));
            } catch (ElasticsearchException e) {
                log.error("Could not store record {}", builderRecord.getId(), e);
                return false;
            }
        } else {
            throw new SinkError("The provided record class '%s' is not supported", record.getClass());
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

    @Override
    public boolean prepare(MappingConfiguration mapping) {
        if (!mapping.isIndexCreationEnabled()) {
            log.info("Index creation is disabled, stopping preparations!");
            return false;
        }

        try {
            IndicesAdminClient indices = getClient().admin().indices();
            String indexId = mapping.getIndex();
            if (indices.prepareExists(indexId).get().isExists()) {
                log.info("Index {} already exists, updating the mapping ...", indexId);
                return updateMapping(indexId, mapping);
            } else {
                log.info("Index {} does not exist, creating it ...", indexId);
                return createMapping(mapping, indexId);
            }
        } catch (RuntimeException e) {
            throw new SinkError(e, "Problem preparing sink: %s", e.getMessage());
        }
    }

    protected boolean createMapping(MappingConfiguration mapping, String indexId) {
        IndicesAdminClient indices = getClient().admin().indices();

        Map<String, Object> schema = schemaGenerator.generate(mapping);
        log.trace("Built schema creation request:\n{}", Arrays.toString(schema.entrySet().toArray()));

        // create metadata mapping and schema mapping
        CreateIndexRequestBuilder request = indices.prepareCreate(indexId)
                .addMapping(MetadataDataMapping.METADATA_TYPE_NAME, getMetadataSchema())
                .addMapping(mapping.getType(), schema);
        if (mapping.hasIndexCreationRequest()) {
            request.setSettings(mapping.getIndexCreationRequest());
        }

        CreateIndexResponse response = request.get();
        log.debug("Created indices: {}, acknowledged: {}", response, response.isAcknowledged());

        Map<String, Object> mdRecord = createMetadataRecord(mapping.getVersion(), mapping.getName());
        IndexResponse mdResponse = getClient().prepareIndex(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID).setSource(mdRecord).get();
        log.debug("Saved mapping metadata '{}': {}", mdResponse.isCreated(), Arrays.toString(mdRecord.entrySet().toArray()));

        return (mdResponse.isCreated() && response.isAcknowledged());
    }

    protected boolean updateMapping(String indexId, MappingConfiguration mapping) throws SinkError {
        double version = getCurrentVersion(indexId);
        log.info("Existing mapping version is {}, vs. c version {}", version, mapping.getVersion());
        if (version < 0) {
            throw new SinkError("Database inconsistency. Metadata version not found in type %s", MetadataDataMapping.METADATA_TYPE_NAME);
        }
        if (version != mapping.getVersion()) {
            throw new SinkError("Database schema version inconsistency. Version numbers don't match. Database version number %d != mapping version number %d",
                    version, mapping.getVersion());
        }

        // schema can be updated
        Map<String, Object> schema = schemaGenerator.generate(mapping);

        PutMappingRequestBuilder request = getClient().admin().indices()
                .preparePutMapping(indexId)
                .setType(mapping.getType())
                .setSource(schema);
        PutMappingResponse updateMappingResponse = request.get();
        log.info("Update mapping of type {} acknowledged: {}", mapping.getType(), updateMappingResponse.isAcknowledged());
        if (!updateMappingResponse.isAcknowledged()) {
            log.error("Problem updating mapping for type {}", mapping.getType());
        }

        Map<String, Object> updatedMetadata = createUpdatedMetadata(indexId);
        UpdateResponse mdUpdate = getClient().prepareUpdate(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
                .setDoc(updatedMetadata).get();
        log.info("Update metadata record created: {} | id = {} @ {}/{}",
                mdUpdate.isCreated(), mdUpdate.getId(), mdUpdate.getIndex(), mdUpdate.getType());

        return (mdUpdate.getId().equals(MetadataDataMapping.METADATA_ROW_ID)
                && updateMappingResponse.isAcknowledged());
    }

    private double getCurrentVersion(String indexId) {
        GetResponse resp = getClient().prepareGet(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
                .setOperationThreaded(false).get();
        if (resp.isExists()) {
            Object versionString = resp.getSourceAsMap().get(MetadataDataMapping.METADATA_VERSION_FIELD.getName());
            if (versionString == null) {
                throw new ElasticsearchException(String.format("Database inconsistency. Version can't be found in row %s/%s/%s",
                        indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID));
            }
            return Double.valueOf(versionString.toString());
        } else {
            return Double.MIN_VALUE;
        }
    }

    private Map<String, Object> createUpdatedMetadata(String indexId) throws SinkError {
        GetResponse resp = getClient().prepareGet(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
                .setOperationThreaded(false).get();
        Object retrievedValues = resp.getSourceAsMap().get(MetadataDataMapping.METADATA_UUIDS_FIELD.getName());
        List<String> values;

        if (retrievedValues instanceof String) {
            values = new LinkedList<>();
            values.add((String) retrievedValues);
        } else if (retrievedValues instanceof List<?>) {
            values = (List<String>) retrievedValues;
        } else {
            throw new SinkError("Invalid %s field type %s should have String or java.util.Collection<String>",
                    MetadataDataMapping.METADATA_UUIDS_FIELD, retrievedValues.getClass());
        }

        String uuid = UUID.randomUUID().toString();
        Map<String, Object> updatedMetadata = Maps.newHashMap();
        values.add(uuid);
        updatedMetadata.put(MetadataDataMapping.METADATA_UUIDS_FIELD.getName(), values);
        updatedMetadata.put(MetadataDataMapping.METADATA_UPDATE_TIME_FIELD.getName(), Calendar.getInstance(DateTimeZone.UTC.toTimeZone()));
        log.info("UUID {} is added to the {} type", uuid, MetadataDataMapping.METADATA_TYPE_NAME);

        return updatedMetadata;
    }

    private Map<String, Object> getMetadataSchema() {
        HashMap<String, Object> mappings = Maps.newHashMap();

        for (Field field : MetadataDataMapping.class.getDeclaredFields()) {
            AbstractEsParameter value = checkField(field);
            if (value != null) {
                resolveParameterField(value, mappings);
            }
        }

        HashMap<String, Object> properties = Maps.newHashMapWithExpectedSize(1);
        properties.put("properties", mappings);
        return properties;
    }

    private void resolveParameterField(AbstractEsParameter value, Map<String, Object> map) {
        if (value instanceof SingleEsParameter) {
            SingleEsParameter single = (SingleEsParameter) value;
            map.put(single.getName(), single.getTypeAsMap());
        } else if (value instanceof ObjectEsParameter) {
            ObjectEsParameter object = (ObjectEsParameter) value;

            // loadup all the children
            // the wrapper properties map is needed to elasticsearch
            Map<String, Object> subproperties = new HashMap<>(1);
            Map<String, Object> childrenMap = new HashMap<>(value.getAllChildren().size());
            subproperties.put("properties", childrenMap);

            for (AbstractEsParameter child : object.getAllChildren()) {
                resolveParameterField(child, childrenMap);
            }

            map.put(object.getName(), subproperties);

        } else {
            throw new IllegalArgumentException("Invalid schema parameter value " + value.toString());
        }
    }

    private AbstractEsParameter checkField(Field field) {
        boolean bool = Modifier.isFinal(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers());
        bool = bool && field.getType().isAssignableFrom((AbstractEsParameter.class));
        if (bool) {
            try {
                return (AbstractEsParameter) field.get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                log.error("Error retrieving field.", e);
            }
        }
        return null;
    }

    private Map<String, Object> createMetadataRecord(int version, String name) {
        String uuid = UUID.randomUUID().toString();
        Map<String, Object> data = new HashMap<>();
        Calendar time = Calendar.getInstance(DateTimeZone.UTC.toTimeZone());
        data.put(MetadataDataMapping.METADATA_CREATION_TIME_FIELD.getName(), time);
        data.put(MetadataDataMapping.METADATA_UPDATE_TIME_FIELD.getName(), time);
        data.put(MetadataDataMapping.METADATA_VERSION_FIELD.getName(), version);
        data.put(YoungsMetadataDataMapping.METADATA_NAME_FIELD.getName(), name);
        data.put(MetadataDataMapping.METADATA_UUIDS_FIELD.getName(), uuid);
        log.info("Initial metadata is created ceated for type {} with uuid {} @ {}",
                MetadataDataMapping.METADATA_TYPE_NAME, uuid, time);

        return data;
    }

    @Override
    public boolean clear(MappingConfiguration mapping) {
        log.info("Deleting index '{}'", mapping.getIndex());
        DeleteIndexRequest request = new DeleteIndexRequest(mapping.getIndex());
        try {
            DeleteIndexResponse delete = getClient().admin().indices().delete(request).actionGet();
            log.info("Delete acknowledged: {}", delete.isAcknowledged());
            return delete.isAcknowledged();
        } catch (IndexMissingException e) {
            log.info("Index does not exist, no need to delete: {}", e.getMessage());
            return true;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cluster", cluster)
                .add("index", index)
                .add("type", type)
                .add("client", getClient())
                .toString();
    }

}
