/*
 * Copyright 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.youngs.validation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Locale;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.youngs.harvest.InMemoryStreamSource;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.harvest.Source;
import org.n52.youngs.harvest.SourceException;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.ReportImpl;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class XmlSchemaValidatorTest {

    @BeforeClass
    public static void setup() {
        Locale.setDefault(Locale.ENGLISH);
    }


    @Test
    public void testValidXmlFile() throws SAXException, SourceException, IOException {
        XmlSchemaValidator val = new XmlSchemaValidator("https://dummy.schema",
                getClass().getResource("/schemas/dummy-schema.xsd"));
        Source source = new InMemoryStreamSource(getClass().getResourceAsStream("/schemas/dummy-doc1.xml"));
        Collection<SourceRecord> records = source.getRecords(new ReportImpl());
        NodeSourceRecord record1 = (NodeSourceRecord) records.iterator().next();

        Assert.assertTrue(val.matchesNamespace(record1.getRecord().getNamespaceURI()));
        Assert.assertTrue(val.validate(record1.getRecord()).isEmpty());
    }

    @Test
    public void testInvalidXmlFile() throws SourceException, IOException, SAXException {
        XmlSchemaValidator val = new XmlSchemaValidator("https://dummy.schema",
                getClass().getResource("/schemas/dummy-schema.xsd"));
        Source source = new InMemoryStreamSource(getClass().getResourceAsStream("/schemas/dummy-doc2_invalid.xml"));
        Collection<SourceRecord> records = source.getRecords(new ReportImpl());
        NodeSourceRecord record1 = (NodeSourceRecord) records.iterator().next();

        Assert.assertTrue(val.matchesNamespace(record1.getRecord().getNamespaceURI()));
        try {
            Assert.assertTrue(val.validate(record1.getRecord()).isEmpty());
            // we should not get here
            Assert.assertThat("No exception was thrown", CoreMatchers.equalTo(""));
        } catch (SAXException ex) {
            Assert.assertThat(ex.getMessage(), CoreMatchers.containsString(" is not a valid value for "));
        }
    }


    @Test
    public void testMultiSchemaXmlFile() throws SAXException, SourceException, IOException {
        XmlSchemaValidator val;
        try {
            val = new XmlSchemaValidator("http://www.isotc211.org/2005/gmi",
                    Paths.get(getClass().getResource("/schemas/gmi/gmi.xsd").toURI()).toFile()
            );
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        Source source = new InMemoryStreamSource(getClass().getResourceAsStream("/schemas/complex_doc.xml"));
        Collection<SourceRecord> records = source.getRecords(new ReportImpl());
        NodeSourceRecord record1 = (NodeSourceRecord) records.iterator().next();

        Assert.assertTrue(val.matchesNamespace(record1.getRecord().getNamespaceURI()));
        Assert.assertTrue(val.validate(record1.getRecord()).isEmpty());
    }

}
