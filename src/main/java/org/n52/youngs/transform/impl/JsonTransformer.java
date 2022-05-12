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
package org.n52.youngs.transform.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Xslt30Transformer;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.trans.XPathException;

public class JsonTransformer {

    private static final Logger log = LoggerFactory.getLogger(JsonTransformer.class);

    // uses a stream to feed the JSON to the XSLT
    // uses the Saxon API to call a named template of the stylesheet
    public String transformWithStream(String jsonImput) throws SaxonApiException, XmlException {
        log.trace("Transform with stream: {}", jsonImput);
        log.trace("------------------------- \n");
        Processor processor = new Processor(false);

        // configure custom resolver to load the JSON stream
        processor.getUnderlyingConfiguration().setUnparsedTextURIResolver(new UnparsedTextURIResolver() {
            @Override
            public Reader resolve(URI uri, String s, Configuration configuration) throws XPathException {
                // feed your stream to the transformation
                return new InputStreamReader(new ByteArrayInputStream(jsonImput.getBytes()),
                        StandardCharsets.UTF_8);
            }
        });

        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable stylesheet = compiler.compile(new StreamSource(getClass().getClassLoader().getResourceAsStream("xslt/pn-json/stream.xsl")));
        Xslt30Transformer transformer = stylesheet.load30();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // call named template "xsl:initial-template" in the xsl file
        transformer.callTemplate(new QName("http://www.w3.org/1999/XSL/Transform", "initial-template"),
                processor.newSerializer(byteArrayOutputStream));
        return byteArrayOutputStream.toString();
    }

}
