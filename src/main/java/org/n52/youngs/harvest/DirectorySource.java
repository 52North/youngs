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
package org.n52.youngs.harvest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.cluster.settings.Validator;
import org.elasticsearch.common.collect.Lists;
import org.n52.youngs.api.Record;
import org.n52.youngs.exception.SourceError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DirectorySource implements Source {

    private static final Logger log = LoggerFactory.getLogger(DirectorySource.class);

    private static final FileFilter DEFAULT_FILTER = (File pathname) -> true;

    private final Path directory;

    private Optional<List<Record>> records = Optional.empty();

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
    public Collection<Record> getRecords() {
        return readRecordsFromDirectory();
    }

    @Override
    public Collection<Record> getRecords(long startPosition, long maxRecords) {
        List<Record> sorted = readRecordsFromDirectory();
        int calculatedEnd = (int) (startPosition + maxRecords);
        return sorted.subList((int) (startPosition - 1), // java starts at 0
                Math.min(sorted.size(), calculatedEnd)); // end is exclusive
    }

    private File[] getFiles() {
        return this.directory.toFile().listFiles(filter);
    }

    private List<Record> readRecordsFromDirectory() {
        if (this.records.isPresent()) {
            return this.records.get();
        }

        List<Record> recs = Arrays.stream(getFiles())
                .map(file -> {
                    try {
                        Record record = readRecordFromFile(file);
                        log.trace("Parsed record: {}", record);
                        return record;
                    } catch (SAXException | IOException | ParserConfigurationException e) {
                        log.warn("Could not parse file {}: {} (turn on debug for full trace)", file, e.getMessage());
                        log.debug("Error reading file {}", file, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        this.records = Optional.of(recs);
        return this.records.get();
    }

    private Record readRecordFromFile(File f) throws ParserConfigurationException, SAXException, IOException {
        log.debug("Reading record from file {}", f);

        DocumentBuilder documentBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(f);

        Element elem = doc.getDocumentElement();
        elem.normalize();
        log.trace("Read docuemtn: {}", elem);

        NodeSourceRecord record = new NodeSourceRecord(elem);
        return record;
    }

}
