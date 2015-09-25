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

import java.util.Optional;
import org.n52.youngs.transform.MappingEntry;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class MappingEntryImpl implements MappingEntry {

    private final String xPath;

    private final String fieldName;

    private final boolean isoQueryable;

    private Optional<String> isoQueryableName = Optional.empty();

    public MappingEntryImpl(String xPath, String fieldName, boolean isoQueryable, String isoQueryableName) {
        this(xPath, fieldName, isoQueryable);
        this.isoQueryableName = Optional.ofNullable(isoQueryableName);
    }

    public MappingEntryImpl(String xPath, String fieldName, boolean isoQueryable) {
        this.xPath = xPath;
        this.fieldName = fieldName;
        this.isoQueryable = isoQueryable;
    }

    @Override
    public String getXPath() {
        return xPath;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean isIsoQueryable() {
        return isoQueryable && isoQueryableName.isPresent();
    }

    @Override
    public String getIsoQueryableName() {
        return isoQueryableName.get();
    }

}
