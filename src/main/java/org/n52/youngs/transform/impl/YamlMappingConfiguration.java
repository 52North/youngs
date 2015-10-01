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
package org.n52.youngs.transform.impl;

import com.github.autermann.yaml.Yaml;
import com.github.autermann.yaml.YamlNode;
import com.github.autermann.yaml.nodes.YamlBooleanNode;
import com.github.autermann.yaml.nodes.YamlDecimalNode;
import com.github.autermann.yaml.nodes.YamlIntegralNode;
import com.github.autermann.yaml.nodes.YamlMapNode;
import com.github.autermann.yaml.nodes.YamlSeqNode;
import com.github.autermann.yaml.nodes.YamlTextNode;
import com.google.common.base.Joiner;
import org.n52.youngs.transform.MappingEntry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.n52.youngs.exception.MappingError;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.transform.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class YamlMappingConfiguration implements MappingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(YamlMappingConfiguration.class);

    List<MappingEntry> entries = Lists.newArrayList();

    private String xpathVersion = DEFAULT_XPATH_VERSION;

    private int version;

    private String name = DEFAULT_NAME;

    private XPathFactory xpathFactory;

    private Optional<XPathExpression> applicabilityExpression = Optional.empty();

    private String type = DEFAULT_TYPE;

    private String index = DEFAULT_INDEX;

    private boolean indexCreationEnabled = DEFAULT_INDEX_CREATION;

    private boolean dynamicMappingEnabled;

    private Optional<String> indexCreationRequest = Optional.empty();

    public YamlMappingConfiguration(File file, NamespaceContext nsContext, XPathFactory factory) throws FileNotFoundException {
        this(new FileInputStream(file), nsContext, factory);
        log.info("Created configuration from file {}", file);
    }

    public YamlMappingConfiguration(String fileName, NamespaceContext nsContext, XPathFactory factory) throws IOException {
        this(Resources.asByteSource(Resources.getResource(fileName)).openStream(),
                nsContext, factory);
        log.info("Created configuration from filename {}", fileName);
    }

    public YamlMappingConfiguration(InputStream input, NamespaceContext nsContext, XPathFactory factory) {
        this.xpathFactory = factory;

        Yaml yaml = new Yaml();
        YamlNode configurationNodes = yaml.load(input);
        log.trace("Read configuration file with the root elements {}", Joiner.on(" ").join(configurationNodes));

        parse(configurationNodes, nsContext);

        log.info("Created configuration from stream {} with {} entries", input, entries.size());
    }

    public YamlMappingConfiguration(String fileName, XPathFactory factory) throws IOException {
        this(Resources.asByteSource(Resources.getResource(fileName)).openStream(), factory);
        log.info("Created configuration from filename {}", fileName);
    }

    public YamlMappingConfiguration(InputStream input, XPathFactory factory) {
        this.xpathFactory = factory;

        Yaml yaml = new Yaml();
        YamlNode configurationNodes = yaml.load(input);
        log.trace("Read configuration file with the root elements {}", Joiner.on(" ").join(configurationNodes));

        NamespaceContext nsContext = parseNamespaceContext(configurationNodes);
        parse(configurationNodes, nsContext);

        log.info("Created configuration from stream {} with {} entries", input, entries.size());
    }

    private NamespaceContext parseNamespaceContext(YamlNode configurationNodes) {
        if (configurationNodes.hasNotNull("namespaces")) {
            Map<String, String> nsMap = Maps.newHashMap();

            final YamlMapNode valueMap = configurationNodes.path("namespaces").asMap();
            valueMap.entries().stream().forEach(
                    (Entry<YamlNode, YamlNode> e) -> {
                        nsMap.put(e.getKey().asTextValue(), e.getValue().asTextValue());
                    });

            NamespaceContext nsc = new NamespaceContextImpl(nsMap);
            log.trace("Created namespace context from mapping configuration: {}", nsc);
            return nsc;
        } else {
            log.error("No namespace in mapping file, must be either there or provided in constructor. Mapping {}",
                    configurationNodes.get("name"));
            throw new MappingError("Mapping must containg 'namespaces' map or namespaces must be provided in constructor.");
        }
    }

    private void parse(YamlNode configurationNodes, NamespaceContext nsContext) {
        // read the entries from the config file
        this.name = configurationNodes.path("name").asTextValue(DEFAULT_NAME);
        this.version = configurationNodes.path("version").asIntValue(DEFAULT_VERSION);
        this.xpathVersion = configurationNodes.path("xpathversion").asTextValue(DEFAULT_XPATH_VERSION);
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

        XPathHelper xph = new XPathHelper();
        if (!xph.isVersionSupported(xpathFactory, xpathVersion)) {
            throw new MappingError("Provided factory {} does not support version {}", xpathFactory, xpathVersion);
        }
        log.debug("Using XPathFactory {}", xpathFactory);

        String applicabilityXPathString = configurationNodes
                .path("applicability_xpath").asTextValue(DEFAULT_APPLICABILITY_PATH);
        XPath path = newXPath(nsContext);
        try {
            applicabilityExpression = Optional.of(path.compile(applicabilityXPathString));
        } catch (XPathExpressionException e) {
            log.error("Could not compile applicability xpath, will always evalute to true", e);
        }

        if (configurationNodes.hasNotNull("mappings")) {
            YamlMapNode mappingsNode = configurationNodes.path("mappings").asMap();
            this.entries = Lists.newArrayList();
            for (Entry<YamlNode, YamlNode> entry : mappingsNode.entries()) { // use old-style lood to forward exception
                MappingEntry e = createEntry(entry.getKey().asTextValue(),
                        entry.getValue(), nsContext);
                log.trace("Created entry: {}", e);
                this.entries.add(e);
            }

            // ensure not exactly one field is identifier
            long idCount = this.entries.stream().filter(MappingEntry::isIdentifier).count();
            if (idCount > 1) {
                List<String> entriesWithId = this.entries.stream().filter(MappingEntry::isIdentifier)
                        .map(MappingEntry::getFieldName).collect(Collectors.toList());
                log.error("Found more than one entries marked as 'identifier': {}", Arrays.toString(entriesWithId.toArray()));
                throw new MappingError("More than one fields are marked as 'identifier'. Found {} in {}", idCount,
                        Arrays.toString(entriesWithId.toArray()));
            }

            // sort list by field name
            Collections.sort(entries, (me1, me2) -> {
                return me1.getFieldName().compareTo(me2.getFieldName());
            });
        }
    }

    private MappingEntry createEntry(String id, YamlNode node, NamespaceContext nsContext) throws MappingError {
        log.trace("Parsing mapping '{}'", id);
        if (node instanceof YamlMapNode) {
            YamlMapNode mapNode = (YamlMapNode) node;

            Map<String, Object> indexProperties = createIndexProperties(id, node);

            boolean isIdentifier = mapNode.path("identifier").asBooleanValue(false);

            String expression = mapNode.path("xpath").asTextValue();
            XPath xPath = newXPath(nsContext);
            try {
                XPathExpression compiledExpression = xPath.compile(expression);
                MappingEntryImpl entry = new MappingEntryImpl(compiledExpression,
                        mapNode.path("isoqueryable").asBooleanValue(false),
                        mapNode.path("isoqueryableName").asTextValue(null),
                        indexProperties,
                        isIdentifier);

                // geo types
                if (mapNode.has("coordinates")) {
                    String coordsType = mapNode.path("coordinates_type").asTextValue();
                    boolean points = mapNode.path("coordinates").has("points");
                    if (coordsType == null || !points) {
                        log.error("Missing properties for field {} for coordinates type: coordinates_type = {}, coordinates.points contained = {}",
                                entry.getFieldName(), coordsType, points);
                        throw new MappingError("Missing properties in field %s for coordinates type: coordinates_type = %s, coordinatesEyxpression = %s, coordinates contained = {}",
                                entry.getFieldName(), coordsType, points);
                    }

                    YamlSeqNode pointsMap = (YamlSeqNode) mapNode.path("coordinates").path("points");

//                    log.trace("Adding type '{}' coordinates xpath: {}", coordsType, coordsExpression);
                    List<XPathExpression[]> pointExpressions = pointsMap.value().stream().filter(n -> n instanceof YamlMapNode)
                            .map(n -> (YamlMapNode) n)
                            .map(mn -> {
                                String expressionStringLat = mn.path("lat").asTextValue();
                                String expressionStringLon = mn.path("lon").asTextValue();
                                try {
                                    XPathExpression compiledLat = newXPath(nsContext).compile(expressionStringLat);
                                    XPathExpression compiledLon = newXPath(nsContext).compile(expressionStringLon);
                                    return new XPathExpression[]{compiledLat, compiledLon};
                                } catch (XPathExpressionException e) {
                                    log.warn("Error creating xpath '{}' or '{}' for point in field {}: {}",
                                            expressionStringLat, expressionStringLon, id, e.getMessage());
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    log.trace("Created {} points for {}", pointExpressions.size(), id);
                    entry.setCoordinatesXPaths(pointExpressions).setCoordinatesType(coordsType);
                }

                return entry;
            } catch (XPathExpressionException e) {
                log.error("Could not create XPath for provided expression '{}' in field {}", expression, e, id);
                throw new MappingError(e, "Could not create XPath for provided expression '%s' in field %s",
                        expression, id);
            }
        }
        throw new MappingError("The provided node class %s is not supported in the mapping '{}': %s",
                node.getClass().toString(), id, node.toString());
    }

    private XPath newXPath(NamespaceContext nsContext) {
        XPath xPath = xpathFactory.newXPath();
        xPath.setNamespaceContext(nsContext);
        return xPath;
    }

    private Map<String, Object> createIndexProperties(String id, YamlNode node) {
        Map<String, Object> props = Maps.newHashMap();

        if (node.hasNotNull("properties")) {
            final YamlMapNode valueMap = node.path("properties").asMap();
            Map<YamlNode, YamlNode> indexProperties = valueMap.entries().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            indexProperties.forEach((YamlNode k, YamlNode v) -> {
                log.trace("Adding property {} = {}, type: {}", k, v, v.getClass());
                String key = k.asTextValue();
                Optional<Object> value = Optional.empty();
                if (v instanceof YamlBooleanNode) {
                    value = Optional.of(v.asBooleanValue());
                } else if (v instanceof YamlTextNode) {
                    value = Optional.of(v.asTextValue());
                } else if (v instanceof YamlDecimalNode) {
                    value = Optional.of(v.asDoubleValue());
                } else if (v instanceof YamlIntegralNode) {
                    value = Optional.of(v.asLongValue());
                }

                if (value.isPresent()) {
                    props.put(key, value.get());
                } else {
                    log.error("Could not parse property {}={} because of unhandled type {}", k, v, v.getClass());
                }
            });
        }

        // make sure index name is a string
        if (props.containsKey(MappingEntry.INDEX_NAME)) {
            Object nameObj = props.get(MappingEntry.INDEX_NAME);
            if (!(nameObj instanceof String)) {
                log.debug("Index name '{}' of field {} is not a string, falling back to id!", name, id);
                props.put(MappingEntry.INDEX_NAME, id);
            }
        }

        // set default type
        if (!props.containsKey(MappingEntry.IndexProperties.TYPE)) {
            props.put(MappingEntry.IndexProperties.TYPE, DEFAULT_INDEXPROPERTY_TYPE);
        }

        // handle defaulting to parent node name for id
        if (!props.containsKey(MappingEntry.INDEX_NAME)) {
            props.put(MappingEntry.INDEX_NAME, id);
        }

        return props;
    }

    @Override
    public Collection<MappingEntry> getEntries() {
        return entries;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getVersion() {
        return this.version;
    }

    @Override
    public String getXPathVersion() {
        return this.xpathVersion;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getIndex() {
        return index;
    }

    @Override
    public boolean isApplicable(Document doc) {
        if (!this.applicabilityExpression.isPresent()) {
            log.debug("No applicability xpath provided, returning TRUE.");
            return true;
        }

        boolean result;
        try {
            XPathExpression expr = this.applicabilityExpression.get();
            result = (boolean) expr.evaluate(doc, XPathConstants.BOOLEAN);
        } catch (XPathExpressionException | RuntimeException e) {
            log.warn("Error executing applicability xpath on document, returning false: {}", doc, e);
            return false;
        }

        return result;
    }

    @Override
    public boolean isIndexCreationEnabled() {
        return indexCreationEnabled;
    }

    @Override
    public boolean isDynamicMappingEnabled() {
        return dynamicMappingEnabled;
    }

    @Override
    public boolean hasIndexCreationRequest() {
        return indexCreationRequest.isPresent();
    }

    @Override
    public String getIndexCreationRequest() {
        return indexCreationRequest.get();
    }

}
