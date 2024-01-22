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

import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.n52.youngs.transform.impl.EntryMapper.EvalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswToBuilderMapper implements Mapper {

    private static final Logger log = LoggerFactory.getLogger(CswToBuilderMapper.class);

    private final MappingConfiguration mapper;

    private Optional<Transformer> stripspaceTransformer = Optional.empty();
    private Transformer defaultTransformer;

    public CswToBuilderMapper(MappingConfiguration mapper) {
        this.mapper = mapper;

        TransformerFactory tFactory = TransformerFactory.newInstance();

        try (InputStream is = Resources.getResource("xslt/stripspace.xslt").openStream();) {
            Source xslt = new StreamSource(is);
            stripspaceTransformer = Optional.of(tFactory.newTransformer(xslt));
            log.trace("Will apply stripspace XSLT.");
        } catch (TransformerConfigurationException | IOException e) {
            log.error("Problem loading strip-space XSLT file.", e);
        }

        try {
            defaultTransformer = tFactory.newTransformer();
        } catch (TransformerConfigurationException ex) {
            log.error("Problem loading deault Transformer.", ex);
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
                IdAndData mappedRecord = mapNodeToBuilder(object.getRecord());

                if (mappedRecord == null) {
                    return null;
                }

                record = new BuilderRecord(mappedRecord.id, mappedRecord.data);
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

    private IdAndData mapNodeToBuilder(final Node node) throws IOException {
        ObjectMapper builder = new ObjectMapper();
        ObjectNode root = builder.createObjectNode();


        // evaluate xpaths and save the results in the builder
        Collection<MappingEntry> entries = mapper.getEntries();
        log.trace("Mapping node {} using {} entries", node, entries.size());

        String id = null;
        try {
            Optional<MappingEntry> idEntry = entries.stream().filter(MappingEntry::isIdentifier).findFirst();
            if (idEntry.isPresent()) {
                id = idEntry.get().getXPath().evaluate(node);
                id = (id == null || id.trim().isEmpty()) ? null : id.trim();
            }
            if (id == null) {
                log.warn("No ID present, skipping");
                return null;
            }

            log.trace("Found id for node: {}", id);
        } catch (XPathExpressionException e) {
            log.warn("Error selecting id field from node", e);
        }

        // handle non-geo entries
        List<EvalResult> mappedEntries = entries.stream().filter(e -> !e.hasCoordinates() && !e.isRawXml())
                .map(entry -> mapEntry(entry, node))
                .filter(entry -> entry.isPresent())
                .map(entry -> entry.get())
                .collect(Collectors.toList());

        // apply a filter for entries (e.g. used by sub-classes)
        if (!assessFilter(mappedEntries)) {
            log.debug("record will be skipped as it did not pass the filter");
            return null;
        }

        mappedEntries.stream()
                .forEach(er -> {
                    try {
                        JsonNode value = builder.valueToTree(er.value);
                        root.set(er.name, value);
                        log.debug("Added field: {} = {}", er.name, (er.value instanceof Object[]) ? Arrays.toString((Object[]) er.value) : er.value);
                    } catch (IllegalArgumentException e) {
                        log.warn("Error adding field {}: {}", er.name, e);
                    }
                });

        // handle geo types
        entries.stream().filter(e -> e.hasCoordinates() && !e.isRawXml()).forEach(entry -> {
            mapSpatialEntry(entry, node, builder, root);
        });

        // handle raw types
        entries.stream().filter(e -> e.isRawXml()).forEach(entry -> {
            mapRawEntry(entry, node, root);
        });

        if (mapper.hasSuggest()) {
            handleSuggest( builder, root, mapper.getSuggest(), mappedEntries);
        }

        String jsonString = builder.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        log.trace("Created content for id '{}':\n{}", id, jsonString);
        JsonData data = JsonData.fromJson(jsonString);

        return new IdAndData(id, data);
    }

    private void mapSpatialEntry(MappingEntry entry, final Node node, ObjectMapper builder, ObjectNode root) {
        log.trace("Applying field mapping '{}' to node: {}", entry.getFieldName(), node);
        try {
            Object coordsNode = entry.getXPath().evaluate(node, XPathConstants.NODE);
            if (coordsNode != null) {
                String geoType = (String) entry.getIndexPropery(MappingEntry.IndexProperties.TYPE);
                String field = entry.getFieldName();

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

                    Map<String, Object>  spatialEntry = new HashMap<>();
                    spatialEntry.put(MappingEntry.IndexProperties.TYPE, entry.getCoordinatesType());
                    spatialEntry.put("coordinates", pointsDoubles);
                    JsonNode child = builder.valueToTree(spatialEntry);
                    root.set(field, child);

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
        } catch (XPathExpressionException | IllegalArgumentException e) {
            log.warn("Error selecting coordinate-field {} as node. Error was: {}", entry.getFieldName(), e.getMessage());
            log.trace("Error selecting field {} as nodeset", entry.getFieldName(), e);
        }
    }

    private Optional<EvalResult> mapEntry(MappingEntry entry, final Node node) {
        Optional<EntryMapper.EvalResult> result = new EntryMapper().mapEntry(entry, node);

        return result;
    }

    private void mapRawEntry(MappingEntry entry, Node node, ObjectNode root) {
        try {
            String xmldoc = new EntryMapper(stripspaceTransformer, defaultTransformer).mapRawEntry(entry, node);
            root.put(entry.getFieldName(), xmldoc);
        } catch (IllegalArgumentException | XPathExpressionException e) {
            log.warn("Error adding field {}: {}", entry.getFieldName(), e);
        }
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("mapping", this.mapper)
                .add("defaultTransformer", this.defaultTransformer)
                .omitNullValues()
                .toString();
    }

    private void handleSuggest(ObjectMapper builder, ObjectNode root, Map<String, Object> suggestDef, List<EvalResult> mappingEntries) throws IOException {
        Map<String, Object> suggest = (Map<String, Object>) suggestDef.get("mappingConfiguration");
        List<String> inputs = new ArrayList<>();

        Boolean singleWords = extractValue(suggest, "input_as_single_words", true);
        String splitSep = extractValue(suggest, "split", " ");
        Boolean fullOutput = extractValue(suggest, "full_output", true);
        List<String> inputExlucdes = extractValue(suggest, "input_exlucdes", Collections.emptyList());
        List<String> inputRemoves = extractValue(suggest, "input_remove", Collections.emptyList());
        Integer weight = extractValue(suggest, "weight", 1);
        List<String> entries = extractValue(suggest, "entries", Collections.emptyList());

        List<Map<String, Object>> suggestEntries = entries.stream()
                .map(fieldName -> {
                    Optional<Object> fieldValue = mappingEntries.stream()
                            .filter(me -> fieldName.equals(me.getName()))
                            .map(me -> me.getValue())
                            .findFirst();

                    if (!fieldValue.isPresent() || !(fieldValue.get() instanceof String)) {
                        return null;
                    }

                    String[] fieldArray = fieldValue.get().toString().split(splitSep);
                    List<String> inputList = Arrays.asList(fieldArray).stream()
                            .filter(s -> {
                                return inputExlucdes.stream().noneMatch((ex) -> (s.equalsIgnoreCase(ex) || s.matches(ex)));
                            })
                            .map(s -> {
                                for (String inputRemove : inputRemoves) {
                                    s = s.replace(inputRemove, "");
                                }
                                return s.trim();
                            })
                            .collect(Collectors.toList());
                    Map<String, Object> map = new HashMap<>();
                    map.put("inputs", inputList);
                    map.put("weight", weight);
                    map.put("output", fieldValue.get());
                    return map;
                })
                .filter(e -> e != null)
                .collect(Collectors.toList());

        if (suggestEntries.isEmpty()) {
            return;
        }
        else if (suggestEntries.size() > 1) {
            JsonNode value = builder.valueToTree(suggestEntries);
            root.set("suggest", value);
        }
        else { //exactly one suggestEntry
            JsonNode value = builder.valueToTree(suggestEntries.get(0));
            root.set("suggest", value);
        }
    }

    private <V> V extractValue(Map<String, Object> map, String key, V defaultValue) {
        V value = (V) map.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * this method can be used by sub-classes to apply a filter on entries.
     *
     * @param mappedEntries input entries
     * @return if the record passed the filter and should be considered for ingestion
     */
    public boolean assessFilter(List<EvalResult> mappedEntries) {
        return true;
    }

    private static class IdAndData {

        protected final String id;

        protected final JsonData data;

        public IdAndData(String id, JsonData data) {
            Objects.nonNull(data);
            this.id = id;
            this.data = data;
        }
    }

}
