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
package org.n52.youngs.transform.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.xml.xpath.XPathExpression;
import org.n52.youngs.transform.MappingEntry;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class MappingEntryImpl implements MappingEntry {

    private final XPathExpression xPath;

    private final boolean isoQueryable;

    private Optional<String> isoQueryableName = Optional.empty();

    private final Map<String, Object> indexProperties = Maps.newHashMap();

    private Optional<Boolean> identifier = Optional.empty();
    
    private Optional<XPathExpression> coordinates = Optional.empty();

    public MappingEntryImpl(XPathExpression xPath, boolean isoQueryable, String isoQueryableName,
            Map<String, Object> indexProperties, boolean identifier) {
        this(xPath, isoQueryable, indexProperties, identifier);
        this.isoQueryableName = Optional.ofNullable(isoQueryableName);
    }

    public MappingEntryImpl(XPathExpression xPath, boolean isoQueryable, Map<String, Object> indexProperties,
            boolean identifier) {
        this.xPath = xPath;
        this.isoQueryable = isoQueryable;
        this.indexProperties.putAll(indexProperties);
        this.identifier = Optional.ofNullable(identifier);
    }

    @Override
    public XPathExpression getXPath() {
        return xPath;
    }

    @Override
    public String getFieldName() {
        return (String) indexProperties.get(INDEX_NAME);
    }

    @Override
    public boolean isIsoQueryable() {
        return isoQueryable && isoQueryableName.isPresent();
    }

    @Override
    public String getIsoQueryableName() {
        return isoQueryableName.get();
    }

    public MappingEntryImpl addIndexProperty(String key, Object value) {
        this.indexProperties.put(key, value);
        return this;
    }

    @Override
    public Map<String, Object> getIndexProperties() {
        return this.indexProperties;
    }

    @Override
    public Object getIndexPropery(String name) {
        return this.indexProperties.get(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("xpath", xPath)
                .add("isoQueryable", isoQueryable)
                .add("isoName", isoQueryableName)
                .add("properties", Arrays.deepToString(indexProperties.entrySet().toArray()))
                .omitNullValues()
                .toString();
    }

    @Override
    public boolean isIdentifier() {
        return identifier.isPresent() && identifier.get();
    }

    @Override
    public boolean hasCoordinates() {
        return coordinates.isPresent();
    }

    @Override
    public XPathExpression getCoordinatesXPath() {
        return coordinates.get();
    }
    
    public MappingEntryImpl setCoordinatesXPath(XPathExpression coords) {
        this.coordinates = Optional.of(coords);
        return this;
    }

}
