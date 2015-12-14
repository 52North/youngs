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

import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswToBuilderMapper implements Mapper {

    private static final Logger log = LoggerFactory.getLogger(CswToBuilderMapper.class);

    private final MappingConfiguration mapper;

    private final TransformerFactory tFactory = TransformerFactory.newInstance();

    private static final Map<String, String> DEFAULT_OUTPUT_PROPERTIES = ImmutableMap.of(
            OutputKeys.OMIT_XML_DECLARATION, "no",
            OutputKeys.INDENT, "no",
            OutputKeys.ENCODING, Charsets.UTF_8.name());

    private Optional<Transformer> stripspaceTransformer = Optional.empty();

    public CswToBuilderMapper(MappingConfiguration mapper) {
        this.mapper = mapper;

        try (InputStream is = Resources.getResource("xslt/stripspace.xslt").openStream();) {
            Source xslt = new StreamSource(is);
            stripspaceTransformer = Optional.of(tFactory.newTransformer(xslt));
            log.trace("Will apply stripspace XSLT.");
        } catch (TransformerConfigurationException | IOException e) {
            log.error("Problem loading strip-space XSLT file.", e);
        }
    }

    @Override
    public MappingConfiguration getMapper() {
        return mapper;
    }

    /**
     * @param sourceRecord the record to map
     * @return a record containing a builder of the provided SourceRecord, or null if the mapper could not be completed.
     */
    @Override
    public BuilderRecord map(SourceRecord sourceRecord) {
        Objects.nonNull(sourceRecord);
        BuilderRecord record = null;

        if (sourceRecord instanceof NodeSourceRecord) {
            try {
                NodeSourceRecord object = (NodeSourceRecord) sourceRecord;
                IdAndBuilder mappedRecord = mapNodeToBuilder(object.getRecord());

                record = new BuilderRecord(mappedRecord.id, mappedRecord.builder);
                return record;
            } catch (IOException e) {
                log.warn("Error mapping the source {}", sourceRecord, e);
                return null;
            }
        } else {
            log.warn("The SourceRecord class {} is not supported", sourceRecord.getClass().getName());
        }

        return record;
    }

    private IdAndBuilder mapNodeToBuilder(final Node node) throws IOException {
        XContentBuilder builder = XContentFactory.jsonBuilder()
                .humanReadable(true)
                .prettyPrint()
                .startObject();

        // evaluate xpaths and save the results in the builder
        Collection<MappingEntry> entries = mapper.getEntries();
        log.trace("Mapping node {} using {} entries", node, entries.size());

        String id = null;
        try {
            Optional<MappingEntry> idEntry = entries.stream().filter(MappingEntry::isIdentifier).findFirst();
            if (idEntry.isPresent()) {
                id = idEntry.get().getXPath().evaluate(node);
                log.trace("Found id for node: {}", id);
            }
        } catch (XPathExpressionException e) {
            log.warn("Error selecting id field from node", e);
        }

        // handle non-geo entries
        entries.stream().filter(e -> !e.hasCoordinates() && !e.isRawXml()).forEach(entry -> {
            mapEntry(entry, node, builder);
        });

        // handle geo types
        entries.stream().filter(e -> e.hasCoordinates() && !e.isRawXml()).forEach(entry -> {
            mapSpatialEntry(entry, node, builder);
        });

        // handle raw types
        entries.stream().filter(e -> e.isRawXml()).forEach(entry -> {
            mapRawEntry(entry, node, builder);
        });

        builder.endObject();
        builder.close();

        log.trace("Created content for id '{}':\n{}", id, builder.string());

        return new IdAndBuilder(id, builder);
    }

    private void mapSpatialEntry(MappingEntry entry, final Node node, XContentBuilder builder) {
        log.trace("Applying field mapping '{}' to node: {}", entry.getFieldName(), node);
        try {
            Object coordsNode = entry.getXPath().evaluate(node, XPathConstants.NODE);
            if (coordsNode != null) {
                String geoType = (String) entry.getIndexPropery(MappingEntry.IndexProperties.TYPE);
                String field = (String) entry.getIndexPropery(MappingEntry.INDEX_NAME_MAPPING_ATTRIBUTE);

                List<XPathExpression[]> pointsXPaths = entry.getCoordinatesXPaths();

                if (!pointsXPaths.isEmpty() && !geoType.isEmpty() && !field.isEmpty() && entry.hasCoordinatesType()) {
                    List<Number[]> pointsDoubles = pointsXPaths.stream().map(p -> {
                        try {
                            Number lat = (Number) p[0].evaluate(coordsNode, XPathConstants.NUMBER);
                            Number lon = (Number) p[1].evaluate(coordsNode, XPathConstants.NUMBER);
                            return new Number[]{lon, lat}; // in arrays: GeoJSON conform as [lon, lat], see https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-geo-point-type.html
                        } catch (XPathExpressionException e) {
                            log.warn("Error evaluating XPath {} for coordinate: {}", p, e);
                            return null;
                        }
                    })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    log.trace("Evaluated {} expressions and got {} points: {}", pointsXPaths.size(),
                            pointsDoubles.size(), Arrays.deepToString(pointsDoubles.toArray()));

                    builder.startObject(field)
                            .field(MappingEntry.IndexProperties.TYPE, entry.getCoordinatesType())
                            .field("coordinates", pointsDoubles)
                            .endObject();
                    log.debug("Added points '{}' as {} of type {}", Arrays.deepToString(pointsDoubles.toArray()),
                            geoType, entry.getCoordinatesType());
                } else {
                    log.warn("Mapping '{}' has coordinates but is missing one of the other required settings, not adding field: "
                            + "node = {}, index_name = {}, coordinates_type = {}, type = {}, points = {}",
                            entry.getFieldName(), coordsNode, field, entry.getCoordinatesType(), geoType,
                            Arrays.deepToString(pointsXPaths.toArray()));
                }
            } else {
                log.warn("Coords node is null, no result evaluating {} on {]", entry.getXPath(), node);
            }
        } catch (XPathExpressionException | IOException e) {
            log.warn("Error selecting coordinate-field {} as node. Error was: {}", entry.getFieldName(), e.getMessage());
            log.trace("Error selecting field {} as nodeset", entry.getFieldName(), e);
        }
    }

    private void mapEntry(MappingEntry entry, final Node node, XContentBuilder builder) {
        log.trace("Applying field mapping '{}' to node: {}", entry.getFieldName(), node);

        Optional<EvalResult> result = Optional.empty();
        // try nodeset first
        try {
            Object nodesetResult = entry.getXPath().evaluate(node, XPathConstants.NODESET);
            result = Optional.ofNullable(handleEvaluationResult(nodesetResult, entry.getFieldName()));
            if (result.isPresent()) {
                log.trace("Found nodeset result: {}", result.get());
            }
        } catch (XPathExpressionException e) {
            log.debug("Error selecting field {} as nodeset, could be XPath 2.0 expression... trying evaluation to string."
                    + " Error was: {}", entry.getFieldName(), e.getMessage());
            log.trace("Error selecting field {} as nodeset", entry.getFieldName(), e);
        }

        // try string eval if nodeset did not work
        if (!result.isPresent()) {
            try {
                String stringResult = (String) entry.getXPath().evaluate(node, XPathConstants.STRING);
                result = Optional.ofNullable(handleEvaluationResult(stringResult, entry.getFieldName()));
                if (result.isPresent()) {
                    log.trace("Found string result: {}", result.get());
                }
            } catch (XPathExpressionException e) {
                log.warn("Error selecting field {} as string: {}", entry.getFieldName(), e.getMessage());
                log.trace("Error selecting field {} as string", entry.getFieldName(), e);
            }
        }

        if (result.isPresent()) {
            EvalResult er = result.get();

            if (entry.hasReplacements()) {
                er = handleReplacements(entry, er);
            }
            if (entry.hasSplit()) {
                er = handleSplit(entry, er);
            }

            try {
                Object value = er.value;
                builder.field(er.name, value);
                log.debug("Added field: {} = {}", er.name, (value instanceof Object[]) ? Arrays.toString((Object[]) value) : value);
            } catch (IOException e) {
                log.warn("Error adding field {}: {}", entry.getFieldName(), e);
            }
        }
    }

    private EvalResult handleReplacements(MappingEntry entry, EvalResult er) {
        EvalResult result = null;
        Map<String, String> replacements = entry.getReplacements();
        log.trace("Applying replacements in {}: {}", er.name, Arrays.toString(replacements.entrySet().toArray()));
        if (er.value instanceof String) {
            result = new EvalResult(er.name, applyReplacements(replacements, (String) er.value));
        } else if (er.value instanceof String[]) {
            String[] val = (String[]) er.value;
            result = new EvalResult(er.name, Arrays.stream(val).map(currentVal -> {
                return applyReplacements(replacements, currentVal);
            }).collect(Collectors.toList()).toArray());
        } else if (er.value instanceof Object[]) {
            Object[] val = (Object[]) er.value;
            result = new EvalResult(er.name, Arrays.stream(val)
                    .filter(v -> v instanceof String)
                    .filter(Objects::nonNull)
                    .map(v -> (String) v)
                    .map(currentVal -> {
                        return applyReplacements(replacements, currentVal);
                    }).collect(Collectors.toList()).toArray());
        }

        log.trace("Result after replacements: {} (if null, then the original is returned)", result);
        if (result != null) {
            return result;
        }

        log.debug("No handling of replacement for given result implemented, returning input again: {}", er);
        return er;
    }

    private String applyReplacements(Map<String, String> replacements, String in) {
        String out = in;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            out = out.replace(replacement.getKey(), replacement.getValue());
        }
        return out;
    }

    private EvalResult handleSplit(MappingEntry entry, EvalResult er) {
        if (er.value instanceof String) {
            String value = (String) er.value;
            log.trace("Applying split in field {} with '{}' on {}", entry.getFieldName(), entry.getSplit(), value);
            String[] split = value.split(entry.getSplit());
            List<String> list = Arrays.asList(split);
            log.trace("Split resulted in {} items: {}", list.size(), Arrays.toString(list.toArray()));
            return new EvalResult(er.name, list);
        } else {
            log.trace("Split can only be applied to string value, but result was {} ({})", er.value, er.value.getClass());
            return er;
        }
    }

    private void mapRawEntry(MappingEntry entry, Node node, XContentBuilder builder) {
        try {
            // handle full xml
            Node nodesetResult = (Node) entry.getXPath().evaluate(node, XPathConstants.NODE);

            Map<String, String> outputProperties = Maps.newHashMap();
            outputProperties.putAll(DEFAULT_OUTPUT_PROPERTIES);
            if (entry.hasOutputProperties()) {
                outputProperties.putAll(entry.getOutputProperties());
            }
            String xmldoc = asString(nodesetResult, outputProperties);
            log.trace("Storing full XML to field {} starting with {}", entry.getFieldName(),
                    xmldoc.substring(0, Math.min(xmldoc.length(), 120)));
            builder.field(entry.getFieldName(), xmldoc);
        } catch (IOException | XPathExpressionException e) {
            log.warn("Error adding field {}: {}", entry.getFieldName(), e);
        }
    }

    private EvalResult handleEvaluationResult(final Object evalutationResult, final String name) {
        if (evalutationResult instanceof String) {
            String valueString = (String) evalutationResult;
            if (!valueString.isEmpty()) {
                log.trace("Found field {} = {}", name, valueString);
                return new EvalResult(name, valueString);
            } else {
                log.debug("Evaluation returned empty string for entry {}", name);
            }
        } else if (evalutationResult instanceof NodeList) {
            NodeList nodeList = (NodeList) evalutationResult;
            Optional<Object> value = Optional.empty();

            if (nodeList.getLength() < 1) {
                log.debug("Evaluation returned no results for entry {}", name);
            } else {
                Set<String> contents = Sets.newHashSet();
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node n = nodeList.item(i);
                    String textContent = n.getTextContent();
                    if (textContent != null && !textContent.isEmpty()) {
                        contents.add(textContent);
                    }
                }

                log.trace("{} evaluation result(s): {} = {}", contents.size(),
                        name, Arrays.toString(contents.toArray()));

                if (contents.size() == 1) {
                    value = Optional.of(contents.iterator().next());
                    log.trace("Adding field {} = '{}'", name, value.get());
                }
                if (contents.size() > 1) {
                    value = Optional.of(contents.toArray());
                    log.trace("Adding array field ({} values) {} = {}", contents.size(), name,
                            Arrays.toString(contents.toArray()));
                }
            }

            if (value.isPresent()) {
                return new EvalResult(name, value.get());
            } else {
                log.trace("No result found for field {}", name);
            }
        } else {
            log.debug("Unsupported evalutation result: {}", evalutationResult);
        }

        return null;
    }

    private String asString(Node node, Map<String, String> outputProperties) {
        log.debug("Converting node {} to string using properties {}", node, Arrays.toString(outputProperties.entrySet().toArray()));

        StringWriter sw = new StringWriter();
        try {
            Transformer t = null;
            if (outputProperties.get("indent").equals("no") && stripspaceTransformer.isPresent()) {
                t = stripspaceTransformer.get();
                log.trace("Will apply stripspace XSLT.");
            } else {
                t = tFactory.newTransformer();
            }

            for (Map.Entry<String, String> op : outputProperties.entrySet()) {
                t.setOutputProperty(op.getKey(), op.getValue());
            }

            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException e) {
            log.warn("Problem getting node {} as string", node, e);
        }

        return sw.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mapping", this.mapper)
                .add("transformerFactory", this.tFactory)
                .omitNullValues()
                .toString();
    }

    private static class IdAndBuilder {

        protected final String id;

        protected final XContentBuilder builder;

        public IdAndBuilder(String id, XContentBuilder builder) {
            Objects.nonNull(builder);
            this.id = id;
            this.builder = builder;
        }
    }

    private static class EvalResult {

        protected final String name;

        protected final Object value;

        public EvalResult(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("name", name)
                    .add("value", value)
                    .omitNullValues()
                    .toString();
        }

    }

}
