package com.hotel.scheduling_system.model;

import lombok.Getter;

@Getter
public enum RoomType {
    SINGLE(1),
    DOUBLE(2),
    SUITE(3);

    private final int rank;

    RoomType(int rank) {
        this.rank = rank;
    }
}