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
package org.n52.youngs.transform.impl;

import org.n52.youngs.transform.MappingEntry;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.Collection;
import org.n52.youngs.transform.MappingConfiguration;

/**
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class YamlMappingConfiguration implements MappingConfiguration {

    private final File file;

    public YamlMappingConfiguration(File file) {
        this.file = file;
    }

    @Override
    public Collection<MappingEntry> getEntries() {
        Collection<MappingEntry> entries = Lists.newArrayList();

        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:fileIdentifier/gco:CharacterString", "id", false));
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:language/gmd:LanguageCode/@codeListValue", "language", false));
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:metadataStandardName/gco:CharacterString", "mdStandardName", false));
        entries.add(new MappingEntryImpl("//gmd:MD_Metadata/gmd:metadataStandardVersion/gco:CharacterString", "mdStandardVersion", false));

        return entries;
    }

}
