/*
 * Copyright 2015-2019 52°North Initiative for Geospatial Open Source
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.n52.youngs.exception.SourceError;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class FileSource extends InputStreamSource {

    private final Path file;
    private DocumentBuilderFactory docBuilderFactory;

    public FileSource(Path file) {
        Objects.requireNonNull(file);
        if (!Files.exists(file)) {
            throw new IllegalStateException("File does not exist: "+ file);
        }
        this.file = file;
        this.docBuilderFactory = DocumentBuilderFactory.newInstance();
        this.docBuilderFactory.setNamespaceAware(true);
    }

    @Override
    public URL getEndpoint() {
        try {
            return this.file.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new SourceError(e, "Could not create URL from directory %s", this.file);
        }
    }

    @Override
    protected InputStream resolveSourceInputStream() throws IOException {
        return Files.newInputStream(this.file);
    }

    @Override
    protected String resolveProtocolIdentifier() {
        return this.file.getFileName().toString();
    }



}
