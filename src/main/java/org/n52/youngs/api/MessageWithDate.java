/*
 * Copyright 2015-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.youngs.api;

import org.joda.time.DateTime;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class MessageWithDate implements Comparable<MessageWithDate> {

    private final DateTime date;
    private final String message;
    private final Report.Level level;

    public MessageWithDate(DateTime date, String message) {
        this(date, message, Report.Level.INFO);
    }

    public MessageWithDate(DateTime date, String message, Report.Level level) {
        this.date = date;
        this.message = message;
        this.level = level;
    }

    @Override
    public int compareTo(MessageWithDate o) {
        return this.date.compareTo(o.date);
    }

    public DateTime getDate() {
        return date;
    }

    public String getMessage() {
        return message;
    }

    public Report.Level getLevel() {
        return level;
    }

    @Override
    public String toString() {
        return "MessageWithDate{" + "date=" + date + ", message=" + message + ", level=" + level + '}';
    }

}
