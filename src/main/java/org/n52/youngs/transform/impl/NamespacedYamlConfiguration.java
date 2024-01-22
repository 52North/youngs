/*
 * Copyright 2015-2024 52°North Spatial Information Research GmbH
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

import com.github.autermann.yaml.YamlNode;
import com.github.autermann.yaml.nodes.YamlMapNode;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import org.n52.youngs.exception.MappingError;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public abstract class NamespacedYamlConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NamespacedYamlConfiguration.class);

    protected final NamespaceContext parseNamespaceContext(YamlNode configurationNodes) {
        if (configurationNodes.hasNotNull("namespaces")) {
            Map<String, String> nsMap = Maps.newHashMap();
            final YamlMapNode valueMap = configurationNodes.path("namespaces").asMap();
            valueMap.entries().stream().forEach((Map.Entry<YamlNode, YamlNode> e) -> {
                nsMap.put(e.getKey().asTextValue(), e.getValue().asTextValue());
            });
            NamespaceContext nsc = new NamespaceContextImpl(nsMap);
            log.trace("Created namespace context from mapping configuration: {}", nsc);
            return nsc;
        } else {
            log.error("Requited namespace map missing in mapping file '{}'", configurationNodes.get("name"));
            throw new MappingError("Mapping '%s' does not contain 'namespaces' map.", configurationNodes.get("name"));
        }
    }

}
