/*
 * Copyright 2015-2022 52°North Initiative for Geospatial Open Source
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

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.opengis.csw.v_2_0_2.AbstractRecordType;
import net.opengis.csw.v_2_0_2.GetRecordsResponseType;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.impl.NamespaceContextImpl;

/**
 * http://www.ogcnetwork.net/jaxb4ogc
 *
 * http://confluence.highsource.org/display/OGCS/Reference
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class Jaxb {

    private Unmarshaller unmarshaller;

    private JAXBContext context;

    private ByteSource getRecordsResponse;

    @Before
    public void createUnmarshaller() throws JAXBException {
        context = JAXBContext.newInstance("net.opengis.csw.v_2_0_2");
        unmarshaller = context.createUnmarshaller();
    }

    @Before
    public void loadResponse() throws IOException {
        getRecordsResponse = Resources.asByteSource(Resources.getResource("responses/dab-records-iso.xml"));
    }

    @Test
    public void basicUnmarshal() throws JAXBException, IOException {
        JAXBElement<GetRecordsResponseType> unmarshalled = unmarshaller.unmarshal(
                new StreamSource(getRecordsResponse.openBufferedStream()), GetRecordsResponseType.class);

        GetRecordsResponseType value = unmarshalled.getValue();

        assertThat("response version is 2.0.2", value.getVersion(), is(equalTo("2.0.2")));
        assertThat("10 records are returned", value.getSearchResults().getNumberOfRecordsReturned().intValue(),
                is(equalTo(10)));
    }

    @Test
    public void xPath() throws MalformedURLException, JAXBException, IOException, XPathExpressionException {
        JAXBElement<GetRecordsResponseType> unmarshalled = unmarshaller.unmarshal(
                new StreamSource(getRecordsResponse.openBufferedStream()), GetRecordsResponseType.class);

        List<JAXBElement<? extends AbstractRecordType>> abstractRecords = unmarshalled.getValue().getSearchResults().getAbstractRecord();
        assertThat("abstracts records is empty", abstractRecords.size(), is(equalTo(0)));
        List<Object> any = unmarshalled.getValue().getSearchResults().getAny();
        assertThat("any is 10 elements", any.size(), is(equalTo(10)));

        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        path.setNamespaceContext(NamespaceContextImpl.create());

        String value = path.evaluate("//gmd:fileIdentifier/gco:CharacterString",
                any.get(1));
        assertThat("value is not empty", value, not(is(isEmptyString())));
    }

}
