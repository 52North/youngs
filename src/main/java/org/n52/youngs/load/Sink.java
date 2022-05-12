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
package org.n52.youngs.load;

import java.util.Collection;
import org.n52.youngs.exception.SinkError;
import org.n52.youngs.transform.MappingConfiguration;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface Sink {

    /**
     * Do whatever is required for the sink to be used, such as testing connections, inserting schemas, ...
     *
     * @param mapping the mapping that contains the configuration for the sink, such as data types and field names
     * @return true if the sink is now ready to be used
     */
    public boolean prepare(MappingConfiguration mapping) throws SinkError;

    /**
     * @param record the record to store
     * @return true if record is stored
     * @throws SinkError on no-recoverable errors
     */
    public boolean store(SinkRecord record) throws SinkError;

    /**
     * @param records the records to store
     * @return true if _all_ records are stored
     * @throws SinkError on no-recoverable errors
     */
    public boolean store(Collection<SinkRecord> records) throws SinkError;

    /**
     * remove all traces of any loading that took or might have taken place for the provided mapping
     *
     * @param mapping the mapping providing the information that shall be cleared from the sink
     * @return false if there were problems with clearing, true otherwise
     */
    public boolean clear(MappingConfiguration mapping);

}
