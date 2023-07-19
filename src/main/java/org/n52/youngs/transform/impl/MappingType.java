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
package org.n52.youngs.transform.impl;

public enum MappingType {

    /**
     * Type for JsonNodes.
     */
    NODE("node"),
    /**
     * Type for dates.
     */
    DATE("date"),
    /**
     * Type for fixed strings.
     */
    STRING("string"),
    /**
     * Type for lists of strings.
     */
    LIST("list");

    private final String stringRepresentation;

    public final String getStringRepresentation() {
        return stringRepresentation;
    }

    MappingType(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    public static MappingType fromString(String type) {
        for (MappingType c : MappingType.values()) {
            if (c.getStringRepresentation().equalsIgnoreCase(type)) {
                return c;
            }
        }
        throw new IllegalArgumentException(type);
    }

    public static String toString(MappingType type) {
        return type.stringRepresentation;
    }

    @Override
    public String toString() {
        return toString(this);
    }

}
