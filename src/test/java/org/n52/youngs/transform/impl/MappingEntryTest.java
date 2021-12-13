/*
 * Copyright 2015-2021 52°North Initiative for Geospatial Open Source
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.XPathHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class MappingEntryTest {

    @Test
    public void testCondition() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        EntryMapper mapper = new EntryMapper();

        Node node = readNode("/records/gmd/metadata_1000.xml");

        XPathFactory xpathFactory = new XPathHelper().newXPathFactory();
        XPath xPath = xpathFactory.newXPath();
        Map<String, String> namespaceMap = new HashMap<>();

        namespaceMap.put("gmi", "http://www.isotc211.org/2005/gmi");
        namespaceMap.put("gmd", "http://www.isotc211.org/2005/gmd");
        namespaceMap.put("gco", "http://www.isotc211.org/2005/gco");
        namespaceMap.put("gml", "http://www.opengis.net/gml");
        namespaceMap.put("gmi", "http://www.eumetsat.int/2008/gmi");
        namespaceMap.put("apiso", "http://www.opengis.net/cat/csw/apiso/1.0");


        NamespaceContextImpl nsContext = new NamespaceContextImpl(namespaceMap);
        xPath.setNamespaceContext(nsContext);

        MappingEntryImpl entry = new MappingEntryImpl("doi",
                xPath.compile("/*/gmd:identificationInfo/*/gmd:citation/*/gmd:citedResponsibleParty/*/gmd:contactInfo/*/gmd:onlineResource/*/gmd:linkage/gmd:URL/text()"),
                Collections.singletonMap("type", "string"),
                false,
                false,
                false,
                xPath.compile("/*/gmd:identificationInfo/*/gmd:citation/*/gmd:citedResponsibleParty/*/gmd:contactInfo/*/gmd:onlineResource/*/gmd:linkage/gmd:URL[text()[contains(.,'doi.org')]]"));

        Optional<EntryMapper.EvalResult> result = mapper.mapEntry(entry, node);

        Assert.assertThat(result.get(), CoreMatchers.notNullValue());
        Assert.assertThat(result.get().value, CoreMatchers.is("http://dx.doi.org/10.5676/EUM_SAF_CM/SARAH/V001"));

        //not matching condition
        entry = new MappingEntryImpl("doi",
                xPath.compile("/*/gmd:identificationInfo/*/gmd:citation/*/gmd:citedResponsibleParty/*/gmd:contactInfo/*/gmd:onlineResource/*/gmd:linkage/gmd:URL/text()"),
                Collections.singletonMap("type", "string"),
                false,
                false,
                false,
                xPath.compile("/*/gmd:identificationInfo/*/gmd:citation/*/gmd:citedResponsibleParty/*/gmd:contactInfo/*/gmd:onlineResource/*/gmd:linkage/gmd:URL[text()[contains(.,'doidoidoi.org')]]"));

        result = mapper.mapEntry(entry, node);

        Assert.assertThat(result.isPresent(), CoreMatchers.is(false));
    }

    @Test
    public void testSplitToArray() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        EntryMapper mapper = new EntryMapper();

        Node node = readNode("/records/gmd/metadata_1000.xml");

        XPathFactory xpathFactory = new XPathHelper().newXPathFactory();
        XPath xPath = xpathFactory.newXPath();
        Map<String, String> namespaceMap = new HashMap<>();

        namespaceMap.put("gmi", "http://www.isotc211.org/2005/gmi");
        namespaceMap.put("gmd", "http://www.isotc211.org/2005/gmd");
        namespaceMap.put("gco", "http://www.isotc211.org/2005/gco");
        namespaceMap.put("gml", "http://www.opengis.net/gml");
        namespaceMap.put("gmi", "http://www.eumetsat.int/2008/gmi");
        namespaceMap.put("apiso", "http://www.opengis.net/cat/csw/apiso/1.0");


        NamespaceContextImpl nsContext = new NamespaceContextImpl(namespaceMap);
        xPath.setNamespaceContext(nsContext);

        MappingEntryImpl entry = new MappingEntryImpl("coverage",
                xPath.compile("/*/gmd:identificationInfo/*/gmd:extent/*/gmd:geographicElement/*/gmd:geographicIdentifier/*/gmd:code/*/text()"),
                Collections.singletonMap("type", "string"),
                false,
                false,
                false,
                null).setSplit(",");

        Optional<EntryMapper.EvalResult> result = mapper.mapEntry(entry, node);

        Assert.assertThat(result.get(), CoreMatchers.notNullValue());
        Assert.assertThat(result.get().value, CoreMatchers.instanceOf(List.class));
        List<String> arr = (List<String>) result.get().value;
        Assert.assertThat(arr.get(0), CoreMatchers.is("Europe"));
        Assert.assertThat(arr.get(1), CoreMatchers.is("Africa"));
        Assert.assertThat(arr.get(2), CoreMatchers.is("Atlantic Ocean"));

        //node with a non-split thingy
        node = readNode("/records/gmd/metadata_1000b.xml");
        result = mapper.mapEntry(entry, node);

        Assert.assertThat(result.get(), CoreMatchers.notNullValue());
        Assert.assertThat(result.get().value, CoreMatchers.instanceOf(List.class));
        arr = (List<String>) result.get().value;
        Assert.assertThat(arr.size(), CoreMatchers.is(1));
        Assert.assertThat(arr.get(0), CoreMatchers.is("Europe"));
    }

    @Test
    public void testFullTextConcatenation() throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        EntryMapper mapper = new EntryMapper();

        Node node = readNode("/records/gmd/metadata_fulltext.xml");

        XPathFactory xpathFactory = new XPathHelper().newXPathFactory();
        XPath xPath = xpathFactory.newXPath();
        Map<String, String> namespaceMap = new HashMap<>();

        namespaceMap.put("gmi", "http://www.isotc211.org/2005/gmi");
        namespaceMap.put("gmd", "http://www.isotc211.org/2005/gmd");
        namespaceMap.put("gco", "http://www.isotc211.org/2005/gco");
        namespaceMap.put("gml", "http://www.opengis.net/gml");
        namespaceMap.put("gmi", "http://www.eumetsat.int/2008/gmi");
        namespaceMap.put("apiso", "http://www.opengis.net/cat/csw/apiso/1.0");


        NamespaceContextImpl nsContext = new NamespaceContextImpl(namespaceMap);
        xPath.setNamespaceContext(nsContext);

        MappingEntryImpl entry = new MappingEntryImpl("coverage",
                xPath.compile("normalize-space(string-join(//text() | //@*[not(starts-with(local-name(), 'xmlns')) and not(starts-with(local-name(), 'schemaLocation'))], ' '))"),
                Collections.singletonMap("type", "string"),
                false,
                false,
                false,
                null);

        Optional<EntryMapper.EvalResult> result = mapper.mapEntry(entry, node);

        Assert.assertThat(result.get(), CoreMatchers.notNullValue());
        Assert.assertThat(result.get().value, CoreMatchers.instanceOf(String.class));
        String arr = (String) result.get().value;
        Assert.assertThat(arr, CoreMatchers.containsString("EO:EUM:CM:MULT:SARAH_V001 "));
        Assert.assertThat(arr, CoreMatchers.containsString(" test-attribute "));
        Assert.assertThat(arr, CoreMatchers.not(CoreMatchers.containsString("http://www.eumetsat.int/2008/gmi")));
    }

    private Node readNode(String res) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
        Document doc = builder.newDocumentBuilder().parse(getClass().getResourceAsStream(res));
        return doc;
    }

}
