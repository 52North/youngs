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

package org.n52.youngs.impl;

import org.joda.time.DateTime;

/**
 *
 * @author <a href="mailto:m.rieke@52north.org">Matthes Rieke</a>
 */
public class MessageWithDate implements Comparable<MessageWithDate> {

    private final DateTime date;
    private final String message;

    public MessageWithDate(DateTime date, String message) {
        this.date = date;
        this.message = message;
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

    @Override
    public String toString() {
        return "MessageWithDate{" + "date=" + date + ", message=" + message + '}';
    }

}
