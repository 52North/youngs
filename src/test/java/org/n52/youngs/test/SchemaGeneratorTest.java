/*
 * Copyright 2015-2017 52°North Initiative for Geospatial Open Source
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.load.impl.SchemaGeneratorImpl;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SchemaGeneratorTest {

//    @Test
    public void generateSchema() throws IOException {
        YamlMappingConfiguration config = new YamlMappingConfiguration("mappings/testmapping.yml", new XPathHelper());

        SchemaGenerator generator = new SchemaGeneratorImpl();
        Map<String, Object> generatedRequest = generator.generate(config);

        assertThat("creation is enabled", config.isIndexCreationEnabled(), is(true));
        assertThat("dynamic option is false (no field type guessing)", generatedRequest.get("dynamic"), is(false));

//        assertThat("contains dynamic field false", generatedRequest,
//                hasJsonPath("mappings.record.dynamic", is(false)));
    }

//    @Test
    public void generateSchemaDisabledAndDynamic() throws IOException {
        YamlMappingConfiguration config = new YamlMappingConfiguration("mappings/testmapping-creation-disabled-but-dynamic.yml", new XPathHelper());

        SchemaGenerator generator = new SchemaGeneratorImpl();
        Map<String, Object> generatedRequest = generator.generate(config);

        assertThat("creation is disabled", config.isIndexCreationEnabled(), is(false));
        assertThat("dynamic options is true", generatedRequest.get("dynamic"), is(true));

//        assertThat("contains dynamic field false", generatedRequest,
//                hasJsonPath("mappings.record.dynamic", is(false)));
    }

//    @Test
    public void testSuggestMapping() throws IOException {
        YamlMappingConfiguration config = new YamlMappingConfiguration("mappings/testmapping-suggest.yml", new XPathHelper());

        SchemaGenerator generator = new SchemaGeneratorImpl();
        Map<String, Object> generatedRequest = generator.generate(config);

        assertThat(generatedRequest.get("properties"), CoreMatchers.notNullValue());
        Map<String, Object> props = (Map<String, Object>) generatedRequest.get("properties");
        assertThat(props.get("suggest"), CoreMatchers.notNullValue());
        Map<String, Object> suggest = (Map<String, Object>) props.get("suggest");

        assertThat(suggest.containsKey("mappingConfiguration"), CoreMatchers.is(false));
        assertThat(suggest.get("type"), CoreMatchers.equalTo("completion"));
        assertThat(suggest.get("analyzer"), CoreMatchers.equalTo("simple"));
        assertThat(suggest.get("search_analyzer"), CoreMatchers.equalTo("simple"));
        assertThat(suggest.get("payloads"), CoreMatchers.equalTo(false));
    }

    @Test
    public void testNgramAnalyzerMapping() throws IOException {
        YamlMappingConfiguration config = new YamlMappingConfiguration("mappings/testmapping-ngram.yml", new XPathHelper());

        SchemaGenerator generator = new SchemaGeneratorImpl();
        Map<String, Object> generatedRequest = generator.generate(config);
        String jsonRequest = new ObjectMapper().writeValueAsString(generatedRequest);


        assertThat(generatedRequest.get("properties"), CoreMatchers.notNullValue());
        Map<String, Object> props = (Map<String, Object>) generatedRequest.get("properties");

        assertThat(props.get("datasetTitle"), CoreMatchers.notNullValue());
        Map<String, Object> dataset = (Map<String, Object>) props.get("datasetTitle");

        assertThat(dataset.get("analyzer"), CoreMatchers.equalTo("suggest_autocomplete"));
        assertThat(dataset.get("search_analyzer"), CoreMatchers.equalTo("standard"));
    }
}
