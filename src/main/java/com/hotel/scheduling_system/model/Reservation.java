package com.hotel.scheduling_system.model;

import java.time.LocalDate;

/**
 * Immutable representation of a guest booking.
 */
public record Reservation(
        int id,
        LocalDate startDate,
        LocalDate endDate,
        RoomType roomType
) {
    /**
     * Compact Constructor: Validates that the stay is logical.
     */
    public Reservation {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date for reservation: " + id);
        }
    }

    /**
     * Heart of the fitness check: Returns true if date ranges overlap.
     * Logic: Two ranges overlap if each starts before the other ends.
     */
    public boolean overlaps(Reservation other) {
        // Standard interval overlap logic: (StartA < EndB) AND (EndA > StartB)
        // We use the "hotel rule": endDate is exclusive (check-out day).
        return this.startDate.isBefore(other.endDate) &&
                other.startDate.isBefore(this.endDate);
    }
}