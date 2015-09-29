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

import com.google.common.collect.Sets;
import java.io.IOException;
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

        entries.forEach(entry -> {
            log.trace("Applying field mapping '{}' to {}", entry.getFieldName(), node);

            try {
                Object evalutationResult = entry.getXPath().evaluate(node, XPathConstants.NODESET);
                if (evalutationResult instanceof String) {
                    String valueString = (String) evalutationResult;
                    if (!valueString.isEmpty()) {
                        builder.field(entry.getFieldName(), valueString);
                        log.trace("Added field {} = {}", entry.getFieldName(), valueString);
                    }
                } else if (evalutationResult instanceof NodeList) {
                    NodeList nodeList = (NodeList) evalutationResult;
                    Optional<Object> value = Optional.empty();

                    if (nodeList.getLength() < 1) {
                        log.debug("Evaluation returned no results for entry {} in {}", entry.getFieldName(), node);
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
                                entry.getFieldName(), Arrays.toString(contents.toArray()));

                        if (contents.size() == 1) {
                            value = Optional.of(contents.iterator().next());
                            log.trace("Adding field {} = '{}'", entry.getFieldName(), value.get());
                        }
                        if (contents.size() > 1) {
                            value = Optional.of(contents.toArray());
                            log.trace("Adding array field ({} values) {} = {}", contents.size(), entry.getFieldName(),
                                    Arrays.toString(contents.toArray()));
                        }
                    }

                    if (value.isPresent()) {
                        builder.field(entry.getFieldName(), value.get());
                    }
                    else {
                        log.trace("No result found for field {} in {}", entry.getFieldName(), node);
                    }
                } else {
                    log.debug("Unsupported evalutation result: {}", evalutationResult);
                }

            } catch (XPathExpressionException | IOException e) {
                log.warn("Error selecting fields from node", e);
            }
        });

        builder.endObject();
        builder.close();

        log.trace("Created content for id '{}':\n{}", id, builder.string());

        return new IdAndBuilder(id, builder);
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
