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
package org.n52.youngs.test;

import java.io.IOException;
import java.util.Collection;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.n52.youngs.harvest.NamespaceContextImpl;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.MappingEntry;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;
import org.w3c.dom.Document;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class YamlConfiguration {

    private YamlMappingConfiguration config;

    @Before
    public void loadFile() throws IOException {
        config = new YamlMappingConfiguration("mappings/testmapping.yml", NamespaceContextImpl.create());
    }

    @Test
    public void testConfigMetadata() throws IOException {
        assertThat("name is correct", config.getName(), is(equalTo("test")));
        assertThat("version is correct", config.getVersion(), is(equalTo(4.2d)));
        assertThat("XPath version is correct", config.getXPathVersion(), is(equalTo("2.0")));
    }

    @Test
    public void testDefaultMetadata() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-invalidXPath.yml", NamespaceContextImpl.create());

        assertThat("name is correct", otherConfig.getName(), is(equalTo(MappingConfiguration.DEFAULT_NAME)));
        assertThat("version is correct", otherConfig.getVersion(), is(equalTo(MappingConfiguration.DEFAULT_VERSION)));
        assertThat("XPath version is correct", otherConfig.getXPathVersion(), is(equalTo(MappingConfiguration.DEFAULT_XPATH_VERSION)));
    }

    @Test
    public void testApplicability() throws Exception {
        Document document = Util.getDocument("<testdoc xmlns=\"http://www.isotc211.org/2005/gmd\">"
                + "<MD_Metadata>"
                + "<id>42</id>"
                + "</MD_Metadata>"
                + "</testdoc>");
        assertThat("matching document is applicable", config.isApplicable(document), is(true));

        document = Util.getDocument("<testdoc xmlns=\"http://www.isotc211.org/2005/gmd\">"
                + "<MI_Metadata>"
                + "<id>42</id>"
                + "</MI_Metadata>"
                + "</testdoc>");
        assertThat("non-matching element is not applicable", config.isApplicable(document), is(false));

        document = Util.getDocument("<testdoc>"
                + "<ns1:MD_Metadata xmlns:ns1=\"http://wrong.namespace\">"
                + "<id>42</id>"
                + "</ns1:MD_Metadata>"
                + "</testdoc>");
        assertThat("non-matching namespace is not applicable", config.isApplicable(document), is(false));
    }

    @Test
    public void testApplicabilityInvalidPath() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-invalidXPath.yml", NamespaceContextImpl.create());

        Document document = Util.getDocument("<testdoc/>");

        assertThat("matching document is always applicable", otherConfig.isApplicable(document), is(true));
        assertThat("matching document is always applicable", otherConfig.isApplicable(null), is(true));
    }

    @Test
    public void testApplicabilityMissing() throws Exception {
        YamlMappingConfiguration otherConfig = new YamlMappingConfiguration("mappings/testmapping-empty.yml", NamespaceContextImpl.create());

        Document document = Util.getDocument("<testdoc/>");

        assertThat("is always applicable", otherConfig.isApplicable(document), is(true));
        assertThat("is always applicable", otherConfig.isApplicable(null), is(true));
    }

    @Test
    public void testEntries() throws IOException {
        Collection<MappingEntry> entries = config.getEntries();

        assertThat("all entries are loaded", entries.size(), is(equalTo(3)));
    }

}
