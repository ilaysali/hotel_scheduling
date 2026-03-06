package com.hotel.scheduling_system.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Reservation(
        int id,
        String guestName,
        LocalDate startDate,
        LocalDate endDate,
        RoomType roomType,
        long nights
) {

    public Reservation(int id, String guestName, LocalDate startDate, LocalDate endDate, RoomType roomType) {
        this(id, guestName, startDate, endDate, roomType, ChronoUnit.DAYS.between(startDate, endDate));
    }

    public boolean overlaps(Reservation other) {
        return this.startDate.isBefore(other.endDate) && other.startDate.isBefore(this.endDate);
    }
}