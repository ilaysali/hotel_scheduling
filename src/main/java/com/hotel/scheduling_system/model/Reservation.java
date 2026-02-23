package com.hotel.scheduling_system.model;

import java.time.LocalDate;

public record Reservation(
        int id,
        String guestName, // <-- השדה החדש שהוספנו!
        LocalDate startDate,
        LocalDate endDate,
        RoomType roomType
) {
    // הפעולה לבדיקת חפיפה (כפי שהגדרת במסמך)
    public boolean overlaps(Reservation other) {
        return this.startDate.isBefore(other.endDate) && other.startDate.isBefore(this.endDate);
    }
}