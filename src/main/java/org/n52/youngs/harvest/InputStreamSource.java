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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.n52.youngs.api.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public abstract class InputStreamSource implements Source {

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamSource.class.getName());
    private final DocumentBuilderFactory docBuilderFactory;

    public InputStreamSource() {
        this.docBuilderFactory = DocumentBuilderFactory.newInstance();
        this.docBuilderFactory.setNamespaceAware(true);
    }

    @Override
    public long getRecordCount() {
        return 1;
    }

    @Override
    public Collection<SourceRecord> getRecords(Report report) {

        try {
            DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();

            Document doc = documentBuilder.parse(new InputSource(new InputStreamReader(resolveSourceInputStream(), sourceCharset())));

            Element elem = doc.getDocumentElement();
            elem.normalize();
            LOG.trace("Read document: {}", elem);

            NodeSourceRecord record = new NodeSourceRecord(elem);
            return Collections.singletonList(record);
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            LOG.warn("Could not read file:" + ex.getMessage(), ex);
        }

        return Collections.emptyList();
    }

    protected abstract InputStream resolveSourceInputStream() throws IOException;

    protected Charset sourceCharset() {
        Charset cs = Charset.forName("utf-8");
        return cs;
    }

    @Override
    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report) {
        return getRecords(report);
    }

}
