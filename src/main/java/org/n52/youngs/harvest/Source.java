/*
 * Copyright 2015-2021 52°North Initiative for Geospatial Open Source
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

import com.google.common.collect.ImmutableList;
import java.net.URL;
import java.util.Collection;
import org.n52.youngs.api.Report;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface Source {

    public static final String DEFAULT_TYPE_NAME = "csw:Record";

    public static final Collection<String> DEFAULT_NAMESPACES = ImmutableList.of("http://www.opengis.net/cat/csw/2.0.2");

    public static final String DEFAULT_OUTPUT_SCHEMA = "http://www.opengis.net/cat/csw/2.0.2";

    public URL getEndpoint();

    /**
     *
     * @return the number of records found in a source, or Long.MIN_VALUE if there were errors.
    */
    public long getRecordCount();

    public Collection<SourceRecord> getRecords(Report report) throws SourceException;

    public Collection<SourceRecord> getRecords(long startPosition, long maxRecords, Report report) throws SourceException;

}
