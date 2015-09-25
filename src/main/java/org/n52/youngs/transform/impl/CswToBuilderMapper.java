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

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.n52.youngs.harvest.NamespaceContextImpl;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
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

    private final XPathFactory factory;

    private final NamespaceContext nc;

    public CswToBuilderMapper(MappingConfiguration mapper) {
        this.mapper = mapper;
        factory = XPathFactory.newInstance();
        nc = NamespaceContextImpl.create();
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
                XContentBuilder builder = mapNodeToBuilder(object.getRecord());

                record = new BuilderRecord(builder);
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

    private XContentBuilder mapNodeToBuilder(final Node node) throws IOException {
        XPath xPath = factory.newXPath();
        xPath.setNamespaceContext(nc);

        XContentBuilder builder = XContentFactory.jsonBuilder()
                .humanReadable(true)
                .prettyPrint()
                .startObject();

        // evaluate xpaths and save the results in the builder
        Collection<MappingEntry> entries = mapper.getEntries();
        log.trace("Mapping node {} using {} entries", node, entries.size());

        entries.forEach(entry -> {
            try {
                String value = xPath.evaluate(entry.getXPath(), node);
                if (!value.isEmpty()) {
                    builder.field(entry.getFieldName(), value);
                    log.trace("Added field {} = {}", entry.getFieldName(), value);
                }
                log.trace("Not adding empty field {}", entry.getFieldName());
            } catch (XPathExpressionException | IOException e) {
                log.warn("Error selecting fields from node", e);
            }
        });

        builder.endObject();
        builder.close();

        log.trace("Created content:\n{}", builder.string());

        return builder;

    }

}
