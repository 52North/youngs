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
package org.n52.youngs.impl;

import org.n52.youngs.api.MessageWithDate;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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

    private final Collection<MessageWithDate> messages = Lists.newArrayList();
    private int identifiedRecordCount;

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
        this.messages.add(new MessageWithDate(new DateTime(), message));
    }

    @Override
    public void addMessage(String message, Level level) {
        this.messages.add(new MessageWithDate(new DateTime(), message, level));
    }

    public void addMessageWithDate(MessageWithDate msg) {
        this.messages.add(msg);
    }

    @Override
    public Collection<MessageWithDate> getMessages() {
        return Collections.unmodifiableCollection(this.messages);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("### Report ###\n");
        sb.append(" Added: ").append(getNumberOfRecordsAdded()).append("\n");
        sb.append(" Failed: ").append(getNumberOfRecordsFailed()).append("\n").append("\n");
        sb.append(" Added IDs: ").append(Joiner.on(", ").join(added)).append("\n");
        sb.append(" Faild IDs: ").append(Joiner.on(", ").withKeyValueSeparator(": ").join(failed)).append("\n");
        sb.append(" Messages: ").append(Joiner.on("; ").join(messages)).append("\n");

        return sb.toString();
    }

    @Override
    public int getIdentifiedRecordCount() {
        return this.identifiedRecordCount;
    }

    public void setIdentifiedRecordCount(int identifiedRecordCount) {
        this.identifiedRecordCount = identifiedRecordCount;
    }



}
