package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.controller.PostProcessor.ProcessingResult;
import com.hotel.scheduling_system.database.GuestDAO;
import com.hotel.scheduling_system.database.ReservationDAO;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.service.SchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppController {

    private final SchedulingService schedulingService;
    private final GuestDAO guestDAO;
    private final ReservationDAO reservationDAO;

    public ProcessingResult onGenerateScheduleClicked() {
        return schedulingService.generateSchedule();
    }

    public void onSaveScheduleClicked(Map<Room, List<Reservation>> assignments) {
        schedulingService.saveScheduleToDatabase(assignments);
    }

    public List<Guest> getAllGuests() {
        return guestDAO.getAllGuests();
    }

    public void createNewReservation(int guestId, String roomType, LocalDate startDate, LocalDate endDate, String preferredView) {
        reservationDAO.addNewReservation(guestId, roomType, startDate, endDate, preferredView);
    }

    public int getOrCreateGuest(String fullName) {
        return guestDAO.getOrCreateGuest(fullName);
    }

    public List<Reservation> getAllReservations() {
        // Fetching all reservations from the database via DAO
        return reservationDAO.getAllReservations();
    }
}