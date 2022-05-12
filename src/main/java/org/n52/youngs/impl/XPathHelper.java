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
package org.n52.youngs.impl;

import java.io.StringReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.n52.youngs.api.XPathConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * test factories for support for XPath versions and create factories in an non-invasive manner.
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class XPathHelper {

    private static final Logger log = LoggerFactory.getLogger(XPathHelper.class);

    private final String systemSettingKey;

    private final String systemSettingValue;

    private final String objectModel;

    public XPathHelper() {
        this("javax.xml.xpath.XPathFactory:"
                //+ NamespaceConstant.OBJECT_MODEL_SAXON;
                + "http://saxon.sf.net/jaxp/xpath/om",
                "net.sf.saxon.xpath.XPathFactoryImpl",
                "http://saxon.sf.net/jaxp/xpath/om");
    }

    public XPathHelper(String systemSettingKey, String systemSettingValue, String objectModel) {
        this.systemSettingKey = systemSettingKey;
        this.systemSettingValue = systemSettingValue;
        this.objectModel = objectModel;
    }

    private void setSystemSetting() {
        System.setProperty(systemSettingKey, systemSettingValue);
        log.info("Setting system property {} to {}", systemSettingKey, systemSettingValue);
    }

    private void unsetSystemSetting() {
        System.clearProperty(systemSettingKey);
        log.info("Unsetting system property {}", systemSettingKey);
    }

    public synchronized XPathFactory newXPathFactory() {
        setSystemSetting();
        XPathFactory factory = null;
        try {
            factory = XPathFactory.newInstance(objectModel);
            //NamespaceConstant.OBJECT_MODEL_SAXON);
        } catch (XPathFactoryConfigurationException e) {
            log.error("Could not create new instance of XPathFactory", e);
        }
        unsetSystemSetting();
        return factory;
    }

    public boolean isVersionSupported(XPathFactory factory, String version) {
        switch (version) {
            case XPathConstants.XPATH_20:
                return isXPath20Supported(factory);
            case XPathConstants.XPATH_10:
                return isXPath10Supported(factory);
            default:
                throw new IllegalArgumentException(String.format("Unknown XSLT version %s", version));
        }
    }

    /**
     * http://docs.oracle.com/javase/8/docs/api/javax/xml/xpath/XPathFactory.html
     * @param factory the factory to be checked
     * @return always true
     */
    public boolean isXPath10Supported(XPathFactory factory) {
        return true;
    }

    /**
     * see http://stackoverflow.com/questions/7951879/which-version-of-xpath-and-xslt-am-i-using and http://stackoverflow.com/questions/926222/using-saxon-xpath-engine-in-java
     *
     * @param factory the factory to be checked
     * @return true if the provided factory supports XPath 2.0
     */
    public boolean isXPath20Supported(XPathFactory factory) {
        try {
            XPath xpath = factory.newXPath();
            @SuppressWarnings("MalformedXPath") // is in fact not malformed but recognized by SAXON
            String evaluate = xpath.evaluate("current-date()", new InputSource(
                    new StringReader("<empty />")));
            log.trace("Evaluated current-date() to {} as a quick check for XSLT 2.0 support", evaluate);
            return !evaluate.isEmpty();
        } catch (XPathExpressionException e) {
            log.debug("Error evaluating test expression, 2.0 not supported.", e.getMessage());
            return false;
        }
    }

}
