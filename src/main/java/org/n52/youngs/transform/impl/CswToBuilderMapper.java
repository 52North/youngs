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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.xml.xpath.XPathConstants;
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

    public CswToBuilderMapper(MappingConfiguration mapper) {
        this.mapper = mapper;
    }

    @Override
    public MappingConfiguration getMapper() {
        return mapper;
    }

    /**
     * @return a record containing a builder of the provided SourceRecord, or null if the mapper could not be completed.
     */
    @Override
    public BuilderRecord map(SourceRecord source) {
        Objects.nonNull(source);
        BuilderRecord record = null;

        if (source instanceof NodeSourceRecord) {
            try {
                NodeSourceRecord object = (NodeSourceRecord) source;
                IdAndBuilder mappedRecord = mapNodeToBuilder(object.getRecord());

                record = new BuilderRecord(mappedRecord.id, mappedRecord.builder);
                return record;
            } catch (IOException e) {
                log.warn("Error mapping the source {}", source, e);
                return null;
            }
        } else {
            log.warn("The SourceRecord class {} is not supported", source.getClass().getName());
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
        entries.stream().filter(e -> !e.hasCoordinates()).forEach(entry -> {
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
                log.warn("Error selecting field {} as nodeset, could be XPath 2.0 expression... trying evaluation to string."
                        + " Error was: {}", entry.getFieldName(), e.getMessage());
                log.trace("Error selecting field {} as nodeset", entry.getFieldName(), e);
            }

            // try string eval if nodeset did not work
            if (!result.isPresent()) {
                try {
                    String stringResult = entry.getXPath().evaluate(node);
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
                try {
                    builder.field(result.get().name, result.get().value);
                    log.debug("Added field: {}", result);
                } catch (IOException e) {
                    log.warn("Error adding field {} to builder", entry.getFieldName(), e);
                }
            }
        });

        // handle geo types
        entries.stream().filter(e -> e.hasCoordinates()).forEach(entry -> {
            try {
                Object coordsNode = entry.getXPath().evaluate(node, XPathConstants.NODE);
                String coordsString = entry.getCoordinatesXPath().evaluate(coordsNode);
                String geoType = (String) entry.getIndexPropery(MappingEntry.IndexProperties.TYPE);
                String field = (String) entry.getIndexPropery(MappingEntry.INDEX_NAME);

                if (!coordsString.isEmpty() && !geoType.isEmpty() && !field.isEmpty() && entry.hasCoordinatesType()) {
                    builder.startObject(field)
                            .field(MappingEntry.IndexProperties.TYPE, entry.getCoordinatesType())
                            .field("coordinates", coordsString)
                            .endObject();
                    log.debug("Added coordinates '{}' as {} of type {}", coordsString, geoType, entry.getCoordinatesType());
                }
                else {
                    log.warn("Mapping '{}' has coordinates but is missing one of the other required settings, not adding field: "
                            + "node = {}, index_name = {}, coordinates_type = {}, type = {}, evalutated coords string = {}",
                            entry.getFieldName(), coordsNode, field, entry.getCoordinatesType(), geoType, coordsString);
                }
            } catch (XPathExpressionException | IOException e) {
                log.warn("Error selecting coordinate-field {} as node. Error was: {}", entry.getFieldName(), e.getMessage());
                log.trace("Error selecting field {} as nodeset", entry.getFieldName(), e);
            }
        });

        builder.endObject();
        builder.close();

        log.trace("Created content for id '{}':\n{}", id, builder.string());

        return new IdAndBuilder(id, builder);
    }

    private EvalResult handleEvaluationResult(final Object evalutationResult, final String name) {
        if (evalutationResult instanceof String) {
            String valueString = (String) evalutationResult;
            if (!valueString.isEmpty()) {
                log.trace("Found field {} = {}", name, valueString);
                return new EvalResult(name, valueString);
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
                    if (!textContent.isEmpty()) {
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
                    .add("name", name).add("value", value).omitNullValues().toString();
        }

    }

}
