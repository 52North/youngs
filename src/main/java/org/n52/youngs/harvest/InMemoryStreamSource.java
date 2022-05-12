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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class InMemoryStreamSource extends InputStreamSource {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryStreamSource.class.getName());

    private final InputStream stream;

    public InMemoryStreamSource(InputStream stream) {
        this.stream = stream;
    }

    @Override
    protected InputStream resolveSourceInputStream() throws IOException {
        return this.stream;
    }

    @Override
    public URL getEndpoint() {
        try {
            return new URL("file:///dev/null");
        } catch (MalformedURLException ex) {
            LOG.warn(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    protected String resolveProtocolIdentifier() {
        return "internal-stream";
    }

}
