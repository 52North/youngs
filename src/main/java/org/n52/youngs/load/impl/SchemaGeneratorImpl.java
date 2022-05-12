/*
 * Copyright 2015-2022 52°North Initiative for Geospatial Open Source
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

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SchemaGeneratorImpl implements SchemaGenerator {

    private static final Logger log = LoggerFactory.getLogger(SchemaGeneratorImpl.class);

    public SchemaGeneratorImpl() {
        //
    }

    @Override
    public Map<String, Object> generate(MappingConfiguration mapping) {
        Map<String, Object> schema = Maps.newHashMap();
        schema.put("dynamic", mapping.isDynamicMappingEnabled());

        Map<String, Object> fields = Maps.newHashMap();
        mapping.getEntries().forEach((MappingEntry entry) -> {
            fields.put(entry.getFieldName(), createElasticEntry(entry));
        });

        if (mapping.hasSuggest()) {
            Map<String, Object> suggest = new HashMap<>(mapping.getSuggest());
            if (suggest.containsKey("mappingConfiguration")) {
                suggest.remove("mappingConfiguration");
            }
            fields.put("suggest", suggest);
        }

        schema.put("properties", fields);

        log.info("Created {} schema with {} fields", mapping.isDynamicMappingEnabled() ? "dynamic" : "", fields.size());
        log.debug("Created schema with {} first level elements: {}", schema.size(), Arrays.deepToString(schema.entrySet().toArray()));
        return schema;
    }

    private Map<String, Object> createElasticEntry(MappingEntry entry) {
        Map<String, Object> properties = Maps.newHashMap();

        entry.getIndexProperties().entrySet().stream().forEach((entryProps) -> {
            properties.put(entryProps.getKey(), entryProps.getValue());
        });
        if (isNested(entry)) {
            Map<String, Object> childrenMap = new HashMap<>();
            entry.getChildren().stream()
                    .forEach(c -> childrenMap.put(c.getFieldName(), createElasticEntry(c)));
            properties.put("properties", childrenMap);
        }

        return properties;
    }

    private boolean isNested(MappingEntry entry) {
        return entry.getIndexProperties().entrySet().stream()
                .filter(e -> e.getKey().equals("type") && e.getValue().equals("nested"))
                .count() > 0;
    }

}
