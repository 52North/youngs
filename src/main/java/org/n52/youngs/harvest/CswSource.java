/*
 * Copyright 2015-2017 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.harvest;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import net.opengis.csw.v_2_0_2.AbstractRecordType;
import org.n52.youngs.api.Report;
import org.n52.youngs.impl.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public abstract class CswSource implements Source {

    private static final Logger log = LoggerFactory.getLogger(CswSource.class);

    private final URL url;

    protected Optional<Long> recordCount = Optional.empty();

    private String typeNames;

    private Optional<String> namespacesParameter = Optional.empty();

    Optional<Collection<String>> namespaces = Optional.empty();

    private String outputSchema;

    protected JAXBContext context;

    protected Unmarshaller unmarshaller;

    private NamespaceContext namespaceContext;

    public CswSource(String url, NamespaceContext nsContext) throws MalformedURLException, JAXBException {
        this(url, DEFAULT_NAMESPACES, nsContext, DEFAULT_TYPE_NAME, DEFAULT_OUTPUT_SCHEMA);
    }

    public CswSource(String url, Collection<String> namespaces, NamespaceContext nsContext,
            String typeName, String outputSchema) throws MalformedURLException, JAXBException {
        this(new URL(url), namespaces, nsContext, typeName, outputSchema);
    }

    public CswSource(URL url, String namespacesParameter, String typeName,
            String outputSchema) throws JAXBException {
        this.url = url;
        this.typeNames = typeName;
        this.namespacesParameter = Optional.of(namespacesParameter);
        this.outputSchema = outputSchema;

        init();
    }

    public CswSource(URL url, Collection<String> namespaces, NamespaceContext nsContext,
            String typeName, String outputSchema) throws JAXBException {
        this.url = url;
        this.typeNames = typeName;
        this.namespaces = Optional.of(namespaces);
        this.outputSchema = outputSchema;
        this.namespaceContext = nsContext;

        init();
    }

    private void init() throws JAXBException {
        context = ContextHelper.getContextForNamespace(this.outputSchema);
        unmarshaller = context.createUnmarshaller();
    }

    @Override
    public URL getEndpoint() {
        return this.url;
    }

    @Override
    public long getRecordCount() {
        return recordCount.orElseGet(getAndStoreRecordCount());
    }

    @Override
    public Collection<SourceRecord> getRecords(Report report) {
        return getRecords(1, Long.MAX_VALUE, report);
    }

    @Override
    public abstract Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report);

    protected abstract Supplier<? extends Long> getAndStoreRecordCount();

    protected Node getNode(JAXBElement<? extends AbstractRecordType> record, JAXBContext context, DocumentBuilder db) {
        try {
            Document document = db.newDocument();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(record, document);
            Element elem = document.getDocumentElement();
            return elem;
        } catch (JAXBException e) {
            log.warn("Error getting node from record {}: {} > {}", record, e, e.getMessage());
            return null;
        }
    }

    protected String getNamespacesParameter() {
        return namespacesParameter.orElseGet(new NamespacesParameterSupplier(
                namespaces.orElse(DEFAULT_NAMESPACES), namespaceContext));
    }

    protected String getTypeNamesParameter() {
        return typeNames;
    }

    public String getOutputSchemaParameter() {
        return outputSchema;
    }

        /**
     * "xmlns(csw=http://www.opengis.net/cat/csw)";
     */
    private class NamespacesParameterSupplier implements Supplier<String> {

        private final Collection<String> namespaces;

        private final NamespaceContext namespaceContext;

        public NamespacesParameterSupplier(Collection<String> namespaces, NamespaceContext namespaceContext) {
            this.namespaces = namespaces;
            this.namespaceContext = namespaceContext;
        }

        @Override
        public String get() {
            StringBuilder sb = new StringBuilder();

            sb.append("xmlns(");
            Map<String, String> ns = Maps.uniqueIndex(namespaces, namespaceContext::getPrefix);
            Joiner.on(";").withKeyValueSeparator("=").appendTo(sb, ns);
            sb.append(")");

            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("url", getEndpoint())
                .omitNullValues().toString();
    }



}
