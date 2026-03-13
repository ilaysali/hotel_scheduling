package com.hotel.scheduling_system.dto;
import java.time.LocalDate;
public record ReservationDTO(int id, int guestId, LocalDate startDate, LocalDate endDate, String roomType, String preferredView) {}