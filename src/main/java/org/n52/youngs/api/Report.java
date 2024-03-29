/*
 * Copyright 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.youngs.api;

import java.util.Collection;
import java.util.Map;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public interface Report {

    public enum Level {
        INFO,
        WARN,
        ERROR
    }

    /**
     * @deprecated use @see(#getNumberOfRecordsSuccesful()) instead
     * @return the number of affected records
     */
    @Deprecated
    public int getNumberOfRecordsAdded();

    public int getNumberOfRecordsSuccesful();

    public int getNumberOfRecordsFailed();

    public void addSuccessfulRecord(String id);

    public void addFailedRecord(String id, String reason);

    public Collection<String> getAddedIds();

    /**
     *
     * @return a map from ID to failure reason description.
     */
    public Map<String, String> getFailedIds();

    default void addMessage(String message) {
        addMessage(message, Level.INFO);
    };

    public void addMessage(String message, Level level);

    public Collection<MessageWithDate> getMessages();

    /**
     * return the total count of records identified for the source (e.g. files)
     * @return the total number
     */
    public int getIdentifiedRecordCount();

}
