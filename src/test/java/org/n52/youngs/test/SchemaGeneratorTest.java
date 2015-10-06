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
import java.util.Map;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.load.impl.SchemaGeneratorImpl;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SchemaGeneratorTest {

    @Test
    public void generateSchema() throws IOException {
        YamlMappingConfiguration config = new YamlMappingConfiguration("mappings/testmapping.yml", new XPathHelper().newXPathFactory());

        SchemaGenerator generator = new SchemaGeneratorImpl();
        Map<String, Object> generatedRequest = generator.generate(config);

        assertThat("creation is enabled", config.isIndexCreationEnabled(), is(true));
        assertThat("dynamic option is false (no field type guessing)", generatedRequest.get("dynamic"), is(false));

//        assertThat("contains dynamic field false", generatedRequest,
//                hasJsonPath("mappings.record.dynamic", is(false)));
    }

    @Test
    public void generateSchemaDisabledAndDynamic() throws IOException {
        YamlMappingConfiguration config = new YamlMappingConfiguration("mappings/testmapping-creation-disabled-but-dynamic.yml", new XPathHelper().newXPathFactory());

        SchemaGenerator generator = new SchemaGeneratorImpl();
        Map<String, Object> generatedRequest = generator.generate(config);

        assertThat("creation is disabled", config.isIndexCreationEnabled(), is(false));
        assertThat("dynamic options is true", generatedRequest.get("dynamic"), is(true));

//        assertThat("contains dynamic field false", generatedRequest,
//                hasJsonPath("mappings.record.dynamic", is(false)));
    }

}
