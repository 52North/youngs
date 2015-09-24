/*
 * Copyright 2015-${currentYearDynamic} 52°North Initiative for Geospatial Open Source
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

import org.n52.youngs.load.SchemaGenerator;
import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.joda.time.DateTimeZone;
import org.n52.iceland.exception.ConfigurationError;
import org.n52.iceland.statistics.api.mappings.MetadataDataMapping;
import org.n52.iceland.statistics.api.parameters.AbstractEsParameter;
import org.n52.iceland.statistics.api.parameters.ObjectEsParameter;
import org.n52.iceland.statistics.api.parameters.SingleEsParameter;
import org.n52.youngs.exception.SinkError;
import org.n52.youngs.transform.MappingConfiguration;
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
                .put("cluster.name", cluster).build();
        this.client = new TransportClient(settings).addTransportAddress(
                new InetSocketTransportAddress(this.host, this.port));
    }

    @Override
    public Client getClient() {
        return this.client;
    }

    @Override
    public boolean prepare(MappingConfiguration mapping) {
        IndicesAdminClient indices = getClient().admin().indices();

        String indexId = mapping.getIndex();
        if (indices.prepareExists(indexId).get().isExists()) {
            log.info("Index {} already exists, updating the mapping ...", indexId);
            return updateMapping(indexId, mapping);
        } else {
            log.info("Index {} does not exist, creating it ...", indexId);
            return createMapping(mapping, indexId);
        }
    }

    protected boolean createMapping(MappingConfiguration mapping, String indexId) {
        IndicesAdminClient indices = getClient().admin().indices();

        SchemaGenerator generator = new SchemaGeneratorImpl();
        XContentBuilder schema = generator.generate(mapping);

        // create metadata mapping and schema mapping
        CreateIndexResponse response = indices.prepareCreate(indexId)
                .addMapping(MetadataDataMapping.METADATA_TYPE_NAME, getMetadataSchema())
                .addMapping(mapping.getType(), schema)
                .get();
        log.debug("Created indices: {}", response);

        Map<String, Object> mdRecord = createMetadataRecord(mapping.getVersion(), mapping.getName());
        IndexResponse mdResponse = client.prepareIndex(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID).setSource(mdRecord).get();

        return (mdResponse.isCreated() && response.isAcknowledged());
    }

    protected boolean updateMapping(String indexId, MappingConfiguration mapping) throws SinkError {
        double version = getCurrentVersion(indexId);
        log.info("Existing mapping version is {}", version);
        if (version < 0) {
            throw new ConfigurationError("Database inconsistency. Metadata version not found in type %s", MetadataDataMapping.METADATA_TYPE_NAME);
        }
        if (version != mapping.getVersion()) {
            throw new SinkError("Database schema version inconsistency. Version numbers don't match. Database version number %d != mapping version number %d",
                    version, mapping.getVersion());
        }

        SchemaGenerator generator = new SchemaGeneratorImpl();
        XContentBuilder schema = generator.generate(mapping);

        UpdateResponse update = client.prepareUpdate(indexId, mapping.getType(), "1").setDoc(schema).get();

        Map<String, Object> updatedMetadata = createUpdatedMetadata(indexId);
        UpdateResponse mdUpdate = client.prepareUpdate(indexId, MetadataDataMapping.METADATA_TYPE_NAME, "1").setDoc(updatedMetadata).get();

        return (mdUpdate.isCreated() && update.isCreated());
    }

    private double getCurrentVersion(String indexId) {
        GetResponse resp = client.prepareGet(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
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
        GetResponse resp = client.prepareGet(indexId, MetadataDataMapping.METADATA_TYPE_NAME, MetadataDataMapping.METADATA_ROW_ID)
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
        HashMap<String, Object> properties = Maps.newHashMapWithExpectedSize(1);
        HashMap<String, Object> mappings = Maps.newHashMap();

        for (Field field : MetadataDataMapping.class.getDeclaredFields()) {
            AbstractEsParameter value = checkField(field);
            if (value != null) {
                resolveParameterField(value, mappings);
            }
        }

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
        data.put(MetadataDataMapping.METADATA_NAME_FIELD.getName(), name);
        data.put(MetadataDataMapping.METADATA_UUIDS_FIELD.getName(), uuid);
        log.info("Initial metadata is created ceated for type {} with uuid {} @ {}",
                MetadataDataMapping.METADATA_TYPE_NAME, uuid, time);

        return data;
    }

}
