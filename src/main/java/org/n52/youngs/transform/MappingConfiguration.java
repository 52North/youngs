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
package org.n52.youngs.transform;

import java.util.Collection;
import org.w3c.dom.Document;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface MappingConfiguration {

    public static final String XPATH_10 = "1.0";

    public static final String XPATH_20 = "2.0";

    public static final String DEFAULT_XPATH_VERSION = XPATH_20;

    public static final double DEFAULT_VERSION = 1.0d;

    public static final String DEFAULT_NAME = "<unnamed>";
    
    public static final String DEFAULT_APPLICABILITY_PATH = "true()";

    public Collection<MappingEntry> getEntries();

    public String getName();

    public double getVersion();

    public String getXPathVersion();

    public boolean isApplicable(Document doc);

}
