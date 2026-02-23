package com.hotel.scheduling_system.service;

import com.hotel.scheduling_system.controller.PostProcessor;
import com.hotel.scheduling_system.database.ReservationDAO;
import com.hotel.scheduling_system.database.RoomDAO;
import com.hotel.scheduling_system.model.GeneticOptimizer;
import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.ScheduleSolution;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SchedulingService {

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;

    public SchedulingService(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
    }

    public Map<String, Object> generateSchedule() {
        List<Reservation> reservations = reservationDAO.getAllReservations();
        List<Room> rooms = roomDAO.getAllRooms();

        if (reservations.isEmpty() || rooms.isEmpty()) {
            throw new RuntimeException("Cannot generate schedule: No reservations or rooms found in DB.");
        }

        GeneticOptimizer optimizer = new GeneticOptimizer();
        ScheduleSolution bestSolution = optimizer.solve(reservations, rooms);

        PostProcessor processor = new PostProcessor();
        return processor.process(bestSolution, reservations, rooms);
    }

    // הפונקציה החדשה שעוברת על התוצאות ושומרת אותן
    public void saveScheduleToDatabase(Map<Room, List<Reservation>> assignments) {
        for (Map.Entry<Room, List<Reservation>> entry : assignments.entrySet()) {
            Room assignedRoom = entry.getKey();

            for (Reservation res : entry.getValue()) {
                // שומרים את השידוך בין מזהה ההזמנה למזהה החדר הפיזי
                reservationDAO.updateReservationRoom(res.id(), assignedRoom.getId());
            }
        }
    }
}