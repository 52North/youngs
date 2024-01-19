/*
 * Copyright 2015-2023 52Â°North Spatial Information Research GmbH
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
import co.elastic.clients.elasticsearch._types.AcknowledgedResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.PutMappingResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel NÃ¼st</a>
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

    public abstract ElasticsearchClient getClient();

    protected final String getCluster() {
        return cluster;
    }

    @Override
    public void storeWithExceptions(SinkRecord record) throws SinkException {
        log.trace("Storing record: {}", record);
        Objects.nonNull(record);

        if (record instanceof BuilderRecord) {
            BuilderRecord builderRecord = (BuilderRecord) record;
            ElasticsearchClient client = getClient();

            log.trace("Indexing record: {}", record);
            IndexRequest.Builder<JsonData> requestBuilder = new IndexRequest.Builder<>(); 
            requestBuilder.index(this.index);
            requestBuilder.document(builderRecord.getData());
            if (record.hasId()) {
                requestBuilder.id(builderRecord.getId());
            }

            try {
                log.trace("Sending record to sink...");
                IndexResponse response = client.index(requestBuilder.build());
                log.trace("Created [{}] with id {} @ {}, version {}", response.result() == Result.Created ,
                        response.id(), response.index(), response.version());

                if (response.result() == Result.Created  || (response.result() != Result.Created && (response.version() > 1))) {
                    return;
                } else {
                    throw new SinkException("Record '%s' could not be stored due to unforeseen error.", builderRecord.getId());
                }
            } catch (ElasticsearchException | IOException e) {
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
            ElasticsearchIndicesClient indices = getClient().indices();
            String indexId = mapping.getIndex();
            BooleanResponse exists = indices.exists(i -> i.index(this.index));
            if (exists.value()) {
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
        } catch (ElasticsearchException | IOException e) {
            log.warn(e.getMessage(), e);
            throw new SinkError(e, "Problem preparing sink: %s", e.getMessage());
        }
    }

    protected boolean createMapping(MappingConfiguration mapping, String indexId) throws ElasticsearchException, IOException {
        ElasticsearchIndicesClient indices = getClient().indices();

        Map<String, Object> schema = schemaGenerator.generate(mapping);
        log.trace("Built schema creation request:\n{}", Arrays.toString(schema.entrySet().toArray()));
        Map<String, Object> requestBody = new HashMap<>();
        if (mapping.hasIndexCreationRequest()) {
            requestBody.put("settings",mapping.getIndexCreationRequest());
        }
        requestBody.put("mappings", schema);
        String requestJson = JsonData.of(requestBody).toString();
        
        CreateIndexRequest.Builder createBuilder = new CreateIndexRequest.Builder();
        createBuilder.index(indexId);
        createBuilder.withJson(new StringReader(requestJson));
        CreateIndexRequest request = createBuilder.build();

        CreateIndexResponse response = indices.create(request);
        log.debug("Created indices: {}, acknowledged: {}", response, response.acknowledged());

        // elasticsearch 6.x removed support for multiple types in one index, we need a separate one
        // create metadata mapping and schema mapping
        requestBody = new HashMap<>();
        if (mapping.hasIndexCreationRequest()) {
            requestBody.put("settings", mapping.getIndexCreationRequest());
        }
        requestBody.put("mappings", getMetadataSchema());
        CreateIndexRequest.Builder createBuilderMeta = new CreateIndexRequest.Builder();
        createBuilderMeta.index(deriveMetadataIndexName(indexId));
        requestJson = JsonData.of(requestBody).toString();
        createBuilderMeta.withJson(new StringReader(requestJson));
        CreateIndexRequest requestMeta = createBuilderMeta.build();
        
        CreateIndexResponse responseMeta = indices.create(requestMeta);
        log.debug("Created indices: {}, acknowledged: {}", responseMeta, responseMeta.acknowledged());

       
        Map<String, Object> mdRecord = createMetadataRecord(mapping.getVersion(), mapping.getName());
        IndexResponse mdResponse = getClient().index(i -> i
            .index(deriveMetadataIndexName(indexId))
            .id(MetadataDataMapping.METADATA_ROW_ID)
            .document(JsonData.of(mdRecord))
        );
        log.debug("Saved mapping metadata '{}': {}", mdResponse.result() == Result.Created, Arrays.toString(mdRecord.entrySet().toArray()));

        return response.acknowledged();
    }

    protected boolean updateMapping(String indexId, MappingConfiguration mapping) throws SinkError, ElasticsearchException, IOException {
        double version;
        try {
            version = getCurrentVersion(indexId);
        } catch (IOException ex) {
            throw new SinkError("unable to determine mapping version for index " + indexId, ex);
        }
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
        String requestJson = JsonData.of(schema).toString();
        
        PutMappingResponse response;
        try {
             response = getClient().indices().putMapping(m -> m
            .index(indexId)
            .withJson(new StringReader(requestJson))
          );
        } catch (IOException | ElasticsearchException  ex) {
            throw new SinkError("error while executing put mapping request for index " + indexId, ex);
        }
 

        log.info("Update mapping for index {} acknowledged: {}", indexId, response.acknowledged());
        if (!response.acknowledged()) {
            log.error("Problem updating mapping for intex {}", indexId);
        }

       
        Map<String, Object> updatedMetadata = createUpdatedMetadata(deriveMetadataIndexName(indexId));
        JsonData updateMetadataJson = JsonData.of(updatedMetadata);
        //new
        UpdateResponse mdUpdate = getClient().update(i -> i
                .index(deriveMetadataIndexName(indexId))
                .id(MetadataDataMapping.METADATA_ROW_ID)
                .doc(updateMetadataJson)
                , JsonData.class);
        
        log.info("Update metadata record created: {} | id = {} @ {}",
                mdUpdate.result()== Result.Created, mdUpdate.id(), mdUpdate.index());

        return (mdUpdate.id().equals(MetadataDataMapping.METADATA_ROW_ID)
                && response.acknowledged());
    }

    private double getCurrentVersion(String indexId) throws IOException {
        
        GetResponse<ObjectNode> response = getClient().get(g -> g
            .index(indexId)
            .id(MetadataDataMapping.METADATA_ROW_ID),
            ObjectNode.class     //raw json
         );
              
        if (response.found() && response.source() != null) {
            JsonNode versionString = response.source().get(MetadataDataMapping.METADATA_VERSION_FIELD.getName());
            if (versionString == null || !versionString.isTextual() || versionString.asText().isEmpty()) {
                throw new IOException(String.format("Database inconsistency. Version can't be found in row %s/%s/%s",
                        deriveMetadataIndexName(indexId), MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID));
            }else{
                return Double.valueOf(versionString.asText());
            }
        } else {
            return Double.MIN_VALUE;
        }
    }

    private Map<String, Object> createUpdatedMetadata(String indexId) throws SinkError {
          GetResponse<ObjectNode> response;
        try {
            response = getClient().get(g -> g
                    .index(indexId)
                    .id(MetadataDataMapping.METADATA_ROW_ID),
                    ObjectNode.class     //raw json
            );
        } catch (IOException | ElasticsearchException ex) {
            throw new SinkError("unable to retrieve document " + MetadataDataMapping.METADATA_ROW_ID + " from index " + indexId, ex);
        } 
 
        if(response.source() == null ||!response.found()){
            throw new SinkError("source of retrieved document " + MetadataDataMapping.METADATA_ROW_ID + " from index " + indexId + " is empty") ;
        }
        JsonNode retrievedValues = response.source().get(MetadataDataMapping.METADATA_UUIDS_FIELD.getName());
        List<String> values;

        if (retrievedValues.isTextual()) {
            values = new LinkedList<>();
            values.add(retrievedValues.asText());
        } else if (retrievedValues.isArray()) {
            values = new ArrayList<>();
            ArrayNode valueArray = (ArrayNode) retrievedValues;
            for(int i = 0; i < valueArray.size(); i++) {
                JsonNode arrayElement = valueArray.get(i);
                if(arrayElement.isTextual()){
                    values.add(arrayElement.asText());
                }else{
                    log.warn("unexpected value of type " + arrayElement.getNodeType().toString() + " in retrieved values list, " + arrayElement.toString());
                }
            }
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
    public boolean clear(MappingConfiguration mapping, boolean clearMetadata) {
        if (clearMetadata) {
            return deleteIndexById(mapping.getIndex()) && deleteIndexById(mapping.getIndex() + "-meta");
        }
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
    private boolean metaIndexExists(String indexId) throws ElasticsearchException, IOException  {
        String metaIndexId = deriveMetadataIndexName(indexId);
        ElasticsearchIndicesClient indices = getClient().indices();
        BooleanResponse exists = indices.exists(i -> i.index(metaIndexId));
        
        return exists.value();
    }

    /**
     * @param indexId
     * @return true if deletion of index is acknowledged
     */
    private boolean deleteIndexById(String indexId) {
        log.info("Deleting index '{}'", indexId);
        try {
            AcknowledgedResponse delete = getClient().indices().delete(i -> i.index(indexId));
            log.info("Delete acknowledged: {}", delete.acknowledged());
            return delete.acknowledged();
        } catch (ElasticsearchException | IOException e) {
            log.info("Index does not exist, no need to delete: {}", e.getMessage());
            return true;
        }
    }

}
