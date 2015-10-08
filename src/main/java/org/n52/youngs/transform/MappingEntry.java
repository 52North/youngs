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
package org.n52.youngs.transform;

import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathExpression;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface MappingEntry {

    public interface IndexProperties {

        public static final String TYPE = "type";

    }

    public static final String INDEX_NAME = "index_name";

    public XPathExpression getXPath();

    public String getFieldName();

    public Map<String, Object> getIndexProperties();

    public Object getIndexPropery(String name);

    public boolean isIdentifier();

    public boolean hasCoordinates();

    /**
     *
     * @return a list of expressions to be evaluated on the element found by getXPath() to create coordinates
     */
    public List<XPathExpression[]> getCoordinatesXPaths();

    public boolean hasCoordinatesType();

    public String getCoordinatesType();

    public boolean isRawXml();

    public boolean hasReplacements();

    public Map<String, String> getReplacements();

}
