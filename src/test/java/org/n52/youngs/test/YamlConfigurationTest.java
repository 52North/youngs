/*
 * Copyright 2015-2015 52°North Initiative for Geospatial Open Source
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

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.exception.MappingError;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class YamlConfigurationTest {

    private YamlMappingConfiguration config;

    private final XPathHelper helper = new XPathHelper();

    @Before
    public void loadFile() throws IOException {
        config = new YamlMappingConfiguration("mappings/testmapping.yml", helper.newXPathFactory());
    }

    @Test
    public void testConfigMetadata() throws IOException {
        assertThat("name is correct", config.getName(), is(equalTo("test")));
        assertThat("version is correct", config.getVersion(), is(equalTo(42)));
        assertThat("XPath version is correct", config.getXPathVersion(), is(equalTo("2.0")));
    }

    @Test
    public void testConfigMetadataVersionParsing() throws IOException {
        YamlMappingConfiguration c = new YamlMappingConfiguration("mappings/testmapping-double-version-number.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());
        assertThat("version is correct", c.getVersion(), is(equalTo(2)));
    }

    @Test
    public void testConfigMetadataVersionString() throws IOException {
        YamlMappingConfiguration c = new YamlMappingConfiguration("mappings/testmapping-string-version-number.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());
        assertThat("version is correct", c.getVersion(), is(equalTo(1)));
    }

    @Test
    public void testDefaultMetadata() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-empty.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());

        assertThat("name is correct", otherConfig.getName(), is(equalTo(MappingConfiguration.DEFAULT_NAME)));
        assertThat("version is correct", otherConfig.getVersion(), is(equalTo(MappingConfiguration.DEFAULT_VERSION)));
        assertThat("XPath version is correct", otherConfig.getXPathVersion(), is(equalTo(MappingConfiguration.DEFAULT_XPATH_VERSION)));
    }

    @Test
    public void testDefaultFieldType() throws Exception {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        assertThat("date entry field type", iter.next().getIndexPropery("type"), is(equalTo("date")));
        assertThat("id entry field type", iter.next().getIndexPropery("type"), is(equalTo("string")));
        assertThat("language entry field type", iter.next().getIndexPropery("type"), is(equalTo("string")));
        assertThat("title entry field type", iter.next().getIndexPropery("type"), is(equalTo("string")));
        assertThat("xtitle entry field type", iter.next().getIndexPropery("type"), is(equalTo("string")));
    }

    @Test
    public void testApplicabilityMatching() throws Exception {
        Document document = Util.getDocument("<testdoc xmlns=\"http://www.isotc211.org/2005/gmd\">"
                + "<MD_Metadata>"
                + "<id>42</id>"
                + "</MD_Metadata>"
                + "</testdoc>");
        assertThat("matching document is applicable", config.isApplicable(document), is(true));
    }

//    @Test // FIXME
    public void testApplicabilityNonMatchingElem() throws Exception {
        Document document = Util.getDocument("<testdoc xmlns=\"http://www.isotc211.org/2005/gmd\">"
                + "<MI_Metadata>"
                + "<id>42</id>"
                + "</MI_Metadata>"
                + "</testdoc>");
        assertThat("non-matching element is not applicable", config.isApplicable(document), is(false));
    }

//    @Test // FIXME
    public void testApplicabilityNonMatchingNS() throws Exception {
        Document document = Util.getDocument("<testdoc>"
                + "<ns1:MD_Metadata xmlns:ns1=\"http://wrong.namespace\">"
                + "<id>42</id>"
                + "</ns1:MD_Metadata>"
                + "</testdoc>");
        assertThat("non-matching namespace is not applicable", config.isApplicable(document), is(false));
    }

    @Test
    public void testApplicabilityInvalidPath() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-invalidXPath.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());

        Document document = Util.getDocument("<testdoc" + new Random().nextInt(52) + " />");

        assertThat("matching document is always applicable", otherConfig.isApplicable(document), is(true));
    }

    @Test
    public void testApplicabilityMissing() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-empty.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());

        Document document = Util.getDocument("<testdoc/>");

        assertThat("is always applicable", otherConfig.isApplicable(document), is(true));
        assertThat("is always applicable, but not for 'null'", otherConfig.isApplicable(null), is(false));
    }

    @Test
    public void testEntriesLoading() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        assertThat("all entries are loaded", entries.size(), is(equalTo(5)));
    }

    @Test
    public void testEntriesFieldname() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        assertThat("first entry fieldname", iter.next().getFieldName(), is(equalTo("date")));
        assertThat("second entry fieldname", iter.next().getFieldName(), is(equalTo("id")));
        assertThat("third entry fieldname", iter.next().getFieldName(), is(equalTo("language")));
        assertThat("fourth entry fieldname", iter.next().getFieldName(), is(equalTo("title")));
    }

    @Test
    public void testEntriesIdentifier() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        assertThat("first entry (date) is not identifier", iter.next().isIdentifier(), is(equalTo(false)));
        assertThat("second entry (id) is identifier", iter.next().isIdentifier(), is(equalTo(true)));
        assertThat("third entry (language) is not identifier", iter.next().isIdentifier(), is(equalTo(false)));
        assertThat("fourth entry (title) is not identifier", iter.next().isIdentifier(), is(equalTo(false)));
    }

    @Test
    public void testEntriesProperties() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        iter.next();
        MappingEntry second = iter.next();
        assertThat("id entry isoqueryable", second.isIsoQueryable(), is(equalTo(false)));
        Map<String, Object> props = second.getIndexProperties();
        assertThat("id entry index properties size", props.size(), is(equalTo(5)));
        assertThat("id entry property type", props.get("type"), is(equalTo("string")));
        assertThat("id entry property type", props.get("store"), is(equalTo(true)));
        assertThat("id entry property type", props.get("index"), is(equalTo("analyzed")));
        assertThat("id entry property type", props.get("boost"), is(equalTo(2d)));

        MappingEntry third = iter.next();
        assertThat("third entry isoqueryable", third.isIsoQueryable(), is(equalTo(true)));
        assertThat("third entry isoqueryable name", third.getIsoQueryableName(), is(equalTo("language")));
    }

    @Test
    public void testEntriesXpaths() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        iter.next();
        MappingEntry second = iter.next();
        assertThat("id entry xpath", second.getXPath().evaluate(new InputSource(
                new StringReader("<gmd:fileIdentifier xmlns:gmd=\"http://www.isotc211.org/2005/gmd\">testid</gmd:fileIdentifier>"))), is("testid"));

        MappingEntry third = iter.next();
        assertThat("id entry xpath", third.getXPath().evaluate(new InputSource(
                new StringReader("<gmi:MI_Metadata xmlns:gmi=\"http://www.isotc211.org/2005/gmi\" xmlns=\"http://www.isotc211.org/2005/gmd\"><language>testlang</language></gmi:MI_Metadata>"))), is("testlang"));
    }

    @Test(expected = MappingError.class)
    public void testMissingNamespaces() throws Exception {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-empty.yml", helper.newXPathFactory());
        assertNotNull(m);
    }

    @Test(expected = MappingError.class)
    public void testMultipleIdentifiers() throws Exception {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-multiple-ids.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());
        assertNotNull(m);
    }

//    @Test(expected = MappingError.class)
//    public void testNoIdentifier() throws Exception {
//        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-no-id.yml",
//                NamespaceContextImpl.create(), helper.newXPathFactory());
//        assertNotNull(m);
//    }
    @Test
    public void testIndexSettings() throws IOException {
        assertThat("name is correct", config.getIndex(), is("testindex"));
        assertThat("type is correct", config.getType(), is("testrecord"));
        assertThat("create is true", config.isIndexCreationEnabled(), is(true));
        assertThat("dynamic mapping false", config.isDynamicMappingEnabled(), is(false));
        assertThat("index request is provided", config.hasIndexCreationRequest(), is(true));
        assertThat("index request correct", config.getIndexCreationRequest(), allOf(
                containsString("number_of_shards: 1"),
                containsString("number_of_replicas: 1")));
    }

    @Test(expected = MappingError.class)
    public void testEmbeddedNamespaceContextMissingNS() throws IOException {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-missing-ns.yml", helper.newXPathFactory());
        assertNotNull(m);
    }

    @Test
    public void testXpath10VersionSupportedJava() throws XPathExpressionException, IOException, XPathFactoryConfigurationException {
        YamlMappingConfiguration c10 = new YamlMappingConfiguration("mappings/testmapping-xpath10.yml",
                NamespaceContextImpl.create(), XPathFactory.newInstance());
        assertThat("mapping configuration was loaded", c10.getName(), is("testxpath"));
    }

    @Test(expected = MappingError.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testXpath20VersionNOTSupportedJava() throws XPathExpressionException, IOException, XPathFactoryConfigurationException {
        new YamlMappingConfiguration("mappings/testmapping-xpath20.yml",
                NamespaceContextImpl.create(), XPathFactory.newInstance());
    }

    @Test
    public void testXpath20VersionSupportedWithHelper() throws XPathExpressionException, IOException, XPathFactoryConfigurationException {
        YamlMappingConfiguration c10 = new YamlMappingConfiguration("mappings/testmapping-xpath20.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());
        assertThat("mapping configuration was loaded", c10.getName(), is("testxpath"));
    }

    @Test
    public void testEnvelope() throws Exception {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-gmd-bbox.yml",
                NamespaceContextImpl.create(), helper.newXPathFactory());

        Iterator<MappingEntry> iter = m.getEntries().iterator();
        iter.next();
        iter.next();
        MappingEntry bbox = iter.next();
        assertThat("has coords", bbox.hasCoordinates(), is(true));
        assertThat("has coords", bbox.hasCoordinatesType(), is(true));
        assertThat("envelope field name", bbox.getFieldName(), is("location"));
        assertThat("envelope type", bbox.getIndexPropery("type"), is("geo_shape"));
        assertThat("envelope type", bbox.getCoordinatesType(), is("envelope"));
//        assertThat("coords are correctly parsed", bbox.getCoordinates(),
//                allOf(containsString("concat('[ ['"), containsString("normalize-space(gmd:northBoundLatitude),")));
        assertThat("coordinates entry xpath works", bbox.getCoordinatesXPath().evaluate(new InputSource(
                new StringReader("<gmd:EX_GeographicBoundingBox xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:gco=\"http://www.isotc211.org/2005/gco\">"
                        + "<gmd:westBoundLongitude>"
                        + "    <gco:Decimal>-11</gco:Decimal>"
                        + "</gmd:westBoundLongitude>"
                        + "<gmd:eastBoundLongitude>"
                        + "    <gco:Decimal>12</gco:Decimal>"
                        + "</gmd:eastBoundLongitude>"
                        + "<gmd:southBoundLatitude>"
                        + "    <gco:Decimal>-13</gco:Decimal>"
                        + "</gmd:southBoundLatitude>"
                        + "<gmd:northBoundLatitude>"
                        + "    <gco:Decimal>14</gco:Decimal>"
                        + "</gmd:northBoundLatitude>"
                        + "</gmd:EX_GeographicBoundingBox>"))), is("[ [14, -11], [-13, 12] ]"));
    }

}
