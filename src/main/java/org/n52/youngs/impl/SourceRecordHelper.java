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
package org.n52.youngs.impl;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import net.opengis.csw.v_2_0_2.AbstractRecordType;
import net.opengis.csw.v_2_0_2.GetRecordsResponseType;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SourceRecordHelper {

    public static Collection<SourceRecord> loadGetRecordsResponse(InputStream input) throws Exception {
        JAXBContext context = JAXBContext.newInstance("net.opengis.csw.v_2_0_2");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Collection<SourceRecord> records = Lists.newArrayList();

        JAXBElement<GetRecordsResponseType> jaxb_response = unmarshaller.unmarshal(new StreamSource(input), GetRecordsResponseType.class);

        List<Object> nodes = jaxb_response.getValue().getSearchResults().getAny();
        if (!nodes.isEmpty()) {
            nodes.stream()
                    .filter(n -> n instanceof Node)
                    .map(n -> (Node) n)
                    .map(n -> new NodeSourceRecord(n))
                    .forEach(records::add);
        }
        List<JAXBElement<? extends AbstractRecordType>> jaxb_records = jaxb_response.getValue().getSearchResults().getAbstractRecord();
        if (!jaxb_records.isEmpty()) {
            jaxb_records.stream()
                    .map(type -> {
                        return getNode(type, context);
                    })
                    .filter(Objects::nonNull)
                    .map(n -> new NodeSourceRecord(n))
                    .forEach(records::add);
        }

        return records;
    }

    private static Node getNode(JAXBElement<? extends AbstractRecordType> record, JAXBContext context) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = db.newDocument();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(record, document);
            Element elem = document.getDocumentElement();
            return elem;
        } catch (JAXBException | ParserConfigurationException e) {
            System.out.println(String.format("Error getting node from record %s: %s > %s", record, e, e.getMessage()));
            return null;
        }
    }

    public static SourceRecord getSourceRecordFromFile(String filename) throws Exception {
        try (InputStream is = Resources.asByteSource(Resources.getResource(filename)).openStream();) {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = documentBuilder.parse(is);
            Element elem = doc.getDocumentElement();
            elem.normalize();
            NodeSourceRecord record = new NodeSourceRecord(elem);
            return record;
        }
    }

}
