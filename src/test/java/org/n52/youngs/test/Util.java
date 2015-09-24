/*
 * Copyright 2015-${currentYearDynamic} 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.test;

import com.google.common.collect.Lists;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import net.opengis.csw.v_2_0_2.GetRecordsResponseType;
import org.n52.youngs.api.Record;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class Util {

    public static Collection<Record> loadGetRecordsResponse(InputStream input) throws Exception {
        JAXBContext context = JAXBContext.newInstance("net.opengis.csw.v_2_0_2");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        Collection<Record> records = Lists.newArrayList();

        JAXBElement<GetRecordsResponseType> jaxb_response = unmarshaller.unmarshal(new StreamSource(input), GetRecordsResponseType.class);

        List<Object> nodes = jaxb_response.getValue().getSearchResults().getAny();
        nodes.stream()
                .filter(n -> n instanceof Node)
                .map(n -> (Node) n)
                .map(n -> new NodeSourceRecord(n))
                .forEach(records::add);

        return records;
    }

    public static Document getDocument(String xmlString) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlString));
        Document doc = db.parse(is);
        return doc;
    }

}
