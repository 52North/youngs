/*
 * Copyright 2015-2023 52°North Spatial Information Research GmbH
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.joda.time.DateTimeZone;
import org.n52.iceland.statistics.api.mappings.MetadataDataMapping;
import org.n52.iceland.statistics.api.parameters.AbstractEsParameter;
import org.n52.iceland.statistics.api.parameters.ElasticsearchTypeRegistry;
import org.n52.iceland.statistics.api.parameters.ObjectEsParameter;
import org.n52.iceland.statistics.api.parameters.SingleEsParameter;
import org.n52.youngs.exception.SinkError;
import org.n52.youngs.exception.SinkException;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.load.Sink;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

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

    protected final String getCluster() {
        return cluster;
    }

    @Override
    public void storeWithExceptions(SinkRecord record) throws SinkException {
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
                log.trace("Sending record to sink...");
                IndexResponse response = request.execute().actionGet();
                log.trace("Created [{}] with id {} @ {}/{}, version {}", response.getResult() == DocWriteResponse.Result.CREATED,
                        response.getId(), response.getIndex(), response.getType(), response.getVersion());

                if (response.getResult() == DocWriteResponse.Result.CREATED || (response.getResult() != DocWriteResponse.Result.CREATED && (response.getVersion() > 1))) {
                    return;
                } else {
                    throw new SinkException("Record '%s' could not be stored due to unforeseen error.", builderRecord.getId());
                }
            } catch (ElasticsearchException e) {
                throw new SinkException(String.format("Could not store record '%s'", builderRecord.getId()), e);
            }
        } else {
            throw new SinkError("The provided record class '%s' is not supported", record.getClass());
        }
    }

    @Override
    public boolean store(SinkRecord record) {
        try {
            this.storeWithExceptions(record);
        } catch (SinkException e) {
            log.error("Could not store record {}", record.getId(), e);
            return false;
        }
        return true;
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
                if (metaIndexExists(indexId)) {
                    log.info("meta index already exists for index {}", indexId);
                    log.info("delete existing meta index for index {} before re-creating it", indexId);
                    boolean isDeleteAcknowledged = deleteIndexById(deriveMetadataIndexName(indexId));

                    if(!isDeleteAcknowledged){
                        log.warn("failed to delete meta index {} for index {}", deriveMetadataIndexName(indexId), indexId);
                    }
                }
                return createMapping(mapping, indexId);
            }
        } catch (RuntimeException e) {
            log.warn(e.getMessage(), e);
            throw new SinkError(e, "Problem preparing sink: %s", e.getMessage());
        }
    }

    protected boolean createMapping(MappingConfiguration mapping, String indexId) {
        IndicesAdminClient indices = getClient().admin().indices();

        Map<String, Object> schema = schemaGenerator.generate(mapping);
        log.trace("Built schema creation request:\n{}", Arrays.toString(schema.entrySet().toArray()));

        // create metadata mapping and schema mapping
        CreateIndexRequestBuilder request = indices.prepareCreate(indexId)
                .addMapping(mapping.getType(), schema);
        if (mapping.hasIndexCreationRequest()) {
            request.setSettings(mapping.getIndexCreationRequest(), XContentType.YAML);
        }

        CreateIndexResponse response = request.get();
        log.debug("Created indices: {}, acknowledged: {}", response, response.isAcknowledged());

        // elasticsearch 6.x removed support for multiple types in one index, we need a separate one
        // create metadata mapping and schema mapping
        CreateIndexRequestBuilder requestMeta = indices.prepareCreate(deriveMetadataIndexName(indexId))
                .addMapping(MetadataDataMapping.METADATA_TYPE_NAME, getMetadataSchema());
        if (mapping.hasIndexCreationRequest()) {
            requestMeta.setSettings(mapping.getIndexCreationRequest(), XContentType.YAML);
        }

        CreateIndexResponse responseMeta = requestMeta.get();
        log.debug("Created indices: {}, acknowledged: {}", responseMeta, responseMeta.isAcknowledged());

        Map<String, Object> mdRecord = createMetadataRecord(mapping.getVersion(), mapping.getName());
        IndexResponse mdResponse = getClient().prepareIndex(deriveMetadataIndexName(indexId), MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID).setSource(mdRecord).get();
        log.debug("Saved mapping metadata '{}': {}", mdResponse.getResult() == DocWriteResponse.Result.CREATED, Arrays.toString(mdRecord.entrySet().toArray()));

        return response.isAcknowledged();
    }

    protected boolean updateMapping(String indexId, MappingConfiguration mapping) throws SinkError {
        double version = getCurrentVersion(indexId);
        log.info("Existing mapping version is {}, vs. c version {}", version, mapping.getVersion());
        if (version < 0) {
            throw new SinkError("Database inconsistency. Metadata version not found in type %s", MetadataDataMapping.METADATA_TYPE_NAME);
        }
        if (version != mapping.getVersion()) {
            throw new SinkError("Database schema version inconsistency. Version numbers don't match. Database version number %s != mapping version number %s",
                    version, mapping.getVersion());
        }

        // schema can be updated
        Map<String, Object> schema = schemaGenerator.generate(mapping);

        PutMappingRequestBuilder request = getClient().admin().indices()
                .preparePutMapping(indexId)
                .setType(mapping.getType())
                .setSource(schema);
        AcknowledgedResponse updateMappingResponse = request.get();
        log.info("Update mapping of type {} acknowledged: {}", mapping.getType(), updateMappingResponse.isAcknowledged());
        if (!updateMappingResponse.isAcknowledged()) {
            log.error("Problem updating mapping for type {}", mapping.getType());
        }

        Map<String, Object> updatedMetadata = createUpdatedMetadata(deriveMetadataIndexName(indexId));
        UpdateResponse mdUpdate = getClient().prepareUpdate(deriveMetadataIndexName(indexId), MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
                .setDoc(updatedMetadata).get();
        log.info("Update metadata record created: {} | id = {} @ {}/{}",
                mdUpdate.getResult() == DocWriteResponse.Result.CREATED, mdUpdate.getId(), mdUpdate.getIndex(), mdUpdate.getType());

        return (mdUpdate.getId().equals(MetadataDataMapping.METADATA_ROW_ID)
                && updateMappingResponse.isAcknowledged());
    }

    private double getCurrentVersion(String indexId) {
        GetResponse resp = getClient().prepareGet(deriveMetadataIndexName(indexId), MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
                .get();
        if (resp.isExists()) {
            Object versionString = resp.getSourceAsMap().get(MetadataDataMapping.METADATA_VERSION_FIELD.getName());
            if (versionString == null) {
                throw new ElasticsearchException(String.format("Database inconsistency. Version can't be found in row %s/%s/%s",
                        deriveMetadataIndexName(indexId), MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID));
            }
            return Double.valueOf(versionString.toString());
        } else {
            return Double.MIN_VALUE;
        }
    }

    private Map<String, Object> createUpdatedMetadata(String indexId) throws SinkError {
        GetResponse resp = getClient().prepareGet(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
                .get();
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
            if (single.getType() == ElasticsearchTypeRegistry.stringField) {
                map.put(single.getName(), new ElasticsearchTypeRegistry.ElasticsearchType(ImmutableMap.<String, Object>of("type", "text", "index", "false")).getType());
            } else {
                map.put(single.getName(), single.getTypeAsMap());
            }
        } else if (value instanceof ObjectEsParameter) {
            ObjectEsParameter object = (ObjectEsParameter) value;

            // loadup all the children
            // the wrapper properties map is needed to elasticsearch
            Map<String, Object> subproperties = new HashMap<>(1);
            Map<String, Object> childrenMap = new HashMap<>(value.getAllChildren().size());
            subproperties.put("properties", childrenMap);

            object.getAllChildren().forEach((child) -> {
                resolveParameterField(child, childrenMap);
            });

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
        return deleteIndexById(mapping.getIndex());
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

    private String deriveMetadataIndexName(String indexId) {
        return indexId + "-meta";
    }

    /**
     * @param indexId
     * @return true if meta index for provided index already exists
     */
    private boolean metaIndexExists(String indexId) {
        String metaIndexId = deriveMetadataIndexName(indexId);
        IndicesAdminClient indices = getClient().admin().indices();

        return indices.prepareExists(metaIndexId).get().isExists();
    }

    /**
     * @param indexId
     * @return true if deletion of index is acknowledged
     */
    private boolean deleteIndexById(String indexId) {
        log.info("Deleting index '{}'", indexId);
        DeleteIndexRequest request = new DeleteIndexRequest(indexId);
        try {
            AcknowledgedResponse delete = getClient().admin().indices().delete(request).actionGet();
            log.info("Delete acknowledged: {}", delete.isAcknowledged());
            return delete.isAcknowledged();
        } catch (Exception e) {
            log.info("Index does not exist, no need to delete: {}", e.getMessage());
            return true;
        }
    }

}
