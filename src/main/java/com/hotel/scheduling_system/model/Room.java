package com.hotel.scheduling_system.model;

public record Room(
        int id,            // The ID used in the genes array
        int roomNumber,    // The display number (e.g., 101)
        RoomType type
) {}