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
package org.n52.youngs.harvest;

import com.google.common.base.MoreObjects;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.n52.youngs.api.Report;
import org.n52.youngs.exception.SourceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DirectorySource implements Source {

    private static final Logger log = LoggerFactory.getLogger(DirectorySource.class);

    private static final FileFilter DEFAULT_FILTER = (File pathname) -> true;

    private final Path directory;

    private Optional<List<SourceRecord>> records = Optional.empty();

    private FileFilter filter;

    private DocumentBuilderFactory docBuilderFactory;

    public DirectorySource(Path directory) {
        this(directory, DEFAULT_FILTER);
    }

    public DirectorySource(Path directory, FileFilter filter) {
        Objects.nonNull(directory);
        if (!directory.toFile().isDirectory() && !directory.toFile().exists()) {
            throw new IllegalArgumentException(String.format("Provided path %s is not an (existing) directory", directory));
        }

        Objects.nonNull(filter);
        this.filter = filter;
        this.directory = directory;

        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
    }

    @Override
    public URL getEndpoint() {
        try {
            return directory.toFile().toURI().toURL();
        } catch (MalformedURLException e) {
            throw new SourceError(e, "Could not create URL from directory %s", directory);
        }
    }

    @Override
    public long getRecordCount() {
        return getFiles().length;
    }

    @Override
    public Collection<SourceRecord> getRecords(Report report) throws SourceException {
        return readRecordsFromDirectory();
    }

    @Override
    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report) throws SourceException {
        List<SourceRecord> sorted = readRecordsFromDirectory();
        int calculatedBegin = (int) (startPosition - 1);
        int calculatedEnd = (int) Math.min(sorted.size(), (calculatedBegin + maxRecords));
        log.trace("Mapped subsetting from start={} max={} to [{}, {}[(exclusive) for {} records",
                startPosition, maxRecords, calculatedBegin, calculatedEnd, sorted.size());
        return sorted.subList(calculatedBegin, // java starts at 0
                calculatedEnd); // end is exclusive
    }

    private File[] getFiles() {
        return this.directory.toFile().listFiles(filter);
    }

    private List<SourceRecord> readRecordsFromDirectory() throws SourceException {
        if (this.records.isPresent()) {
            return this.records.get();
        }

        List<SourceException> exceptions = new ArrayList<>();

        List<SourceRecord> recs = Arrays.stream(getFiles())
                .map(file -> {
                    try {
                        SourceRecord record = readRecordFromFile(file);
                        log.trace("Parsed record: {}", record);
                        return record;
                    } catch (SAXException | IOException | ParserConfigurationException e) {
                        log.warn("Could not parse file {}: {} (turn on debug for full trace)", file, e.getMessage());
                        log.debug("Error reading file {}", file, e);
                        exceptions.add(new SourceException("Issue with file '" + file.getName() + "': " + e.getMessage(), e));
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }

        this.records = Optional.of(recs);
        return this.records.get();
    }

    private SourceRecord readRecordFromFile(File f) throws ParserConfigurationException, SAXException, IOException {
        log.debug("Reading record from file {}", f);

        DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
        Charset cs = Charset.forName("utf-8");
        Document doc = documentBuilder.parse(new InputSource(new InputStreamReader(new FileInputStream(f), cs)));

        Element elem = doc.getDocumentElement();
        elem.normalize();
        log.trace("Read document: {}", elem);

        NodeSourceRecord record = new NodeSourceRecord(elem, f.getName());
        return record;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("directory", this.directory)
                .add("filter", this.filter)
                .omitNullValues()
                .toString();
    }



}
