/*
 * Copyright 2015-2022 52°North Initiative for Geospatial Open Source
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

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.youngs.harvest.CswSource;
import org.n52.youngs.harvest.KvpCswSource;
import org.n52.youngs.harvest.PoxCswSource;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.impl.ReportImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class CswSourceIT {

    private static YamlMappingConfiguration mapping;

    private static CswToBuilderMapper mapper;

    @BeforeClass
    public static void prepare() throws IOException {
        mapping = new YamlMappingConfiguration(Resources.asByteSource(
                Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper());
        mapper = new CswToBuilderMapper(mapping);
    }

    @Test
    public void kvpCount() throws Exception {
        CswSource source = new KvpCswSource(new URL("http://api.eurogeoss-broker.eu/dab/services/cswiso"),
                (Collection<String>) ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2"), NamespaceContextImpl.create(),
                "csw:Record", "http://www.opengis.net/cat/csw/2.0.2");

        long count = source.getRecordCount();
        Assert.assertThat("record count is higher than last manual check", count, is(greaterThan(900000l)));
    }

    @Test
    public void poxCount() throws Exception {
        CswSource source = new PoxCswSource(new URL("http://api.eurogeoss-broker.eu/dab/services/cswiso"),
                (Collection<String>) ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2"), NamespaceContextImpl.create(),
                "csw:Record", "http://www.opengis.net/cat/csw/2.0.2");

        long count = source.getRecordCount();
        Assert.assertThat("record count is higher than last manual check", count, is(greaterThan(900000l)));
    }

    @Test
    public void harvestKVP() throws Exception {
        CswSource source = new KvpCswSource(new URL("http://api.eurogeoss-broker.eu/dab/services/cswiso"),
                (Collection<String>) ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2"), NamespaceContextImpl.create(),
                "csw:Record", "http://www.opengis.net/cat/csw/2.0.2");

        Collection<SourceRecord> records = source.getRecords(1, 52, new ReportImpl());
        List<SinkRecord> mappedRecords = records.stream().map(mapper::map).collect(Collectors.toList());

        assertThat("all records mapped", mappedRecords.size(), is(52));

        Set<String> ids = mappedRecords.stream().map(SinkRecord::getId).collect(Collectors.toSet());
        assertThat("ids are contained in mapped records", ids, // some test IDs
                hasItems("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:MULT:AHL-DLI",
                        "urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:METOP:IASIL2CLP",
                        "urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:GOES:FAG"));
    }

    @Test
    public void harvestPOX() throws Exception {
        CswSource source = new PoxCswSource(new URL("http://api.eurogeoss-broker.eu/dab/services/cswiso"),
                (Collection<String>) ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2"), NamespaceContextImpl.create(),
                "csw:Record", "http://www.opengis.net/cat/csw/2.0.2");

        Collection<SourceRecord> records = source.getRecords(1, 52, new ReportImpl());
        List<SinkRecord> mappedRecords = records.stream().map(mapper::map).collect(Collectors.toList());

        assertThat("all records mapped", mappedRecords.size(), is(52));

        Set<String> ids = mappedRecords.stream().map(SinkRecord::getId).collect(Collectors.toSet());
        assertThat("ids are contained in mapped records", ids, // some test IDs
                hasItems("urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:MULT:AHL-DLI",
                        "urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:METOP:IASIL2CLP",
                        "urn:x-wmo:md:int.eumetsat::EO:EUM:DAT:GOES:FAG"));
    }

}
