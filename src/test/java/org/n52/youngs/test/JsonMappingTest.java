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
package org.n52.youngs.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.JsonNodeSourceRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.impl.JsonToBuilderMapper;
import org.n52.youngs.transform.impl.LightweightMappingEntry;
import org.n52.youngs.transform.impl.LightweightYamlMappingConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonMappingTest {

    private static final String FIELDNAME_ID = "id";

    private static final String FIELDNAME_KEYWORDS = "keywords";

    private static final String FIELDNAME_KEYWORD = "keyword";

    private static final String FIELDNAME_KEYWORDS_AS_OBJECTS = "keywordsAsObjects";

    private static final String FIELDNAME_TYPE = "type";

    private static final String FIELDNAME_KEYWORDS_TYPES = "keywords_types";

    private static final String FIELDNAME_YOUNGS_CREATED_ON = "youngs_created_on";

    private static final String FIELDNAME_RESPONSIBLE_PARTY_ROLE = "responsiblePartyRole";

    private static final String FIELDNAME_RESPONSIBLE_ORG = "responsible_org";

    private static final long EXPECTED_TIME_DIFFERENCE_IN_SECONDS = 5;

    private static final String FIELDNAME_RESPONSIBLE_PARTY = "responsibleParty";

    private static final String FIELDNAME_ORGANIZATION_NAME = "organisationName";

    private static final String FIELDNAME_NAME = "name";

    private static final String FIELDNAME_TYPICAL_FILENAME = "typical_filename";

    private static final String FIELDNAME_TYPICALFILENAME = "typicalFilename";

    private static final String FIELDNAME_DIGITAL_TRANSFERS = "digitalTransfers";

    private static final String FIELDNAME_FORMAT = "format";

    private static final String ASSERTION_TEXT = "%s does not match. Got %s, expected %s.";

    private static final String FIELDNAME_AVAILABLE_FORMATS = "availableFormats";

    private static final String FIELDNAME_AVAILABILITY = "availability";

    private static final String FIELDNAME_DISTRIBUTION = "distribution";

    private List<String> expectedKeywords = new ArrayList<String>();

    private Map<String, String> expectedMappingStringMap;

    private Map<String, String> expectedValuesStringMap;

    private ObjectMapper objectMapper;

    private int expectedTypicalFileNameArraySize;

    private List<String> expectedTypicalFilenameList;

    private int expectedAvailableFormatsArraySize;

    private List<String> expectedAvailableFormatsList;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        expectedMappingStringMap = new HashMap<String, String>();
        expectedValuesStringMap = new HashMap<String, String>();
        expectedTypicalFilenameList = new ArrayList<String>();
        expectedAvailableFormatsList = new ArrayList<String>();
    }

    @Test
    public void testJsonMapping() {
        LightweightYamlMappingConfiguration mapper = new LightweightYamlMappingConfiguration(getClass().getClassLoader().getResourceAsStream("mappings/json-record.yml"));
        extractExpectedValues(mapper);
        JsonToBuilderMapper builderMapper = new JsonToBuilderMapper(mapper);
        JsonNode record;
        try {
            record = objectMapper.readTree(getClass().getClassLoader().getResourceAsStream("records/json/record2.json"));
        } catch (IOException e) {
            fail(e.getMessage());
            return;
        }
        extractExpectedValues(record);
        SourceRecord sourceRecord = new JsonNodeSourceRecord(record, "");
        SinkRecord mappedRecord = builderMapper.map(sourceRecord);
        assertTrue(String.format("Mapped record not of type BuilderRecord. Instead is of type %s.", mappedRecord.getClass().getCanonicalName()), mappedRecord instanceof BuilderRecord);
        BuilderRecord mappedBuilderRecord = (BuilderRecord) mappedRecord;
        JsonNode mappedRecordJsonNode;
        try {
            mappedRecordJsonNode = objectMapper.readTree(mappedBuilderRecord.getBuilder().string());
        } catch (IOException e) {
            fail(e.getMessage());
            return;
        }
        checkKeywords(mappedRecordJsonNode);
        checkCreatedOn(mappedRecordJsonNode);
        checkExpectedElements(mappedRecordJsonNode);
        checkValues(mappedRecordJsonNode);
        checkTypicalFilenames(mappedRecordJsonNode);
        checkAvailableFormats(mappedRecordJsonNode);
    }

    private void checkTypicalFilenames(JsonNode mappedRecordJsonNode) {
        JsonNode typicalFileNameNode = mappedRecordJsonNode.findPath(FIELDNAME_TYPICAL_FILENAME);
        assertTrue(String.format("Node not instanceof ArrayNode. Is instance of %s.", typicalFileNameNode.getClass().getCanonicalName()), typicalFileNameNode instanceof ArrayNode);
        ArrayNode typicalFileNameArrayNode = (ArrayNode)typicalFileNameNode;
        assertTrue(String.format("ArrayNode length not as expected. Is %d, expected %d.", typicalFileNameArrayNode.size(), expectedTypicalFileNameArraySize), typicalFileNameArrayNode.size() == expectedTypicalFileNameArraySize);
        ArrayList<String> typicalFilenameList = new ArrayList<String>();
        for (JsonNode typicalFileNameTextNode : typicalFileNameArrayNode) {
            typicalFilenameList.add(typicalFileNameTextNode.asText());
        }
        for (String expectedTypicalFilename : expectedTypicalFilenameList) {
            assertTrue(String.format("Expected typical file name not mapped: %s", expectedTypicalFilename), typicalFilenameList.contains(expectedTypicalFilename));
        }
    }

    private void checkAvailableFormats(JsonNode mappedRecordJsonNode) {
        JsonNode availableFormat = mappedRecordJsonNode.findPath(FIELDNAME_AVAILABLE_FORMATS);
        assertTrue(String.format("Node not instanceof ArrayNode. Is instance of %s.", availableFormat.getClass().getCanonicalName()), availableFormat instanceof ArrayNode);
        ArrayNode availableFormatArrayNode = (ArrayNode)availableFormat;
        assertTrue(String.format("ArrayNode length not as expected. Is %d, expected %d.", availableFormatArrayNode.size(), expectedAvailableFormatsArraySize), availableFormatArrayNode.size() == expectedAvailableFormatsArraySize);
        ArrayList<String> AvailableFormatList = new ArrayList<String>();
        for (JsonNode AvailableFormatTextNode : availableFormatArrayNode) {
            AvailableFormatList.add(AvailableFormatTextNode.asText());
        }
        for (String expectedAvailableFormat : expectedAvailableFormatsList) {
            assertTrue(String.format("Expected typical file name not mapped: %s", expectedAvailableFormat), AvailableFormatList.contains(expectedAvailableFormat));
        }
    }

    private void extractExpectedValues(LightweightYamlMappingConfiguration mapper) {
        Collection<LightweightMappingEntry> entries = mapper.getLightweightEntries();
        for (LightweightMappingEntry lightweightMappingEntry : entries) {
            switch (lightweightMappingEntry.getType()) {
            case STRING:
                String expression = lightweightMappingEntry.getExpression();
                expectedMappingStringMap.put(lightweightMappingEntry.getFieldName(), expression);
                break;
            default:
                break;
            }
        }
    }

    private void checkValues(JsonNode mappedRecordJsonNode) {
        for (String expectedValueKey : expectedValuesStringMap.keySet()) {
            String expectedValue = expectedValuesStringMap.get(expectedValueKey);
            TextNode responsiblePartyRoleNode = (TextNode) mappedRecordJsonNode.path(expectedValueKey);
            String responsiblePartyRoleString = responsiblePartyRoleNode.asText();
            assertTrue(String.format(ASSERTION_TEXT , expectedValueKey, responsiblePartyRoleString, expectedValue), expectedValue.equals(responsiblePartyRoleString));
        }
    }

    private void checkExpectedElements(JsonNode mappedRecordJsonNode) {
        for (String expectedNodeFieldName : expectedMappingStringMap.keySet()) {
            TextNode youngsUsedMappingNode = (TextNode) mappedRecordJsonNode.path(expectedNodeFieldName);
            String youngsUsedMappingString = youngsUsedMappingNode.asText();
            String expectedYoungsUsedMappingString = expectedMappingStringMap.get(expectedNodeFieldName);
            assertTrue(String.format(ASSERTION_TEXT, expectedNodeFieldName, youngsUsedMappingString, expectedYoungsUsedMappingString), expectedYoungsUsedMappingString.equals(youngsUsedMappingString));
        }
    }

    private void checkCreatedOn(JsonNode mappedRecordJsonNode) {
        TextNode youngsCreatedOnNode = (TextNode) mappedRecordJsonNode.path(FIELDNAME_YOUNGS_CREATED_ON);
        String youngsCreatedOnString = youngsCreatedOnNode.asText();
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime youngsCreatedOnocalDateTime = LocalDateTime.parse(youngsCreatedOnString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            long diff = ChronoUnit.SECONDS.between(now, youngsCreatedOnocalDateTime);
            assertTrue("Youngs created on date time that is older than five seconds from current time. Value is: " + youngsCreatedOnocalDateTime, diff < EXPECTED_TIME_DIFFERENCE_IN_SECONDS);
        } catch (DateTimeParseException e) {
            fail(String.format("Could not parse text to date. Got text: %s.", youngsCreatedOnString));
        }
    }

    private void extractExpectedValues(JsonNode metadataNode) throws IllegalArgumentException {
        expectedValuesStringMap.put(FIELDNAME_ID, metadataNode.path(FIELDNAME_ID).asText());
        JsonNode keywordsAsObjectsNode = metadataNode.path(FIELDNAME_KEYWORDS_AS_OBJECTS);
        JsonNode keywordNode = keywordsAsObjectsNode.path(FIELDNAME_KEYWORD);
        ArrayNode keywordArrayNode = getArrayNode(keywordNode);
        keywordArrayNode.iterator().forEachRemaining(keyword -> expectedKeywords.add(keyword.asText()));
        expectedValuesStringMap.put(FIELDNAME_KEYWORDS_TYPES, keywordsAsObjectsNode.path(FIELDNAME_TYPE).asText());
        JsonNode responsiblePartyNode = metadataNode.path(FIELDNAME_RESPONSIBLE_PARTY);
        expectedValuesStringMap.put(FIELDNAME_RESPONSIBLE_PARTY_ROLE, responsiblePartyNode.path(FIELDNAME_RESPONSIBLE_PARTY_ROLE).asText());
        expectedValuesStringMap.put(FIELDNAME_RESPONSIBLE_ORG, responsiblePartyNode.path(FIELDNAME_ORGANIZATION_NAME).asText());
        JsonNode digitalTransfersNode = metadataNode.path(FIELDNAME_DIGITAL_TRANSFERS);
        JsonNode availabilityNode = digitalTransfersNode.path(FIELDNAME_AVAILABILITY);
        String expectedAvailabiltyString = "";
        if(availabilityNode instanceof ArrayNode) {
            expectedAvailabiltyString = ((ArrayNode)availabilityNode).get(0).path(FIELDNAME_AVAILABILITY).asText();
        }
        expectedValuesStringMap.put(FIELDNAME_DISTRIBUTION, expectedAvailabiltyString);
        expectedValuesStringMap.put(FIELDNAME_AVAILABILITY, expectedAvailabiltyString);
        extractExpectedTypicalFilenames(metadataNode);
        extractAvailableFormats(metadataNode);
    }

    private void extractExpectedTypicalFilenames(JsonNode metadataNode) {
        JsonNode digitalTransfersNode = metadataNode.path(FIELDNAME_DIGITAL_TRANSFERS);
        JsonNode formatsNode = digitalTransfersNode.path(FIELDNAME_FORMAT);
        if(formatsNode instanceof ArrayNode) {
            for (JsonNode jsonNode : formatsNode) {
                expectedTypicalFilenameList.add(jsonNode.path(FIELDNAME_TYPICALFILENAME).asText());
            }
            expectedTypicalFileNameArraySize = expectedTypicalFilenameList.size();
        } else {
            expectedTypicalFilenameList.add(formatsNode.path(FIELDNAME_TYPICALFILENAME).asText());
            expectedTypicalFileNameArraySize = 1;
        }
    }

    private void extractAvailableFormats(JsonNode metadataNode) {
        JsonNode digitalTransfersNode = metadataNode.path(FIELDNAME_DIGITAL_TRANSFERS);
        JsonNode formatsNode = digitalTransfersNode.path(FIELDNAME_FORMAT);
        if(formatsNode instanceof ArrayNode) {
            for (JsonNode jsonNode : formatsNode) {
                expectedAvailableFormatsList.add(jsonNode.path(FIELDNAME_NAME).asText());
            }
            expectedAvailableFormatsArraySize = expectedAvailableFormatsList.size();
        } else {
            expectedAvailableFormatsList.add(formatsNode.path(FIELDNAME_NAME).asText());
            expectedAvailableFormatsArraySize = 1;
        }
    }

    private ArrayNode getArrayNode(JsonNode arrayNode) {
        assertTrue("Node is not an ArrayNode.", arrayNode instanceof ArrayNode);
        return (ArrayNode) arrayNode;
    }

    private void checkKeywords(JsonNode tree) {
        JsonNode keywordsNode = tree.path(FIELDNAME_KEYWORDS);
        ArrayNode keywordsArrayNode = getArrayNode(keywordsNode);
        int keywordsArraySize = keywordsArrayNode.size();
        assertTrue("Keywords empty.", keywordsArraySize > 0);
        assertTrue(String.format("Keywords size not equal to expected size. Got %d, expected %d.", keywordsArraySize, expectedKeywords.size()), keywordsArraySize == expectedKeywords.size());
        for (JsonNode jsonNode : keywordsArrayNode) {
            String keyWordText = jsonNode.asText();
            if (!expectedKeywords.contains(keyWordText)) {
                fail(String.format("Mapped keywords do not contain %s.", keyWordText));
            }
        }
    }
}
