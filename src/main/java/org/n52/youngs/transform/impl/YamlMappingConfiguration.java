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

import com.github.autermann.yaml.Yaml;
import com.github.autermann.yaml.YamlNode;
import com.github.autermann.yaml.nodes.YamlBooleanNode;
import com.github.autermann.yaml.nodes.YamlDecimalNode;
import com.github.autermann.yaml.nodes.YamlIntegralNode;
import com.github.autermann.yaml.nodes.YamlMapNode;
import com.github.autermann.yaml.nodes.YamlSeqNode;
import com.github.autermann.yaml.nodes.YamlTextNode;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import org.n52.youngs.transform.MappingEntry;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.n52.youngs.exception.MappingError;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.transform.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class YamlMappingConfiguration extends NamespacedYamlConfiguration implements MappingConfiguration {

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

    private Optional<Map<String, Object>> indexCreationRequest = Optional.empty();

    private final XPathHelper xpathHelper;

    private String identifierField;

    private Optional<String> locationField = Optional.empty();
    private Map<String, Object> suggest;

    public YamlMappingConfiguration(String fileName, XPathHelper xpathHelper) throws IOException {
        this(Resources.asByteSource(Resources.getResource(fileName)).openStream(), xpathHelper);
        log.info("Created configuration from filename {}", fileName);
    }

    public YamlMappingConfiguration(InputStream input, XPathHelper xpathHelper) {
        this.xpathHelper = xpathHelper;
        this.xpathFactory = xpathHelper.newXPathFactory();

        Yaml yaml = new Yaml();
        YamlNode configurationNodes = yaml.load(input);
        if (configurationNodes == null) {
            log.error("Could not load configuration from {}, nodes: {}", input, configurationNodes);
        } else {
            log.trace("Read configuration file with the root elements {}", Joiner.on(" ").join(configurationNodes));

            NamespaceContext nsContext = parseNamespaceContext(configurationNodes);
            init(configurationNodes, nsContext);
        }

        log.info("Created configuration from stream {} with {} entries", input, entries.size());
    }

    private void init(YamlNode configurationNodes, NamespaceContext nsContext) {
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
                Map<String, Object> yamlAsMap = new org.yaml.snakeyaml.Yaml().load(indexField.get("settings").asTextValue());
                this.indexCreationRequest = Optional.of(yamlAsMap);
            }
        }

        if (!this.xpathHelper.isVersionSupported(xpathFactory, xpathVersion)) {
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
            for (Entry<YamlNode, YamlNode> entry : mappingsNode.entries()) { // use old-style loop to forward exception
                MappingEntry e = createEntry(entry.getKey().asTextValue(),
                        entry.getValue(), nsContext);
                log.trace("Created entry: {}", e);
                this.entries.add(e);
            }

            // ensure exactly one field is identifier
            long idCount = this.entries.stream().filter(MappingEntry::isIdentifier).count();
            if (idCount > 1) {
                List<String> entriesWithId = this.entries.stream().filter(MappingEntry::isIdentifier)
                        .map(MappingEntry::getFieldName).collect(Collectors.toList());
                log.error("Found more than one entries marked as 'identifier': {}", Arrays.toString(entriesWithId.toArray()));
                throw new MappingError("More than one field are marked as 'identifier'. Found {}: {}", idCount,
                        Arrays.toString(entriesWithId.toArray()));
            }
            Optional<MappingEntry> identifier = this.entries.stream().filter(MappingEntry::isIdentifier).findFirst();
            if (identifier.isPresent()) {
                this.identifierField = identifier.get().getFieldName();
                log.trace("Found identifier field '{}'", this.identifierField);
            } else {
                throw new MappingError("No field is marked as 'identifier', exactly one must be.");
            }

            // ensure not more than one field is location
            long locationCount = this.entries.stream().filter(MappingEntry::isLocation).count();
            if (locationCount > 1) {
                List<String> entriesWithLocation = this.entries.stream().filter(MappingEntry::isIdentifier)
                        .map(MappingEntry::getFieldName).collect(Collectors.toList());
                log.error("Found more than one entries marked as 'location': {}", Arrays.toString(entriesWithLocation.toArray()));
                throw new MappingError("More than one field are marked as 'location'. Found {}: {}", idCount,
                        Arrays.toString(entriesWithLocation.toArray()));
            }
            Optional<MappingEntry> location = this.entries.stream().filter(MappingEntry::isLocation).findFirst();
            if (location.isPresent()) {
                this.locationField = Optional.of(location.get().getFieldName());
                log.trace("Found location field '{}'", this.locationField.get());
            } else {
                log.warn("No field is marked as 'location'.");
            }

            // sort list by field name
            Collections.sort(entries, (me1, me2) -> {
                return me1.getFieldName().compareTo(me2.getFieldName());
            });
        }

        if (configurationNodes.hasNotNull("suggest")) {
            YamlMapNode suggestNode = configurationNodes.path("suggest").asMap();
            this.suggest = Maps.newHashMap();
            for (Entry<YamlNode, YamlNode> entry : suggestNode.entries()) {
                YamlNode tmp = entry.getValue();
                Object val = extractObjectValue(tmp);
                if (val != null) {
                    this.suggest.put(entry.getKey().asTextValue(), val);
                }
            }
        }
    }

    private Map<String, Object> createSuggestConfiguration(YamlNode node) {
        if (node.hasNotNull("suggest")) {
            final YamlMapNode suggestMap = node.path("suggest").asMap();
            Map<YamlNode, YamlNode> suggestProps = suggestMap.entries().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            Map<String, Object> result = new HashMap<>(suggestProps.size());
            suggestProps.forEach((YamlNode k, YamlNode v) -> {
                Object val = extractObjectValue(v);

                if (val != null) {
                    result.put(k.asTextValue(), val);
                }
            });

            return result;
        }

        return Collections.emptyMap();
    }

    private Object extractObjectValue(YamlNode yn) {
        Object val;
        if (yn.isText()) {
            val = yn.asTextValue();
        }
        else if (yn.isInt()) {
            val = yn.intValue();
        }
        else if (yn.isBoolean()) {
            val = yn.booleanValue();
        }
        else if (yn.isSequence()) {
            val = yn.asSequence().stream()
                    .map(y -> extractObjectValue(y))
                    .collect(Collectors.toList());
        }
        else if (yn.isMap()) {
            val = yn.asMap().entries().stream()
                    .collect(Collectors.toMap(e -> {
                        return extractObjectValue(e.getKey());
                    }, e -> {
                        return extractObjectValue(e.getValue());
                    }));
        }
        else {
            val = null;
        }
        return val;
    }


    private MappingEntry createEntry(String id, YamlNode node, NamespaceContext nsContext) throws MappingError {
        log.trace("Parsing mapping '{}'", id);
        if (node instanceof YamlMapNode) {
            YamlMapNode mapNode = (YamlMapNode) node;

            Map<String, Object> indexProperties = createIndexProperties(id, node);

            boolean isIdentifier = mapNode.path("identifier").asBooleanValue(false);
            boolean isLocation = mapNode.path("location").asBooleanValue(false);
            boolean isXml = mapNode.path("raw_xml").asBooleanValue(false);

            String expression = mapNode.path("xpath").asTextValue();

            XPath xPath = newXPath(nsContext);
            try {
                XPathExpression compiledExpression = xPath.compile(expression);

                XPathExpression condition = null;
                if (mapNode.has("condition")) {
                    String conditionString = mapNode.path("condition").asTextValue();
                    condition = newXPath(nsContext).compile(conditionString);
                }

                List<MappingEntry> children = createChildren(node, expression, nsContext);

                MappingEntryImpl entry = new MappingEntryImpl(id,
                        compiledExpression,
                        indexProperties,
                        isIdentifier,
                        isLocation,
                        isXml,
                        condition,
                        children);
                log.trace("Starting new entry: {}", entry);

                // geo types
                if (mapNode.hasNotNull("coordinates")) {
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

                if (mapNode.hasNotNull("replacements")) {
                    YamlSeqNode rMap = (YamlSeqNode) mapNode.path("replacements");
                    Map<String, String> replacements = Maps.newHashMap();
                    rMap.value().stream().filter(n -> n instanceof YamlMapNode)
                            .map(n -> (YamlMapNode) n)
                            .map(mn -> {
                                String replace = mn.path("replace").asTextValue();
                                String with = mn.path("with").asTextValue();
                                return new String[]{replace, with};
                            })
                            .forEach(e -> replacements.put(e[0], e[1]));
                    log.trace("Parsed replacements: {}", Arrays.toString(replacements.entrySet().toArray()));
                    entry.setReplacements(replacements);
                }

                if (mapNode.hasNotNull("split")) {
                    YamlNode sNode = mapNode.path("split");
                    String split = sNode.asTextValue();
                    log.trace("Parsed split: {}", split);
                    entry.setSplit(split);
                }

                // for raw types
                if (mapNode.hasNotNull("output_properties")) {
                    YamlSeqNode rMap = (YamlSeqNode) mapNode.path("output_properties");
                    Map<String, String> op = Maps.newHashMap();
                    rMap.value().stream().filter(n -> n instanceof YamlMapNode)
                            .map(n -> (YamlMapNode) n)
                            .map(mn -> {
                                String replace = mn.path("name").asTextValue();
                                String with = mn.path("value").asTextValue();
                                return new String[]{replace, with};
                            })
                            .forEach(e -> op.put(e[0], e[1]));
                    log.trace("Parsed outputProperties: {}", Arrays.toString(op.entrySet().toArray()));
                    entry.setOutputProperties(op);
                }

                return entry;
            } catch (XPathExpressionException e) {
                log.error("Could not create XPath for provided expression '{}' in field {}", expression, id, e);
                throw new MappingError(e, "Could not create XPath for provided expression '%s' in field %s",
                        expression, id);
            }
        }
        throw new MappingError("The provided node class %s is not supported in the mapping '%s': %s",
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
                else {
                    // multi fields (e.g. for sorting _and_ analyzing)
                    if (k.isText() && k.asTextValue().equals("fields")) {
                        Map<String, Object> subFields = new HashMap<>();
                        YamlNode fieldsNode = v;
                        if (v.isMap()) {
                            YamlMapNode fieldsNodeMap = v.asMap();

                            // walk through each entry in the fields
                            fieldsNodeMap.entries().stream()
                                .forEach((Entry<YamlNode, YamlNode> fieldNode) -> {
                                    String subFieldName = fieldNode.getKey().asTextValue();
                                    if (fieldNode.getValue().isMap()) {
                                        YamlMapNode subFieldValues = fieldNode.getValue().asMap();
                                        // add the string values of the subField
                                        Map<String, String> subFieldProperties = new HashMap<>();
                                        subFieldValues.entries().stream()
                                                .filter((Entry<YamlNode, YamlNode> fieldNodeProperties) -> {
                                                    String propertyKey = fieldNodeProperties.getKey().asTextValue();
                                                    if ("type".equals(propertyKey) || "index".equals(propertyKey) || "normalizer".equals(propertyKey)) {
                                                        YamlNode propertyValue = fieldNodeProperties.getValue();
                                                        if (propertyValue.isText()) {
                                                            return true;
                                                        }
                                                    }

                                                    return false;
                                                })
                                                .forEach((Entry<YamlNode, YamlNode> fieldNodeProperties) -> {
                                                    String propertyKey = fieldNodeProperties.getKey().asTextValue();
                                                    String propertyValue = fieldNodeProperties.getValue().asTextValue();
                                                    subFieldProperties.put(propertyKey, propertyValue);
                                                });
                                        subFields.put(subFieldName, subFieldProperties);
                                        log.info("{}: {}", subFieldName, subFieldProperties);
                                    }
                                });
                            value = Optional.of(subFields);
                        }
                    }
                }

                if (value.isPresent()) {
                    props.put(key, value.get());
                } else {
                    log.error("Could not parse property {}={} because of unhandled type {}", k, v, v.getClass());
                }
            });
        }

        // set default type
        if (!props.containsKey(MappingEntry.IndexProperties.TYPE)) {
            props.put(MappingEntry.IndexProperties.TYPE, DEFAULT_INDEXPROPERTY_TYPE);
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
    public Map<String, Object> getIndexCreationRequest() {
        return indexCreationRequest.get();
    }

    @Override
    public MappingEntry getEntry(String name) {
        return this.entries.stream().filter(e -> e.getFieldName().equals(name)).findFirst().get();
    }

    @Override
    public String getIdentifierField() {
        return this.identifierField;
    }

    @Override
    public boolean hasLocationField() {
        return this.locationField.isPresent();
    }

    @Override
    public String getLocationField() {
        return this.locationField.get();
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper s = MoreObjects.toStringHelper(this)
                .add("version", this.version)
                .add("index", this.index)
                .add("name", this.name)
                .add("type", this.type)
                .add("XPath version", this.xpathVersion);
        if (this.applicabilityExpression.isPresent()) {
            s.add("applicability", this.applicabilityExpression.get());
        }

        return s.omitNullValues().toString();
    }

    private List<MappingEntry> createChildren(YamlNode node, String xpath, NamespaceContext nsContext) {
        if (node.hasNotNull("children")) {
            final YamlMapNode childrenMap = node.path("children").asMap();
            Map<YamlNode, YamlNode> children = childrenMap.entries().stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            List<MappingEntry> result = new ArrayList<>(children.size());
            children.forEach((YamlNode k, YamlNode v) -> {
                log.trace("Parsing children {} = {}, type: {}", k, v, v.getClass());
                result.add(createEntry(k.asTextValue(), v, nsContext));
            });

            return result;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasSuggest() {
        return this.suggest != null && !this.suggest.isEmpty();
    }

    @Override
    public Map<String, Object> getSuggest() {
        return this.suggest;
    }

}
