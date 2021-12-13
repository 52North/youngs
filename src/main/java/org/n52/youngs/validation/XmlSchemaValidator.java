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
package org.n52.youngs.validation;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class XmlSchemaValidator {

    private static final Logger LOG = LoggerFactory.getLogger(XmlSchemaValidator.class.getName());

    private final Schema schema;
    private final String namespace;
    private Validator validator;

    /**
     * Creates a new instance of a validator for the given namespace with the
     * schema resource.
     *
     * If there are schema imports, this validator instance might have issues, depending
     * on the underlying implementation. Some cannot resolve imports when URLs
     * are used to create the schema source.
     *
     * @param namespace the target namespace
     * @param schemaResources the resources
     * @throws SAXException when the schema cannot be processed
     */
    public XmlSchemaValidator(String namespace, URL... schemaResources) throws SAXException {
        Objects.nonNull(schemaResources);
        Objects.nonNull(namespace);
        this.namespace = namespace;
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schema = schemaFactory.newSchema(convertToSources(schemaResources));
        this.initValidator();
    }

    /**
     * Creates a new instance of a validator for the given namespace with the
     * schema resource.
     *
     * @param namespace the target namespace
     * @param schemaFiles the resources as files
     * @throws SAXException when the schema cannot be processed
     */
    public XmlSchemaValidator(String namespace, File... schemaFiles) throws SAXException {
        Objects.nonNull(schemaFiles);
        Objects.nonNull(namespace);
        this.namespace = namespace;
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        this.schema = schemaFactory.newSchema(convertToSources(schemaFiles));
        this.initValidator();
    }

    private void initValidator() {
        this.validator = schema.newValidator();
        String validationFeature = "http://xml.org/sax/features/validation";
        String schemaFeature = "http://apache.org/xml/features/validation/schema";

        try {
            this.validator.setFeature(validationFeature, true);
            this.validator.setFeature(schemaFeature, true);
        } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
            LOG.warn("Could not enabled specific validation feature: " + ex.getMessage());
            LOG.debug(ex.getMessage(), ex);
        }

    }

    public boolean matchesNamespace(String uri) {
        return this.namespace.endsWith(uri);
    }

    public List<String> validate(Node xmlFile) throws SAXException, IOException {
        LocalErrorHandler eh = new LocalErrorHandler();
        this.validator.setErrorHandler(eh);
        validator.validate(new DOMSource(xmlFile));

        // if we have not errors or fatals, return the warnings (not breaking but informative)
        if (eh.errors.isEmpty() && eh.fatalErrors.isEmpty()) {
            return eh.warnings.stream()
                    .map(e -> e.getMessage())
                    .collect(Collectors.toList());
        }

        // this matches if fatal errors is empty --> throw the first error
        if (eh.fatalErrors.isEmpty()) {
            SAXParseException e = eh.errors.get(0);
            throw new SAXException(e.getMessage(), e);
        }

        // otherwise throw the fatal error
        SAXParseException e = eh.fatalErrors.get(0);
        throw new SAXException(e.getMessage(), e);
    }

    private Source[] convertToSources(URL[] schemaResources) throws SAXException {
        List<IOException> exceptions = Lists.newArrayList();
        StreamSource[] result = Arrays.asList(schemaResources).stream()
                .map(sr -> {
                    try {
                        return new StreamSource(sr.openStream());
                    } catch (IOException ex) {
                        exceptions.add(ex);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
                .toArray(new StreamSource[0]);
        if (!exceptions.isEmpty()) {
            throw new SAXException(exceptions.get(0));
        }
        return result;
    }

    private Source[] convertToSources(File[] schemaResources) throws SAXException {
        Source[] result = Arrays.asList(schemaResources).stream()
                .map(sr -> new StreamSource(sr))
                .collect(Collectors.toList())
                .toArray(new StreamSource[0]);
        return result;
    }

    private class LocalErrorHandler implements ErrorHandler {

        private final List<SAXParseException> warnings = Lists.newArrayList();
        private final List<SAXParseException> errors = Lists.newArrayList();
        private final List<SAXParseException> fatalErrors = Lists.newArrayList();

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            this.warnings.add(exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            this.errors.add(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            this.fatalErrors.add(exception);
        }
    }

}
