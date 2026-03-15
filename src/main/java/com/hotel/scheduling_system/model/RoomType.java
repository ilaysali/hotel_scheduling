package com.hotel.scheduling_system.model;

import lombok.Getter;

@Getter
public enum RoomType {
    SINGLE(1, "LIGHTGREEN"),
    DOUBLE(2, "LIGHTBLUE"),
    SUITE(3, "GOLD");

    private final int rank;
    private final String colorName; // Added standard color for the room type

    RoomType(int rank, String colorName) {
        this.rank = rank;
        this.colorName = colorName;
    }
}