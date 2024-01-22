/*
 * Copyright 2015-2024 52°North Spatial Information Research GmbH
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
package org.n52.youngs.load.impl;

import co.elastic.clients.json.JsonData;
import com.google.common.base.MoreObjects;
import java.util.Optional;
import org.n52.youngs.load.SinkRecord;

/**
 *
 * @author <a href="mailto:d.nuest@52north.org">Daniel NÃ¼st</a>
 */
public class BuilderRecord implements SinkRecord {

    private final JsonData data;
    private Optional<String> id = Optional.empty();

    public BuilderRecord(String id, JsonData data) {
        this.id = Optional.ofNullable(id);
        this.data = data;
    }

    public BuilderRecord(JsonData data) {
        this(null, data);
    }

    public JsonData getData() {
        return this.data;
    }

    @Override
    public String getId() {
        return id.get();
    }

    @Override
    public boolean hasId() {
        return id.isPresent();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", getId())
                .toString();
    }

}
