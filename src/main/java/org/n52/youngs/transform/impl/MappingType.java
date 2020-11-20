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
    STRING("string");

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
