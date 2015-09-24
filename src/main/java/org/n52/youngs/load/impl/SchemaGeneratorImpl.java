/*
 * Copyright 2015-${currentYearDynamic} 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.load.impl;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.n52.youngs.load.SchemaGenerator;
import org.n52.youngs.transform.MappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class SchemaGeneratorImpl implements SchemaGenerator {

    public SchemaGeneratorImpl() {
        //
    }

    @Override
    public XContentBuilder generate(MappingConfiguration mapping) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
