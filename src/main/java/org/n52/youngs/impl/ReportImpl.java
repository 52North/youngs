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
package org.n52.youngs.impl;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.joda.time.DateTime;
import org.n52.youngs.api.Report;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel Nüst</a>
 */
public class ReportImpl implements Report {

    private final Collection<String> added = Lists.newArrayList();

    private final Map<String, String> failed = Maps.newHashMap();

    private final Map<DateTime, String> messages = Maps.newHashMap();

    @Override
    public int getNumberOfRecordsAdded() {
        return getNumberOfRecordsSuccesful();
    }

    @Override
    public int getNumberOfRecordsSuccesful() {
        return added.size();
    }

    @Override
    public int getNumberOfRecordsFailed() {
        return failed.size();
    }

    @Override
    public void addSuccessfulRecord(String id) {
        added.add(id);
    }

    @Override
    public void addFailedRecord(String id, String reason) {
        failed.put(id, reason);
    }

    public void addFailedRecord(String id) {
        failed.put(id, "");
    }

    @Override
    public Collection<String> getAddedIds() {
        return added;
    }

    @Override
    public Map<String, String> getFailedIds() {
        return failed;
    }

    @Override
    public void addMessage(String message) {
        this.messages.put(new DateTime(), message);
    }

    @Override
    public Map<LocalTime, String> getMessages() {
        Map<LocalTime, String> result = Maps.newHashMap();

        this.messages.keySet().forEach(dt -> {
            result.put(LocalTime.of(dt.getHourOfDay(),
                    dt.getMinuteOfHour(),
                    dt.getSecondOfMinute(),
                    dt.getMillisOfSecond()*1000), this.messages.get(dt));
        });

        return result;
    }

    public Map<DateTime, String> getMessagesDateTime() {
        return Collections.unmodifiableMap(this.messages);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("### Report ###\n");
        sb.append(" Added: ").append(getNumberOfRecordsAdded()).append("\n");
        sb.append(" Failed: ").append(getNumberOfRecordsFailed()).append("\n").append("\n");
        sb.append(" Added IDs: ").append(Joiner.on(", ").join(added)).append("\n");
        sb.append(" Faild IDs: ").append(Joiner.on(", ").withKeyValueSeparator(": ").join(failed)).append("\n");
        sb.append(" Messages: ").append(Joiner.on("; ").withKeyValueSeparator(": ").join(messages)).append("\n");

        return sb.toString();
    }



}
