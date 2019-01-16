/*
 * Copyright 2015-2019 52°North Initiative for Geospatial Open Source
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
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import net.opengis.csw.v_2_0_2.AbstractRecordType;
import net.opengis.csw.v_2_0_2.GetRecordsResponseType;
import org.apache.http.client.fluent.Request;
import org.n52.youngs.api.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class KvpCswSource extends CswSource {

    private static final Logger log = LoggerFactory.getLogger(KvpCswSource.class);

    private static final Joiner.MapJoiner urlParameterJoiner = Joiner.on("&").withKeyValueSeparator("=");

    public KvpCswSource(String url, NamespaceContext nsContext) throws MalformedURLException, JAXBException {
        super(url, nsContext);
    }

    public KvpCswSource(String url, Collection<String> namespaces, NamespaceContext nsContext, String typeName, String outputSchema) throws MalformedURLException, JAXBException {
        super(url, namespaces, nsContext, typeName, outputSchema);
    }

    public KvpCswSource(URL url, String namespacesParameter, String typeName, String outputSchema) throws JAXBException {
        super(url, namespacesParameter, typeName, outputSchema);
    }

    public KvpCswSource(URL url, Collection<String> namespaces, NamespaceContext nsContext, String typeName, String outputSchema) throws JAXBException {
        super(url, namespaces, nsContext, typeName, outputSchema);
    }

    @Override
    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report) {
        log.debug("Requesting {} records from catalog starting at {}", maxRecords, startPosition);
        Collection<SourceRecord> records = Lists.newArrayList();

        String recordsRequest = createRequest(startPosition, maxRecords);
        log.trace("GetRecords request: {}", recordsRequest);

        try {
            InputStream response = Request.Get(recordsRequest).execute().returnContent().asStream();

            JAXBElement<GetRecordsResponseType> jaxb_response = unmarshaller.unmarshal(new StreamSource(response),
                    GetRecordsResponseType.class);
            BigInteger numberOfRecordsReturned = jaxb_response.getValue().getSearchResults().getNumberOfRecordsReturned();
            log.debug("Got response with {} records", numberOfRecordsReturned);

            List<Object> nodes = jaxb_response.getValue().getSearchResults().getAny();
            if (!nodes.isEmpty()) {
                log.trace("Found {} \"any\" nodes.", nodes.size());
                nodes.stream()
                        .filter(n -> n instanceof Node)
                        .map(n -> (Node) n)
                        .map(n -> new NodeSourceRecord(n))
                        .forEach(records::add);
            }

            List<JAXBElement<? extends AbstractRecordType>> jaxb_records = jaxb_response.getValue().getSearchResults().getAbstractRecord();
            if (!jaxb_records.isEmpty()) {
                log.trace("Found {} \"AbstractRecordType\" records.", jaxb_records.size());
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                jaxb_records.stream()
                        .map(type -> {
                            return getNode(type, context, db);
                        })
                        .filter(Objects::nonNull)
                        .map(n -> new NodeSourceRecord(n))
                        .forEach(records::add);
            }
        } catch (IOException | JAXBException | ParserConfigurationException e) {
            log.error("Could not retrieve records using url {}", recordsRequest, e);
            report.addMessage(String.format("Error retrieving record from endpoint %s: %s", this, e));
        }

        return records;
    }

    private String createRequest(long startPosition, long maxRecords) {
        StringBuilder recordsRequest = new StringBuilder();

        URL url = getEndpoint();
        recordsRequest.append(url);
        if (!url.toString().endsWith("?")) {
            recordsRequest.append("?");
        }
        String fixedParameters = urlParameterJoiner.join(
                ImmutableMap.of("service", "CSW",
                        "version", "2.0.2",
                        "request", "GetRecords",
                        "resultType", "results",
                        "ElementSetName", "full"));
        recordsRequest.append(fixedParameters);
        String parameters = urlParameterJoiner.join(
                ImmutableMap.of("namespace", getNamespacesParameter(),
                        "typeNames", getTypeNamesParameter(),
                        "outputSchema", getOutputSchemaParameter(),
                        "startPosition", startPosition,
                        "maxRecords", maxRecords));
        recordsRequest.append("&").append(parameters);
        return recordsRequest.toString();
    }

    @Override
    protected Supplier<? extends Long> getAndStoreRecordCount() {
        Supplier<Long> s = new CswRecordCountSupplier();
        recordCount = Optional.of(s.get());
        return s;
    }

    private class CswRecordCountSupplier implements Supplier<Long> {

        public CswRecordCountSupplier() {
            //
        }

        @Override
        public Long get() {
            URL url = getEndpoint();
            log.debug("Requesting record count at {} using {} and {}", url, getTypeNamesParameter(), getNamespacesParameter());
            Long count = Long.MIN_VALUE;

            // http://api.eurogeoss-broker.eu/dab/services/cswiso?service=CSW&version=2.0.2&
            //request=GetRecords&namespace=xmlns%28csw=http://www.opengis.net/cat/csw/2.0.2%29%29&typeNames=csw:Record&ElementSetName=summary
            StringBuilder hitsRequest = new StringBuilder();

            hitsRequest.append(url);
            if (!url.toString().endsWith("?")) {
                hitsRequest.append("?");
            }
            String fixedParameters = urlParameterJoiner.join(
                    ImmutableMap.of("service", "CSW",
                            "version", "2.0.2",
                            "request", "GetRecords",
                            "resultType", "hits",
                            "ElementSetName", "summary"));
            hitsRequest.append(fixedParameters);
            String parameters = urlParameterJoiner.join(
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

            return count;
        }
    }

}
