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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.isotc211.x2005.gco.CharacterStringPropertyType;
import org.isotc211.x2005.gmd.AbstractMDIdentificationType;
import org.isotc211.x2005.gmd.LanguageCodeDocument;
import org.isotc211.x2005.gmd.MDIdentificationPropertyType;
import org.isotc211.x2005.gmd.MDKeywordsPropertyType;
import org.isotc211.x2005.gmd.MDKeywordsType;
import org.isotc211.x2005.gmd.MDScopeCodePropertyType;
import org.isotc211.x2005.gmi.MIMetadataDocument;
import org.isotc211.x2005.gmi.MIMetadataType;
import org.junit.Before;
import org.junit.Test;

public class JsonTransformerTest {

    public static final String CODELIST_SCOPECODE = "http://standards.iso.org/ittf/PubliclyAvailableStandards/ISO_19139_Schemas/resources/Codelist/gmxCodelists.xml#MD_ScopeCode";

    public static final String CODELIST_LANGUAGE = "http://www.w3.org/WAI/ER/IG/ert/iso639.htm";

    private MDKeywordsPropertyType[] expectedDescriptiveKeywordsArray;

    private String expectedId;

    private CharacterStringPropertyType[] expectedHierarchyLevelNameArray;

    private MDScopeCodePropertyType[] expectedHierarchyLevelArray;

    private LanguageCodeDocument expectedLanguage;

    @Before
    public void setUp() {

        XmlObject expectedXmlObject = XmlObject.Factory.newInstance();

        try {
            expectedXmlObject = XmlObject.Factory
                    .parse(getClass().getClassLoader().getResource("records/gmi/EO EUM DAT METOP IASSND02.xml"));
        } catch (XmlException | IOException e) {
            fail("Could not parse XML. " + e.getMessage());
        }

        assertTrue(
                String.format("Xmlbject not instance of MIMetadataDocument. Instead is instance of %s.",
                        expectedXmlObject.getClass().getCanonicalName()),
                expectedXmlObject instanceof MIMetadataDocument);

        MIMetadataDocument expectedMIMetadataDocument = (MIMetadataDocument) expectedXmlObject;

        MIMetadataType miMetadata = expectedMIMetadataDocument.getMIMetadata();

        assertTrue("MIMetadataType not present.", !miMetadata.isNil());

        MDIdentificationPropertyType identificationInfo = miMetadata.getIdentificationInfoArray(0);

        assertTrue("MDIdentificationPropertyType not present.", !identificationInfo.isNil());

        AbstractMDIdentificationType abstractIdentificationInfo = identificationInfo.getAbstractMDIdentification();

        assertTrue("AbstractMDIdentificationType not present.", !abstractIdentificationInfo.isNil());

        expectedDescriptiveKeywordsArray = abstractIdentificationInfo.getDescriptiveKeywordsArray();

        expectedId = miMetadata.getFileIdentifier().getCharacterString();

        expectedHierarchyLevelNameArray = miMetadata.getHierarchyLevelNameArray();

        expectedHierarchyLevelArray = miMetadata.getHierarchyLevelArray();

        try {
            expectedLanguage = LanguageCodeDocument.Factory.parse(miMetadata.getLanguage().xmlText());
        } catch (XmlException e) {
            fail("Could not parse expected LanguageCodeDocument." + e.getMessage());
        }
    }

