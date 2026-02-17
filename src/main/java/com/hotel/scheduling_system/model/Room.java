package com.hotel.scheduling_system.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Room {
    private int id;            // The ID used in the genes array
    private int roomNumber;    // The display number (e.g., 101)
    private RoomType type;
    private double price;
    private int floor;

    /**
     * Simplified constructor for initial setup
     */
    public Room(int id, int roomNumber, RoomType type) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.type = type;
    }
}
