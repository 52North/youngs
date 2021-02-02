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

import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpression;

import org.n52.youngs.transform.MappingEntry;

public class LightweightMappingEntry implements MappingEntry {

    private String expression;

    private String fieldName;

    private MappingType type;

    public LightweightMappingEntry(String fieldName, String expression) {
        this.fieldName = fieldName;
        this.expression = expression;
    }

    public String getExpression() {
        return this.expression;
    }

    public void setType(MappingType mappingType) {
        this.type = mappingType;
    }

    public MappingType getType() {
        return this.type;
    }

    @Override
    public XPathExpression getXPath() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    @Override
    public Map<String, Object> getIndexProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getIndexPropery(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isIdentifier() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLocation() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasCoordinates() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<XPathExpression[]> getCoordinatesXPaths() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasCoordinatesType() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCoordinatesType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRawXml() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasReplacements() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getReplacements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasSplit() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getSplit() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasOutputProperties() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<? extends String, ? extends String> getOutputProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isAnalyzed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public XPathExpression getCondition() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasCondition() {
        // TODO Auto-generated method stub
        return false;
    }
}