    @Test
    public void testTransformJson() {

        StringWriter writer = new StringWriter();
        String encoding = StandardCharsets.UTF_8.name();
        InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream("records/json/EO EUM DAT METOP IASSND02.json");
        try {
            IOUtils.copy(inputStream, writer, encoding);
        } catch (IOException e) {
            fail("Could not read redord json.");
        }

        XmlObject xmlObject = XmlObject.Factory.newInstance();

        try {
            xmlObject = XmlObject.Factory.parse(new JsonTransformer().transformWithStream(writer.toString()));
            assertTrue(xmlObject != null);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertTrue(String.format("Transformed object not instance of MIMetadataDocument. Is instance of %s.",
                xmlObject.getClass().getCanonicalName()), xmlObject instanceof MIMetadataDocument);

        MIMetadataDocument miMetadataDocument = (MIMetadataDocument) xmlObject;

        MIMetadataType miMetadata = miMetadataDocument.getMIMetadata();

        assertTrue("MIMetadataType not present.", !miMetadata.isNil());

        MDIdentificationPropertyType identificationInfo = miMetadata.getIdentificationInfoArray(0);

        assertTrue("MDIdentificationPropertyType not present.", !identificationInfo.isNil());

        AbstractMDIdentificationType abstractIdentificationInfo = identificationInfo.getAbstractMDIdentification();

        assertTrue("AbstractMDIdentificationType not present.", !abstractIdentificationInfo.isNil());

        MDKeywordsPropertyType[] descriptiveKeywordsArray = abstractIdentificationInfo.getDescriptiveKeywordsArray();

        assertTrue("MDKeywordsPropertyType not present.", descriptiveKeywordsArray != null);

        checkMDKeywordsPropertyTypeArray(descriptiveKeywordsArray);

        checkId(miMetadata);

        CharacterStringPropertyType[] actualHierarchyLevelNameArray = miMetadata.getHierarchyLevelNameArray();

        assertTrue("HierarchyLevelNameArray not present.", actualHierarchyLevelNameArray != null);

        assertTrue("HierarchyLevelNameArray empty", actualHierarchyLevelNameArray.length > 0);

        checkHierarchyLevelName(actualHierarchyLevelNameArray);

        MDScopeCodePropertyType[] mappedHierarchyLevelArray = miMetadata.getHierarchyLevelArray();

        assertTrue("HierarchyLevelArray not present.", mappedHierarchyLevelArray != null);

        assertTrue("HierarchyLevelArray empty", mappedHierarchyLevelArray.length > 0);

        checkHierarchyLevel(mappedHierarchyLevelArray);

        checkLanguage(miMetadata);

    }

    private void checkLanguage(MIMetadataType miMetadata) {
        try {
            LanguageCodeDocument mappedLanguage =
                    LanguageCodeDocument.Factory.parse(miMetadata.getLanguage().xmlText());
            String mappedLanguageCodeListString = mappedLanguage.getLanguageCode().getCodeList();
            String mappedLanguageCodeListValueString = mappedLanguage.getLanguageCode().getCodeListValue();
            String expectedLanguageCodeListValueString = expectedLanguage.getLanguageCode().getCodeListValue();
            assertTrue(
                    String.format("Language CodeList not as expected. Expected %s, got %s",
                            CODELIST_LANGUAGE, mappedLanguageCodeListString),
                    CODELIST_LANGUAGE.equals(mappedLanguageCodeListString));
            assertTrue(
                    String.format("Language CodeListValue not as expected. Expected %s, got %s",
                            expectedLanguageCodeListValueString, mappedLanguageCodeListValueString),
                    expectedLanguageCodeListValueString.equals(mappedLanguageCodeListValueString));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void checkHierarchyLevel(MDScopeCodePropertyType[] mappedHierarchyLevelArray) {
        List<String> mappedHierarchyLevels = new ArrayList<String>();
        for (MDScopeCodePropertyType mappedHierarchyLevel : mappedHierarchyLevelArray) {
            mappedHierarchyLevels.add(mappedHierarchyLevel.getMDScopeCode().getCodeListValue());
            String codeListString = mappedHierarchyLevel.getMDScopeCode().getCodeList();
            assertTrue(
                    String.format("Value of CodeList attribute not as expected. Expected %s, got %s.",
                            CODELIST_SCOPECODE, codeListString),
                    CODELIST_SCOPECODE.equals(codeListString));
        }
        for (MDScopeCodePropertyType expectedHierarchyLevel : expectedHierarchyLevelArray) {
            String expectedHierarchyLevelString = expectedHierarchyLevel.getMDScopeCode().getCodeListValue();
            assertTrue(String.format("Expected HierarchyLevel not present: %s", expectedHierarchyLevelString),
                    mappedHierarchyLevels.contains(expectedHierarchyLevelString));
        }
    }

    private void checkHierarchyLevelName(CharacterStringPropertyType[] mappedHierarchyLevelNameArray) {
        List<String> mappedHierarchyLevelNames = new ArrayList<String>();
        for (CharacterStringPropertyType mappedHierarchyLevelName : mappedHierarchyLevelNameArray) {
            mappedHierarchyLevelNames.add(mappedHierarchyLevelName.getCharacterString());
        }
        for (CharacterStringPropertyType expectedHierarchyLevelName : expectedHierarchyLevelNameArray) {
            String expectedHierarchyLevelNameString = expectedHierarchyLevelName.getCharacterString();
            assertTrue(String.format("Expected HierarchyLevelName not present: %s", expectedHierarchyLevelNameString),
                    mappedHierarchyLevelNames.contains(expectedHierarchyLevelNameString));
        }
    }

    private void checkId(MIMetadataType miMetadata) {
        try {
            String actualId = miMetadata.getFileIdentifier().getCharacterString();
            assertTrue(String.format("Ids do not match, expected %s, got %s", expectedId, actualId),
                    actualId.equals(expectedId));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void checkMDKeywordsPropertyTypeArray(MDKeywordsPropertyType[] descriptiveKeywordsArray) {

        int expectedMDKeywordsPropertyTypeArrayLength = expectedDescriptiveKeywordsArray.length;
        int actualMDKeywordsPropertyTypeArrayLength = descriptiveKeywordsArray.length;

        assertTrue(
                String.format("MDKeywordsPropertyTypeArray length does not match. Expected %d, is %d",
                        expectedMDKeywordsPropertyTypeArrayLength, actualMDKeywordsPropertyTypeArrayLength),
                expectedMDKeywordsPropertyTypeArrayLength == actualMDKeywordsPropertyTypeArrayLength);

        for (MDKeywordsPropertyType expectedMDKeywordsPropertyType : expectedDescriptiveKeywordsArray) {
            boolean foundMatch = false;
            for (MDKeywordsPropertyType mdKeywordsPropertyType : descriptiveKeywordsArray) {
                foundMatch = compareMDKeywordsPropertyTypes(expectedMDKeywordsPropertyType, mdKeywordsPropertyType);
            }
            assertTrue(String.format("Did not find a match for expected MDKeywordsPropertyType: %s",
                    expectedMDKeywordsPropertyType.xmlText()), foundMatch);
        }

    }

    private boolean compareMDKeywordsPropertyTypes(MDKeywordsPropertyType expectedMDKeywordsPropertyType,
            MDKeywordsPropertyType actualMDKeywordsPropertyType) {

        boolean result = true;

        String expectedCodeList =
                expectedMDKeywordsPropertyType.getMDKeywords().getType().getMDKeywordTypeCode().getCodeList();
        String actualCodeList =
                actualMDKeywordsPropertyType.getMDKeywords().getType().getMDKeywordTypeCode().getCodeList();

        result = result && (expectedCodeList.equals(actualCodeList));

        String expectedCodeListValue =
                expectedMDKeywordsPropertyType.getMDKeywords().getType().getMDKeywordTypeCode().getCodeListValue();
        String actualCodeListValue =
                actualMDKeywordsPropertyType.getMDKeywords().getType().getMDKeywordTypeCode().getCodeListValue();

        result = result && (expectedCodeListValue.equals(actualCodeListValue));

        MDKeywordsType expectedMDKeywordsType = expectedMDKeywordsPropertyType.getMDKeywords();
        MDKeywordsType actualMDKeywordsType = actualMDKeywordsPropertyType.getMDKeywords();

        result = result && checkKeywords(expectedMDKeywordsType, actualMDKeywordsType);

        return result;
    }

    private boolean checkKeywords(MDKeywordsType expectedMDKeywordsType,
            MDKeywordsType actualMDKeywordsType) {
        boolean result = true;
        List<String> actualKeywordsAsStringList = new ArrayList<String>();

        for (CharacterStringPropertyType characterStringPropertyType : actualMDKeywordsType.getKeywordArray()) {
            actualKeywordsAsStringList.add(characterStringPropertyType.getCharacterString());
        }
        for (CharacterStringPropertyType characterStringPropertyType : expectedMDKeywordsType.getKeywordArray()) {
            result = result && actualKeywordsAsStringList.contains(characterStringPropertyType.getCharacterString());
        }
        return result;
    }

}
