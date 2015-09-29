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
package org.n52.youngs.test;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.youngs.api.Record;
import org.n52.youngs.harvest.DirectorySource;
import org.n52.youngs.impl.NamespaceContextImpl;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class DirectorySourceTest {

    private static Path baseDirectory;

    private static Mapper cswMapper;

    @BeforeClass
    public static void baseDir() throws URISyntaxException, IOException {
        baseDirectory = Paths.get(Resources.getResource("records").toURI());

        MappingConfiguration cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/csw-record.yml")).openStream(),
                NamespaceContextImpl.create(), new XPathHelper().newXPathFactory());
        cswMapper = new CswToBuilderMapper(cswConfiguration);
    }

    @Test
    public void fileUrl() throws MalformedURLException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        URL endpoint = source.getEndpoint();
        // probably everything is already good at this point
        assertThat("beginning of url is file:/", endpoint.toExternalForm(), startsWith("file:/"));
        assertThat("end of url is directories", endpoint.getPath(), endsWith("records/csw/"));
        assertThat("file is correct", endpoint.sameFile(baseDirectory.resolve("csw").toFile().toURI().toURL()), is(true));
    }

    @Test
    public void testCount() {
        assertThat("correct number of files found", new DirectorySource(baseDirectory.resolve("csw"))
                .getRecordCount(), is(equalTo(13l)));
    }

    @Test
    public void testFilteredCount() {
        FileFilter filter = (File pathname) -> pathname.getName().endsWith("xml");
        assertThat("correct number of files found", new DirectorySource(baseDirectory.resolve("csw"), filter)
                .getRecordCount(), is(equalTo(12l)));
    }

    @Test
    public void testCswRecordsDirectory() throws IOException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        Collection<SourceRecord> records = source.getRecords();
        assertThat("correct number of records returned", records.size(), is(equalTo(12)));

        Set<Boolean> isNodeRecord = records.stream().map(r -> {
            return r instanceof NodeSourceRecord;
        }).collect(Collectors.toSet());
        assertThat("all records are NodeSourceRecords", isNodeRecord, is(equalTo(Sets.newHashSet(true))));

        Iterator<SourceRecord> iter = records.iterator();
        BuilderRecord mappedRecord = (BuilderRecord) cswMapper.map((SourceRecord) iter.next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("record id is in mapped record", mappedRecordString, containsString("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f"));
    }

    @Test
    public void testPagination() {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        Collection<SourceRecord> records = source.getRecords(2, 9);

        String allMappedRecordsString = Util.sourceRecordsToString(records, cswMapper);

        assertThat("first record id is in NOT mapped records", allMappedRecordsString,
                not(containsString("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f")));
        assertThat("second record id is in mapped records", allMappedRecordsString,
                containsString("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd"));
        assertThat("second last record id is in mapped records", allMappedRecordsString,
                containsString("urn:uuid:ab42a8c4-95e8-4630-bf79-33e59241605a"));
        assertThat("last record id is in NOT mapped records", allMappedRecordsString,
                not(containsString("urn:uuid:e9330592-0932-474b-be34-c3a3bb67c7db")));
    }

    @Test
    public void testPaginationUpperBound() {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        Collection<SourceRecord> records = source.getRecords(12, 100);

        String allMappedRecordsString = Util.sourceRecordsToString(records, cswMapper);

        assertThat("last record id is in mapped records", allMappedRecordsString,
                containsString("e9330592-0932-474b-be34-c3a3bb67c7db"));
    }

    @Test
    public void testMixedMetadata() throws IOException, URISyntaxException {
        File tempDirectory = Files.createTempDirectory("youngs-").toFile();

        FileFilter filter = (File pathname) -> pathname.getName().endsWith(".xml");

        FileUtils.copyDirectory(baseDirectory.resolve("csw").toFile(), tempDirectory, filter);
        FileUtils.copyDirectory(baseDirectory.resolve("gmd").toFile(), tempDirectory, filter);
        FileUtils.copyDirectory(baseDirectory.resolve("gmi").toFile(), tempDirectory, filter);

        System.out.println(String.format("Copied %s files into common directory for harvesting: %s",
                tempDirectory.listFiles().length,
                Arrays.toString(tempDirectory.listFiles())));

        DirectorySource source = new DirectorySource(tempDirectory.toPath());

        assertThat("all files are found", source.getRecordCount(), is(equalTo(16l)));
    }

}
