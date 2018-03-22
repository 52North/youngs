/*
 * Copyright 2015-2018 52°North Initiative for Geospatial Open Source
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
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.n52.youngs.exception.MappingError;
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
        config = new YamlMappingConfiguration("mappings/testmapping.yml", helper);
    }

    @Test
    public void entryByName() throws IOException {
        assertThat("name is correct", config.getEntry("title").getFieldName(), is(equalTo("title")));
        assertThat("name is correct", config.getEntry("title").getIndexProperties().size(), is(equalTo(4)));
    }

    @Test
    public void testConfigMetadata() throws IOException {
        assertThat("name is correct", config.getName(), is(equalTo("test")));
        assertThat("version is correct", config.getVersion(), is(equalTo(42)));
        assertThat("XPath version is correct", config.getXPathVersion(), is(equalTo("2.0")));
    }

    @Test
    public void testRawXmlMetadata() throws IOException {
        assertThat("one raw field is found", config.getEntries().stream().filter(e -> e.isRawXml()).count(), is(1l));

        MappingEntry rawEntry = config.getEntries().stream().filter(e -> e.isRawXml()).findFirst().get();
        assertThat("one raw field is found", rawEntry.getFieldName(), is("raw_xml"));
    }

    @Test
    public void testAnalyzedField() throws IOException {
        assertThat("two not analyzed fields are found", config.getEntries().stream().filter(e -> !e.isAnalyzed()).count(), is(2l));
        assertThat("two not analyzed fields are found", config.getEntries().stream().filter(e -> e.isAnalyzed()).count(), is(5l));

        List<MappingEntry> notAnalyzedEntries = config.getEntries().stream().filter(e -> !e.isAnalyzed()).collect(Collectors.toList());
        assertThat("raw_xml field is found", notAnalyzedEntries.stream()
                .filter(e -> e.getFieldName().equals("raw_xml")).count(), is(1l));
        assertThat("language field is found", notAnalyzedEntries.stream()
                .filter(e -> e.getFieldName().equals("language")).count(), is(1l));
    }

    @Test
    public void testDefaultAnalyzed() throws Exception {
        Collection<MappingEntry> entries = config.getEntries();

        List<String> notAnalyzedFields = entries.stream().filter(e -> {
            return !e.isAnalyzed();
        }).map(MappingEntry::getFieldName).collect(Collectors.toList());
        assertThat(notAnalyzedFields.size(), is(2));
        assertThat(notAnalyzedFields, CoreMatchers.hasItems("raw_xml", "language"));
    }

    @Test
    public void testConfigMetadataVersionParsing() throws IOException {
        YamlMappingConfiguration c = new YamlMappingConfiguration("mappings/testmapping-double-version-number.yml",
                helper);
        assertThat("version is correct", c.getVersion(), is(equalTo(2)));
    }

    @Test
    public void testConfigMetadataVersionString() throws IOException {
        YamlMappingConfiguration c = new YamlMappingConfiguration("mappings/testmapping-string-version-number.yml",
                helper);
        assertThat("version is correct", c.getVersion(), is(equalTo(1)));
    }

    @Test
    public void testDefaultMetadata() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-empty.yml",
                helper);

        assertThat("name is correct", otherConfig.getName(), is(equalTo(MappingConfiguration.DEFAULT_NAME)));
        assertThat("version is correct", otherConfig.getVersion(), is(equalTo(MappingConfiguration.DEFAULT_VERSION)));
        assertThat("XPath version is correct", otherConfig.getXPathVersion(), is(equalTo(MappingConfiguration.DEFAULT_XPATH_VERSION)));
    }

    @Test
    public void testDefaultFieldType() throws Exception {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        assertThat("date entry field type", iter.next().getIndexPropery("type"), is(equalTo("date")));
        assertThat("id entry field type", iter.next().getIndexPropery("type"), is(equalTo("text")));
        assertThat("language entry field type", iter.next().getIndexPropery("type"), is(equalTo("text")));
        assertThat("title entry field type", iter.next().getIndexPropery("type"), is(equalTo("text")));
        assertThat("xtitle entry field type", iter.next().getIndexPropery("type"), is(equalTo("text")));
    }

    @Test
    public void testApplicabilityMatching() throws Exception {
        Document document = getDocument("<testdoc xmlns=\"http://www.isotc211.org/2005/gmd\">"
                + "<MD_Metadata>"
                + "<id>42</id>"
                + "</MD_Metadata>"
                + "</testdoc>");
        assertThat("matching document is applicable", config.isApplicable(document), is(true));
    }

    @Ignore
    @Test
    public void testApplicabilityNonMatchingElem() throws Exception {
        Document document = getDocument("<testdoc xmlns=\"http://www.isotc211.org/2005/gmd\">"
                + "<MI_Metadata>"
                + "<id>42</id>"
                + "</MI_Metadata>"
                + "</testdoc>");
        assertThat("non-matching element is not applicable", config.isApplicable(document), is(false));
    }

    @Ignore
    @Test
    public void testApplicabilityNonMatchingNS() throws Exception {
        Document document = getDocument("<testdoc>"
                + "<ns1:MD_Metadata xmlns:ns1=\"http://wrong.namespace\">"
                + "<id>42</id>"
                + "</ns1:MD_Metadata>"
                + "</testdoc>");
        assertThat("non-matching namespace is not applicable", config.isApplicable(document), is(false));
    }

    @Test
    public void testApplicabilityInvalidPath() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-invalidXPath.yml", helper);

        Document document = getDocument("<testdoc" + new Random().nextInt(52) + " />");

        assertThat("matching document is always applicable", otherConfig.isApplicable(document), is(true));
    }

    @Test
    public void testApplicabilityMissing() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-empty.yml", helper);

        Document document = getDocument("<testdoc/>");

        assertThat("is always applicable", otherConfig.isApplicable(document), is(true));
        assertThat("is always applicable, but not for 'null'", otherConfig.isApplicable(null), is(false));
    }

    @Test
    public void testEntriesLoading() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        assertThat("all entries are loaded", entries.size(), is(equalTo(7)));
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
    public void testEntriesNonLocations() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        assertThat("first entry (date) is not location", iter.next().isLocation(), is(equalTo(false)));
        assertThat("second entry (id) is not location", iter.next().isLocation(), is(equalTo(false)));
        assertThat("third entry (language) is not location", iter.next().isLocation(), is(equalTo(false)));
        assertThat("fourth entry (loc) is location", iter.next().isLocation(), is(equalTo(true)));
        assertThat("fourth entry (title) is not location", iter.next().isLocation(), is(equalTo(false)));
    }

    @Test
    public void testEntryLocation() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();

        assertThat("one location field found", entries.stream().filter(e -> e.isLocation()).count(), is(1l));
        assertThat("the one location field has correct name", entries.stream()
                .filter(e -> e.isLocation()).findFirst().get().getFieldName(), is("loc"));
    }

    @Test
    public void testEntriesProperties() throws IOException, XPathExpressionException {
        Collection<MappingEntry> entries = config.getEntries();
        Iterator<MappingEntry> iter = entries.iterator();
        iter.next();
        MappingEntry second = iter.next();
        Map<String, Object> props = second.getIndexProperties();
        assertThat("id entry index properties size", props.size(), is(equalTo(4)));
        assertThat("id entry property type", props.get("type"), is(equalTo("text")));
        assertThat("id entry property type", props.get("store"), is(equalTo(true)));
        assertThat("id entry property type", props.get("index"), is(equalTo(true)));
        assertThat("id entry property type", props.get("boost"), is(equalTo(2d)));
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
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-no-namespaces.yml", helper);
        assertNotNull(m);
    }

    @Test(expected = MappingError.class)
    public void testMultipleIdentifiers() throws Exception {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-multiple-ids.yml", helper);
        assertNotNull(m);
    }

    @Test(expected = MappingError.class)
    public void testMissingIdentifier() throws Exception {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-missing-id.yml", helper);
        assertNotNull(m);
    }

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
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-missing-ns.yml", helper);
        assertNotNull(m);
    }

    @Test
    public void testXpath10VersionSupportedJava() throws XPathExpressionException, IOException, XPathFactoryConfigurationException {
        YamlMappingConfiguration c10 = new YamlMappingConfiguration("mappings/testmapping-xpath10.yml", new XPathHelperTest10());
        assertThat("mapping configuration was loaded", c10.getName(), is("testxpath"));
    }

    @Test(expected = MappingError.class)
    @SuppressWarnings("ResultOfObjectAllocationIgnored")
    public void testXpath20VersionNOTSupportedJava() throws XPathExpressionException, IOException, XPathFactoryConfigurationException {
        new YamlMappingConfiguration("mappings/testmapping-xpath20.yml", new XPathHelperTest10());
    }

    @Test
    public void testXpath20VersionSupportedWithHelper() throws XPathExpressionException, IOException, XPathFactoryConfigurationException {
        YamlMappingConfiguration c10 = new YamlMappingConfiguration("mappings/testmapping-xpath20.yml", helper);
        assertThat("mapping configuration was loaded", c10.getName(), is("testxpath"));
    }

    @Test
    public void testEnvelope() throws Exception {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-gmd-bbox.yml", helper);

        MappingEntry bbox = m.getEntries().stream()
                .filter(e -> e.getFieldName().equals("location")).findFirst().get();
        assertThat("has coords", bbox.hasCoordinates(), is(true));
        assertThat("has coords", bbox.hasCoordinatesType(), is(true));
        assertThat("envelope field name", bbox.getFieldName(), is("location"));
        assertThat("envelope type", bbox.getIndexPropery("type"), is("geo_shape"));
        assertThat("envelope type", bbox.getCoordinatesType(), is("envelope"));
//        assertThat("coords are correctly parsed", bbox.getCoordinates(),
//                allOf(containsString("concat('[ ['"), containsString("normalize-space(gmd:northBoundLatitude),")));
        String bboxString = "<gmd:EX_GeographicBoundingBox xmlns:gmd=\"http://www.isotc211.org/2005/gmd\" xmlns:gco=\"http://www.isotc211.org/2005/gco\">"
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
                + "    <gco:Decimal>14.0</gco:Decimal>"
                + "</gmd:northBoundLatitude>"
                + "</gmd:EX_GeographicBoundingBox>";

        List<XPathExpression[]> coordinatesXPaths = bbox.getCoordinatesXPaths();
        assertThat("coordinates entry xpath works", coordinatesXPaths.get(0)[0].evaluate(new InputSource(new StringReader(bboxString)), XPathConstants.NUMBER), is(14d));
        assertThat("coordinates entry xpath works", coordinatesXPaths.get(0)[1].evaluate(new InputSource(new StringReader(bboxString)), XPathConstants.NUMBER), is(-11d));
        assertThat("coordinates entry xpath works", coordinatesXPaths.get(1)[0].evaluate(new InputSource(new StringReader(bboxString)), XPathConstants.NUMBER), is(-13d));
        assertThat("coordinates entry xpath works", coordinatesXPaths.get(1)[1].evaluate(new InputSource(new StringReader(bboxString)), XPathConstants.NUMBER), is(12d));
        //is("[ [14, -11], [-13, 12] ]"));
    }

    @Test
    public void testReplacement() throws IOException {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-replacement.yml", helper);
        assertThat("replacement field is found", m.getEntries().stream().filter(e -> e.hasReplacements()).count(), is(1l));

        MappingEntry entry = m.getEntries().stream().filter(e -> e.hasReplacements()).findFirst().get();
        assertThat("fieldname is correct", entry.getFieldName(), is("replacer"));
        assertThat("two replacements found", entry.getReplacements().size(), is(2));
        assertThat("first key is correct", entry.getReplacements().keySet().iterator().next(), is("."));
        assertThat("first value is correct", entry.getReplacements().values().iterator().next(), is(","));
    }

    @Test
    public void testSplit() throws IOException {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-split.yml", helper);
        assertThat("split field is found", m.getEntries().stream().filter(e -> e.hasSplit()).count(), is(1l));

        MappingEntry entry = m.getEntries().stream().filter(e -> e.hasSplit()).findFirst().get();
        assertThat("fieldname is correct", entry.getFieldName(), is("splitter"));
        assertThat("split field value is correct", entry.getSplit(), is("__split__"));
    }

    @Test
    public void testOutputProperties() throws IOException {
        YamlMappingConfiguration m = new YamlMappingConfiguration("mappings/testmapping-raw-outputproperties.yml", helper);
        assertThat("outputproperties field is found", m.getEntries().stream().filter(e -> e.hasOutputProperties()).count(), is(1l));

        MappingEntry entry = m.getEntries().stream().filter(e -> e.hasOutputProperties()).findFirst().get();
        assertThat("fieldname is correct", entry.getFieldName(), is("raw_xml"));
        assertThat("two properties found", entry.getOutputProperties().size(), is(2));
        assertThat("has omit", entry.getOutputProperties().keySet().contains("omit-xml-declaration"), is(true));
        assertThat("omit value is correct", entry.getOutputProperties().get("omit-xml-declaration"), is("yes"));
        assertThat("has indent", entry.getOutputProperties().keySet().contains("indent"), is(true));
        assertThat("indent value is correct", entry.getOutputProperties().get("indent"), is("no"));
    }

    @Test
    public void testIdentifierField() throws IOException, XPathExpressionException {
        String idField = config.getIdentifierField();
        assertThat("id field is set", idField, is(equalTo("id")));
    }

    private Document getDocument(String xmlString) throws Exception {
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlString));
        Document doc = db.parse(is);
        return doc;
    }

    private static class XPathHelperTest10 extends XPathHelper {

        public XPathHelperTest10() {
            //
        }

        @Override
        public synchronized XPathFactory newXPathFactory() {
            return XPathFactory.newInstance();
        }

    }

}
