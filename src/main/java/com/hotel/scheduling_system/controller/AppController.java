package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.database.GuestDAO;
import com.hotel.scheduling_system.database.MockDataGenerator;
import com.hotel.scheduling_system.database.ReservationDAO;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.service.SchedulingService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class AppController {

    private final SchedulingService schedulingService;
    private final MockDataGenerator mockDataGenerator;
    private final GuestDAO guestDAO;
    private final ReservationDAO reservationDAO;

    public AppController(SchedulingService schedulingService, MockDataGenerator mockDataGenerator, GuestDAO guestDAO, ReservationDAO reservationDAO) {
        this.schedulingService = schedulingService;
        this.mockDataGenerator = mockDataGenerator;
        this.guestDAO = guestDAO;
        this.reservationDAO = reservationDAO;
    }

    public Map<String, Object> onGenerateScheduleClicked() {
        return schedulingService.generateSchedule();
    }

    public void onSaveScheduleClicked(Map<Room, List<Reservation>> assignments) {
        schedulingService.saveScheduleToDatabase(assignments);
    }

    public void onLoadMockDataClicked() {
        mockDataGenerator.resetAndGenerate();
    }

    public List<Guest> getAllGuests() {
        return guestDAO.getAllGuests();
    }

    public void createNewReservation(int guestId, String roomType, LocalDate startDate, LocalDate endDate) {
        reservationDAO.addNewReservation(guestId, roomType, startDate, endDate);
    }

    // הנה הפעולה שחסרה לך ומונעת את הקומפילציה!
    public int getOrCreateGuest(String fullName) {
        return guestDAO.getOrCreateGuest(fullName);
    }
}