package com.hotel.scheduling_system.model;

import lombok.Getter;

@Getter
public enum RoomType {
    SINGLE(1, 100.0),
    DOUBLE(2, 150.0),
    SUITE(3, 300.0);

    private final int rank;
    private final double basePrice; // Added standard price for the room type

    RoomType(int rank, double basePrice) {
        this.rank = rank;
        this.basePrice = basePrice;
    }
}