/*
 * Copyright 2015-2024 52°North Spatial Information Research GmbH
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
import java.util.Collections;
import java.util.Map;
import org.n52.youngs.api.XPathConstants;
import org.w3c.dom.Document;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface MappingConfiguration {

    public static final String DEFAULT_XPATH_VERSION = XPathConstants.XPATH_20;

    public static final int DEFAULT_VERSION = 1;

    public static final String DEFAULT_NAME = "<unnamed>";

    public static final String DEFAULT_APPLICABILITY_PATH = "true()";

    public static final String DEFAULT_TYPE = "record";

    public static final String DEFAULT_INDEX = "elasticsearch";

    public static final boolean DEFAULT_INDEX_CREATION = false;

    public static final boolean DEFAULT_STORE_XML = true;

    public static final String DEFAULT_STORE_XML_FIELDNAME = "xmldoc";

    public static final boolean DEFAULT_DYNAMIC_MAPPING = false;

    public static final String DEFAULT_INDEXPROPERTY_TYPE = "text";

    /**
     *
     * @return a sorted list of mapping entries, ordered by the field name
    */
    public Collection<MappingEntry> getEntries();

    public MappingEntry getEntry(String name);

    public String getName();

    public int getVersion();

    public String getIndex();

    public String getType();

    public String getIdentifierField();

    public boolean hasLocationField();

    public String getLocationField();

    public String getXPathVersion();

    public boolean isApplicable(Document doc);

    public boolean isIndexCreationEnabled();

    public boolean isDynamicMappingEnabled();

    public boolean hasIndexCreationRequest();

    public Map<String, Object> getIndexCreationRequest();

    default boolean hasSuggest() {
        return false;
    }

    default Map<String, Object> getSuggest() {
        return Collections.emptyMap();
    }

}
