/*
 * Copyright 2015-2018 52°North Initiative for Geospatial Open Source
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;
import org.n52.youngs.harvest.DirectorySource;
import org.n52.youngs.harvest.NodeSourceRecord;
import org.n52.youngs.harvest.SourceException;
import org.n52.youngs.harvest.SourceRecord;
import org.n52.youngs.impl.ReportImpl;
import org.n52.youngs.impl.XPathHelper;
import org.n52.youngs.load.SinkRecord;
import org.n52.youngs.load.impl.BuilderRecord;
import org.n52.youngs.transform.Mapper;
import org.n52.youngs.transform.MappingConfiguration;
import org.n52.youngs.transform.impl.CswToBuilderMapper;
import org.n52.youngs.transform.impl.YamlMappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 *
 * TODO: Fix this, it is platform-dependent!
 */
public class DirectorySourceTestDisabled {

    private static Path baseDirectory;

    private static Mapper mapper;

    @BeforeClass
    public static void baseDir() throws URISyntaxException, IOException {
        baseDirectory = Paths.get(Resources.getResource("records").toURI());

        MappingConfiguration cswConfiguration = new YamlMappingConfiguration(
                Resources.asByteSource(Resources.getResource("mappings/csw-record.yml")).openStream(),
                new XPathHelper());
        mapper = new CswToBuilderMapper(cswConfiguration);
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
    public void testCswRecordsDirectory() throws IOException, SourceException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        Collection<SourceRecord> records = source.getRecords(new ReportImpl());
        assertThat("correct number of records returned", records.size(), is(equalTo(12)));

        Set<Boolean> isNodeRecord = records.stream().map(r -> {
            return r instanceof NodeSourceRecord;
        }).collect(Collectors.toSet());
        assertThat("all records are NodeSourceRecords", isNodeRecord, is(equalTo(Sets.newHashSet(true))));

        Iterator<SourceRecord> iter = records.iterator();
        BuilderRecord mappedRecord = (BuilderRecord) mapper.map(iter.next());
        String mappedRecordString = mappedRecord.getBuilder().string();

        assertThat("record id is in mapped record", mappedRecordString, containsString("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f"));
    }

    @Test
    public void testPagination() throws SourceException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        Collection<SourceRecord> records = source.getRecords(2, 9, new ReportImpl());
        assertThat("correct number of records", records.size(), is(9));

        String allMappedRecordsString = sourceRecordsToString(records, mapper);
        assertThat("first record id is in NOT mapped records", allMappedRecordsString,
                not(containsString("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f")));
        assertThat("second record id is in mapped records", allMappedRecordsString,
                containsString("urn:uuid:1ef30a8b-876d-4828-9246-c37ab4510bbd"));
        assertThat("third last id is in mapped records", allMappedRecordsString,
                containsString("urn:uuid:a06af396-3105-442d-8b40-22b57a90d2f2"));
        assertThat("second last record id is not in mapped records", allMappedRecordsString,
                not(containsString("urn:uuid:ab42a8c4-95e8-4630-bf79-33e59241605a")));
        assertThat("last record id is in NOT mapped records", allMappedRecordsString,
                not(containsString("urn:uuid:e9330592-0932-474b-be34-c3a3bb67c7db")));
    }

    @Test
    public void testPaginationUpperBound() throws SourceException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));
        Collection<SourceRecord> records = source.getRecords(12, 100, new ReportImpl());
        assertThat("correct number of records", records.size(), is(1));

        String allMappedRecordsString = sourceRecordsToString(records, mapper);
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

    public static String sourceRecordsToString(Collection<SourceRecord> records, Mapper mapper) {
        return records.stream()
                .map(mapper::map)
                .map(r -> (BuilderRecord) r)
                .map(BuilderRecord::getBuilder)
                .map((xContentBuilder) -> {
                    try {
                        return xContentBuilder.string();
                    } catch (IOException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));
    }

    @Test
    public void testCswRecordsDirectoryWithMapping() throws IOException, SourceException {
        DirectorySource source = new DirectorySource(baseDirectory.resolve("csw"));

        Collection<SourceRecord> records = source.getRecords(1, 7, new ReportImpl());
        List<SinkRecord> mappedRecords = records.stream().map(mapper::map).collect(Collectors.toList());
        assertThat("all records mapped", mappedRecords.size(), is(7));

        Set<String> ids = mappedRecords.stream().map(SinkRecord::getId).collect(Collectors.toSet());
        assertThat("ids are contained in mapped records", ids, // some test IDs
                hasItems("urn:uuid:19887a8a-f6b0-4a63-ae56-7fba0e17801f",
                        "urn:uuid:6a3de50b-fa66-4b58-a0e6-ca146fdd18d4",
                        "urn:uuid:88247b56-4cbc-4df9-9860-db3f8042e357"));
    }

}
