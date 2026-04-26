package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.ScheduleSolution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;


@Component
public class PostProcessor {

    // Define a clear, strongly-typed return object using a Java Record
    public record ProcessingResult(
            Map<Room, List<Reservation>> assignments,
            List<Reservation> unassigned,
            double fitnessScore
    ) {}

    /**
     * Processes the raw genetic algorithm solution to create a final, usable schedule.
     * It maps the assigned genes (room IDs) back to actual Room objects and checks for
     * hard constraints (like overlapping dates within the same room). If an assignment
     * causes a conflict or is invalid, the reservation is moved to the unassigned list.
     *
     * @param winningSolution the best schedule solution produced by the genetic algorithm
     * @param allReservations the list of all reservations to be scheduled
     * @param rooms the list of all available rooms in the hotel
     * @return a ProcessingResult containing the final valid room assignments, a list of unassigned reservations, and the fitness score
     */
    public ProcessingResult process(ScheduleSolution winningSolution, List<Reservation> allReservations, List<Room> rooms) {

        // Safety check to prevent indexing errors
        int[] genes = winningSolution.getGenes();
        if (genes.length != allReservations.size()) {
            throw new IllegalArgumentException("Mismatch between number of genes and reservations");
        }

        List<Reservation> unassignedList = new ArrayList<>();
        Map<Room, List<Reservation>> roomAssignments = new HashMap<>();
        Map<Integer, Room> roomMap = new HashMap<>();

        // Initialize maps to maintain O(1) performance
        for (Room room : rooms) {
            roomAssignments.put(room, new ArrayList<>());
            roomMap.put(room.id(), room);
        }

        for (int i = 0; i < allReservations.size(); i++) {
            Reservation currentRes = allReservations.get(i);
            int assignedRoomId = genes[i];
            Room assignedRoom = roomMap.get(assignedRoomId);

            // Handle invalid room assignments
            if (assignedRoom == null) {
                unassignedList.add(currentRes);
                continue;
            }

            List<Reservation> existingRoomSchedule = roomAssignments.get(assignedRoom);
            boolean hasConflict = false;

            // Check for overlaps within the specific room
            for (Reservation other : existingRoomSchedule) {
                if (currentRes.overlaps(other)) {
                    hasConflict = true;
                    break;
                }
            }

            if (hasConflict) {
                unassignedList.add(currentRes);
            } else {
                existingRoomSchedule.add(currentRes);
            }
        }
        return new ProcessingResult(roomAssignments, unassignedList, winningSolution.getFitness());
    }
}