package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.database.GuestDAO;
import com.hotel.scheduling_system.database.HousekeepingDAO;
import com.hotel.scheduling_system.database.ReservationDAO;
import com.hotel.scheduling_system.database.StaffDAO;
import com.hotel.scheduling_system.model.Guest;
import com.hotel.scheduling_system.model.HousekeepingTask;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.Staff;
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
    private final StaffDAO staffDAO;
    private final HousekeepingDAO housekeepingDAO;

    public Map<String, Object> onGenerateScheduleClicked() { return schedulingService.generateSchedule(); }
    public void onSaveScheduleClicked(Map<Room, List<Reservation>> assignments) { schedulingService.saveScheduleToDatabase(assignments); }
    public List<Guest> getAllGuests() { return guestDAO.getAllGuests(); }
    public void createNewReservation(int guestId, String roomType, LocalDate startDate, LocalDate endDate, String preferredView) {
        reservationDAO.addNewReservation(guestId, roomType, startDate, endDate, preferredView);
    }
    public int getOrCreateGuest(String fullName) { return guestDAO.getOrCreateGuest(fullName); }

    public List<HousekeepingTask> generateAndGetHousekeepingReport(LocalDate date, List<Integer> roomIdsToClean) {
        List<Staff> cleaners = staffDAO.getHousekeepingStaff();

        if (!cleaners.isEmpty() && roomIdsToClean != null && !roomIdsToClean.isEmpty()) {
            List<Integer> staffIds = cleaners.stream().map(Staff::id).toList();
            housekeepingDAO.assignTasksForDate(date, roomIdsToClean, staffIds);
        }

        return housekeepingDAO.getTasksForDate(date);
    }
}