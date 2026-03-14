package com.hotel.scheduling_system.model;

import lombok.Getter;

@Getter
public enum RoomType {
    SINGLE(1, 100.0, "LIGHTGREEN"),
    DOUBLE(2, 150.0, "LIGHTBLUE"),
    SUITE(3, 300.0, "GOLD");

    private final int rank;
    private final double basePrice; // Added standard price for the room type
    private final String colorName; // Added standard color for the room type

    RoomType(int rank, double basePrice, String colorName) {
        this.rank = rank;
        this.basePrice = basePrice;
        this.colorName = colorName;
    }
}