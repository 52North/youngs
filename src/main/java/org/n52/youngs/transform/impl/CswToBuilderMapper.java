/*
 * Copyright 2015-2016 52°North Initiative for Geospatial Open Source
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

import com.google.common.base.MoreObjects;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
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
        Optional<EntryMapper.EvalResult> result = new EntryMapper().mapEntry(entry, node);

        if (result.isPresent()) {
            EvalResult er = result.get();

            try {
                Object value = er.value;
                builder.field(er.name, value);
                log.debug("Added field: {} = {}", er.name, (value instanceof Object[]) ? Arrays.toString((Object[]) value) : value);
            } catch (IOException e) {
                log.warn("Error adding field {}: {}", entry.getFieldName(), e);
            }
        }
    }

    private void mapRawEntry(MappingEntry entry, Node node, XContentBuilder builder) {
        try {
            String xmldoc = new EntryMapper(stripspaceTransformer, defaultTransformer).mapRawEntry(entry, node);
            builder.field(entry.getFieldName(), xmldoc);
        } catch (IOException | XPathExpressionException e) {
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


    private static class IdAndBuilder {

        protected final String id;

        protected final XContentBuilder builder;

        public IdAndBuilder(String id, XContentBuilder builder) {
            Objects.nonNull(builder);
            this.id = id;
            this.builder = builder;
        }
    }

}
