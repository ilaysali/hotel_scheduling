package com.hotel.scheduling_system.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Reservation(
        int id,
        String guestName,
        LocalDate startDate,
        LocalDate endDate,
        RoomType roomType,
        long nights,
        String preferredView // Added for the soft constraint
) {

    // Constructor that includes the preferred view
    public Reservation(int id, String guestName, LocalDate startDate, LocalDate endDate, RoomType roomType, String preferredView) {
        this(id, guestName, startDate, endDate, roomType, ChronoUnit.DAYS.between(startDate, endDate), preferredView);
    }

    // constructor for defaults preferredView to null)
    public Reservation(int id, String guestName, LocalDate startDate, LocalDate endDate, RoomType roomType) {
        this(id, guestName, startDate, endDate, roomType, ChronoUnit.DAYS.between(startDate, endDate), null);
    }

    public boolean overlaps(Reservation other) {
        return this.startDate.isBefore(other.endDate) && other.startDate.isBefore(this.endDate);
    }

    public String getPreferredView() {
        return this.preferredView;
    }
}