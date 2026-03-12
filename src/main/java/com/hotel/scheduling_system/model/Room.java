package com.hotel.scheduling_system.model;

public record Room(
        int id,            // The ID used in the genes array
        int roomNumber,    // The display number (e.g., 101)
        RoomType type,
        String view        // Added for the view preference (e.g., "Sea", "Pool", "City")
) {
    // Helper method to resolve the missing method error in ScheduleSolution
    public String getView() {
        return this.view;
    }
}