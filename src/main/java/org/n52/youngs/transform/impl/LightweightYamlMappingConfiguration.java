/*
 * Copyright 2015-2021 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.transform.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.github.autermann.yaml.Yaml;
import com.github.autermann.yaml.YamlNode;
import com.github.autermann.yaml.nodes.YamlMapNode;
import com.google.common.base.Joiner;
import com.google.common.io.Resources;

public class LightweightYamlMappingConfiguration implements MappingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LightweightYamlMappingConfiguration.class);

    private static final String EXPRESSION_KEY = "expression";

    private static final String PROPERTIES_KEY = "properties";

    private static final String TYPE_KEY = "type";

    private String name;

    private int version;

    private String index;

    private boolean indexCreationEnabled;

    private boolean dynamicMappingEnabled;

    private String type;

    private Optional<String> indexCreationRequest;

    private Map<String, LightweightMappingEntry> entries;

    public LightweightYamlMappingConfiguration(String fileName) throws IOException {
        this(Resources.asByteSource(Resources.getResource(fileName)).openStream());
        log.info("Created configuration from filename {}", fileName);
    }

    public LightweightYamlMappingConfiguration(InputStream input) {
        entries = new HashMap<String, LightweightMappingEntry>();
        Yaml yaml = new Yaml();
        YamlNode configurationNodes = yaml.load(input);
        if (configurationNodes == null) {
            log.error("Could not load configuration from {}, nodes: {}", input, configurationNodes);
        } else {
            log.trace("Read configuration file with the root elements {}", Joiner.on(" ").join(configurationNodes));
            init(configurationNodes);
        }
        log.info("Created configuration from stream {} with {} entries", input, entries.size());
    }

    private void init(YamlNode configurationNodes) {
        // read the entries from the config file
        this.name = configurationNodes.path("name").asTextValue(DEFAULT_NAME);
        this.version = configurationNodes.path("version").asIntValue(DEFAULT_VERSION);
        if (configurationNodes.hasNotNull("index")) {
            YamlNode indexField = configurationNodes.get("index");
            this.index = indexField.path("name").asTextValue(DEFAULT_INDEX);
            this.indexCreationEnabled = indexField.path("create").asBooleanValue(DEFAULT_INDEX_CREATION);
            this.dynamicMappingEnabled = indexField.path("dynamic_mapping").asBooleanValue(DEFAULT_DYNAMIC_MAPPING);
            this.type = indexField.path("type").asTextValue(DEFAULT_TYPE);
            if (indexField.hasNotNull("settings")) {
                this.indexCreationRequest = Optional.of(indexField.get("settings").asTextValue());
            }
        }
        YamlMapNode valueMap = configurationNodes.path("mappings").asMap();
        valueMap.forEach(yamlNode -> mapYamlNode(yamlNode, valueMap.get(yamlNode)));
    }

    private void mapYamlNode(YamlNode entry, YamlNode yamlNode) {
        String expression = yamlNode.get(EXPRESSION_KEY).asTextValue();
        String fieldName = entry.asTextValue();
        LightweightMappingEntry lightweightYamlEntry = new LightweightMappingEntry(fieldName, expression);
        YamlNode propertiesNode = yamlNode.get(PROPERTIES_KEY);
        YamlNode typeNode = propertiesNode.get(TYPE_KEY);
        lightweightYamlEntry.setType(MappingType.fromString(typeNode.asTextValue()));
        entries.put(fieldName, lightweightYamlEntry);
    }

    public Collection<LightweightMappingEntry> getLightweightEntries() {
        return this.entries.values();
    }

    public MappingEntry getLightweightEntry(String name) {
        return entries.get(name);
    }

    @Override
    public Collection<MappingEntry> getEntries() {
        return Collections.emptyList();
    }

    @Override
    public MappingEntry getEntry(String name) {
        return null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getVersion() {
        // TODO Auto-generated method stub
        return this.version;
    }

    @Override
    public String getIndex() {
        // TODO Auto-generated method stub
        return this.index;
    }

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return this.type;
    }

    @Override
    public String getIdentifierField() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasLocationField() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getLocationField() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getXPathVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isApplicable(Document doc) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isIndexCreationEnabled() {
        // TODO Auto-generated method stub
        return this.indexCreationEnabled;
    }

    @Override
    public boolean isDynamicMappingEnabled() {
        // TODO Auto-generated method stub
        return this.dynamicMappingEnabled;
    }

    @Override
    public boolean hasIndexCreationRequest() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getIndexCreationRequest() {
        // TODO Auto-generated method stub
        return null;
    }

}
