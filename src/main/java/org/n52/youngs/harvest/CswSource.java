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
package org.n52.youngs.harvest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.stream.StreamSource;
import net.opengis.csw.v_2_0_2.GetRecordsResponseType;
import org.apache.http.client.fluent.Request;
import org.elasticsearch.common.collect.Lists;
import org.n52.youngs.api.Record;
import org.n52.youngs.impl.ContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswSource implements Source {

    private static final Logger log = LoggerFactory.getLogger(CswSource.class);

    private final URL url;

    private Optional<Long> recordCount = Optional.empty();

    private String typeNames;

    private Optional<String> namespacesParameter = Optional.empty();

    private Optional<Collection<String>> namespaces = Optional.empty();

    private String outputSchema;

    private JAXBContext context;

    private Unmarshaller unmarshaller;

    public CswSource(String url) throws MalformedURLException, JAXBException {
        this(url, DEFAULT_NAMESPACES, DEFAULT_TYPE_NAME, DEFAULT_OUTPUT_SCHEMA);
    }

    public CswSource(String url, Collection<String> namespaces, String typeName,
            String outputSchema) throws MalformedURLException, JAXBException {
        this(new URL(url), namespaces, typeName, outputSchema);
    }

    public CswSource(URL url, String namespacesParameter, String typeName,
            String outputSchema) throws JAXBException {
        this.url = url;
        this.typeNames = typeName;
        this.namespacesParameter = Optional.of(namespacesParameter);
        this.outputSchema = outputSchema;

        init();
    }

    public CswSource(URL url, Collection<String> namespaces, String typeName,
            String outputSchema) throws JAXBException {
        this.url = url;
        this.typeNames = typeName;
        this.namespaces = Optional.of(namespaces);
        this.outputSchema = outputSchema;

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
        return recordCount.orElseGet(new CswRecordCountSupplier());
    }

    @Override
    public Collection<Record> getRecords() {
        return getRecords(1, Long.MAX_VALUE);
    }

    @Override
    public Collection<Record> getRecords(long startPosition, long maxRecords) {
        log.debug("Requesting {} records from catalog starting at {}", maxRecords, startPosition);
        Collection<Record> records = Lists.newArrayList();

        // http://api.eurogeoss-broker.eu/dab/services/cswiso?service=CSW&version=2.0.2&request=GetRecords&namespace=xmlns(gmd=http://www.isotc211.org/2005/gmd)&typeNames=gmd:MD_Metadata&ElementSetName=full&resultType=results&maxRecords=10&outputSchema=http://www.isotc211.org/2005/gmd
        StringBuilder recordsRequest = new StringBuilder();
        recordsRequest.append(url);
        if (!url.toString().endsWith("?")) {
            recordsRequest.append("?");
        }
        String fixedParameters = Joiner.on("&").withKeyValueSeparator("=").join(
                ImmutableMap.of("service", "CSW",
                        "version", "2.0.2",
                        "request", "GetRecords",
                        "resultType", "results",
                        "ElementSetName", "full"));
        recordsRequest.append(fixedParameters);
        String parameters = Joiner.on("&").withKeyValueSeparator("=").join(
                ImmutableMap.of("namespace", getNamespacesParameter(),
                        "typeNames", getTypeNamesParameter(),
                        "outputSchema", getOutputSchemaParameter()));
        recordsRequest.append("&").append(parameters);

        try {
            log.trace("GetRecords request: {}", recordsRequest);
            InputStream response = Request.Get(recordsRequest.toString()).execute().returnContent().asStream();

            JAXBElement<GetRecordsResponseType> jaxb_response = unmarshaller.unmarshal(new StreamSource(response),
                    GetRecordsResponseType.class);
            BigInteger numberOfRecordsReturned = jaxb_response.getValue().getSearchResults().getNumberOfRecordsReturned();
            log.trace("Got response with {} records", numberOfRecordsReturned);

            List<Object> nodes = jaxb_response.getValue().getSearchResults().getAny();

            nodes.stream()
                    .filter(n -> n instanceof Node)
                    .map(n -> (Node) n)
                    .map(n -> new NodeSourceRecord(n))
                    .forEach(records::add);
        } catch (IOException e) {
            log.error("Could not retrieve record count using url {}", recordsRequest, e);
        } catch (JAXBException ex) {
            java.util.logging.Logger.getLogger(CswSource.class.getName()).log(Level.SEVERE, null, ex);
        }

        return records;
    }

    private String getNamespacesParameter() {
        return namespacesParameter.orElseGet(new NamespacesParameterSupplier(
                namespaces.orElse(DEFAULT_NAMESPACES)));
    }

    private String getTypeNamesParameter() {
        return typeNames;
    }

    public String getOutputSchemaParameter() {
        return outputSchema;
    }

    private class CswRecordCountSupplier implements Supplier<Long> {

        public CswRecordCountSupplier() {
            //
        }

        @Override
        public Long get() {
            log.debug("Requesting record count at {} using {} and {}", url, typeNames, namespaces);
            Long count = Long.MIN_VALUE;

            // http://api.eurogeoss-broker.eu/dab/services/cswiso?service=CSW&version=2.0.2&
            //request=GetRecords&namespace=xmlns%28csw=http://www.opengis.net/cat/csw/2.0.2%29%29&typeNames=csw:Record&ElementSetName=summary
            StringBuilder hitsRequest = new StringBuilder();
            hitsRequest.append(url);
            if (!url.toString().endsWith("?")) {
                hitsRequest.append("?");
            }
            String fixedParameters = Joiner.on("&").withKeyValueSeparator("=").join(
                    ImmutableMap.of("service", "CSW",
                            "version", "2.0.2",
                            "request", "GetRecords",
                            "resultType", "hits",
                            "ElementSetName", "summary"));
            hitsRequest.append(fixedParameters);
            String parameters = Joiner.on("&").withKeyValueSeparator("=").join(
                    ImmutableMap.of("namespace", getNamespacesParameter(),
                            "typeNames", getTypeNamesParameter()));
            hitsRequest.append("&").append(parameters);

            try {
                InputStream response = Request.Get(hitsRequest.toString()).execute().returnContent().asStream();

                GetRecordsResponseType getRecordsResponse = unmarshaller.unmarshal(new StreamSource(response), GetRecordsResponseType.class).getValue();
                BigInteger numberOfRecordsMatched = getRecordsResponse.getSearchResults().getNumberOfRecordsMatched();
                count = numberOfRecordsMatched.longValue();
                log.debug("Found {} records", count);
            } catch (IOException | JAXBException e) {
                log.error("Could not retrieve record count using url {}", hitsRequest, e);
            }

            // FIXME inproper use of supplier
            recordCount = Optional.of(count);

            return count;
        }
    }

    /**
     * "xmlns(csw=http://www.opengis.net/cat/csw)";
     */
    private class NamespacesParameterSupplier implements Supplier<String> {

        private final Collection<String> namespaces;

        public NamespacesParameterSupplier(Collection<String> namespaces) {
            this.namespaces = namespaces;
        }

        @Override
        public String get() {
            StringBuilder sb = new StringBuilder();
            NamespaceContext nc = NamespaceContextImpl.create();

            sb.append("xmlns(");
            Map<String, String> ns = Maps.uniqueIndex(namespaces, nc::getPrefix);
            Joiner.on(";").withKeyValueSeparator("=").appendTo(sb, ns);
            sb.append(")");

            return sb.toString();
        }
    }

}
