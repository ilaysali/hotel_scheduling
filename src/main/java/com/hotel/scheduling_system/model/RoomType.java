package com.hotel.scheduling_system.model;

public enum RoomType {
    SINGLE,
    DOUBLE,
    SUITE,
    DELUXE;

    /**
     * Optional utility: Returns the enum constant from a string,
     * ignoring case to prevent errors from DB entries.
     */
    public static RoomType fromString(String text) {
        for (RoomType b : RoomType.values()) {
            if (b.name().equalsIgnoreCase(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}
