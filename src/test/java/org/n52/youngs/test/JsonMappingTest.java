package org.n52.youngs.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
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

    private static final String FIELDNAME_ORGANIZAiION_NAME = "organisationName";
   
    private List<String> expectedKeywords = new ArrayList<String>();

    private String expectedKeywordsTypes = "";

    private String expectedIdentifier;
    
    private String expectedResponsiblePartyRole;
    
    private String expectedResponsibleOrg;
    
    private Map<String, String> expectedMappingStringMap;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        expectedMappingStringMap = new HashMap<String, String>();
    }

    @Test
    public void testJsonMapping() {
        LightweightYamlMappingConfiguration mapper = new LightweightYamlMappingConfiguration(getClass().getClassLoader().getResourceAsStream("mappings/json-record.yml"));
        extractExpectedValues(mapper);
        JsonToBuilderMapper builderMapper = new JsonToBuilderMapper(mapper);
        JsonNode record;
        try {
            record = objectMapper.readTree(getClass().getClassLoader().getResourceAsStream("records/json/record.json"));
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
        checkId(mappedRecordJsonNode);
        checkKeywords(mappedRecordJsonNode);
        checkCreatedOn(mappedRecordJsonNode);
        checkExpectedElements(mappedRecordJsonNode);
        checkResponsiblePartyElements(mappedRecordJsonNode);
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

    private void checkResponsiblePartyElements(JsonNode mappedRecordJsonNode) {        
      TextNode responsiblePartyRoleNode = (TextNode) mappedRecordJsonNode.path(FIELDNAME_RESPONSIBLE_PARTY_ROLE);
      String responsiblePartyRoleString = responsiblePartyRoleNode.asText();
      assertTrue(String.format("%s does not match. Got %s, expected %s.", FIELDNAME_RESPONSIBLE_PARTY_ROLE, responsiblePartyRoleString, expectedResponsiblePartyRole), expectedResponsiblePartyRole.equals(responsiblePartyRoleString));
      TextNode responsibleOrgNode = (TextNode) mappedRecordJsonNode.path(FIELDNAME_RESPONSIBLE_ORG);
      String responsibleOrgString = responsibleOrgNode.asText();
      assertTrue(String.format("%s does not match. Got %s, expected %s.", FIELDNAME_RESPONSIBLE_ORG, responsibleOrgString, expectedResponsibleOrg), expectedResponsibleOrg.equals(responsibleOrgString));
    }
    
    private void checkExpectedElements(JsonNode mappedRecordJsonNode) {        
        for (String expectedNodeFieldName : expectedMappingStringMap.keySet()) {
            TextNode youngsUsedMappingNode = (TextNode) mappedRecordJsonNode.path(expectedNodeFieldName);
            String youngsUsedMappingString = youngsUsedMappingNode.asText();
            String expectedYoungsUsedMappingString = expectedMappingStringMap.get(expectedNodeFieldName);
            assertTrue(String.format("%sdoes not match. Got %s, expected %s.", expectedNodeFieldName, youngsUsedMappingString, expectedYoungsUsedMappingString), expectedYoungsUsedMappingString.equals(youngsUsedMappingString));     
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
        expectedIdentifier = metadataNode.path(FIELDNAME_ID).asText();
        JsonNode keywordsAsObjectsNode = metadataNode.path(FIELDNAME_KEYWORDS_AS_OBJECTS);
        JsonNode keywordNode = keywordsAsObjectsNode.path(FIELDNAME_KEYWORD);
        ArrayNode keywordArrayNode = getArrayNode(keywordNode);
        keywordArrayNode.iterator().forEachRemaining(keyword -> expectedKeywords.add(keyword.asText()));
        expectedKeywordsTypes = keywordsAsObjectsNode.path(FIELDNAME_TYPE).asText();        
        JsonNode responsiblePartyNode = metadataNode.path(FIELDNAME_RESPONSIBLE_PARTY);
        expectedResponsiblePartyRole = responsiblePartyNode.path(FIELDNAME_RESPONSIBLE_PARTY_ROLE).asText();
        expectedResponsibleOrg = responsiblePartyNode.path(FIELDNAME_ORGANIZAiION_NAME).asText();        
    }

    private ArrayNode getArrayNode(JsonNode keywordNode) {
        assertTrue("Keywords not a list.", keywordNode instanceof ArrayNode);
        return (ArrayNode) keywordNode;
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
        String keyword_types = tree.findPath(FIELDNAME_KEYWORDS_TYPES).asText();
        assertTrue(String.format("Keyword types not equal. Got %s, expected %s.", keyword_types, expectedKeywordsTypes), expectedKeywordsTypes.equals(keyword_types));
    }

    private void checkId(JsonNode tree) {
        String _id = tree.findPath(FIELDNAME_ID).asText();
        assertTrue(String.format("IDs do not match. Got %S, should be %S", _id, expectedIdentifier), _id.equals(expectedIdentifier));
    }
}
