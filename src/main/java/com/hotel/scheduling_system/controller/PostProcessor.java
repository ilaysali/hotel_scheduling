package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.ScheduleSolution;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostProcessor {

    public Map<String, Object> process(ScheduleSolution winningSolution, List<Reservation> allReservations, List<Room> rooms) {
        Map<String, Object> result = new HashMap<>();
        List<Reservation> unassignedList = new ArrayList<>();
        Map<Room, List<Reservation>> roomAssignments = new HashMap<>();

        // Create HashMap for rooms to maintain O(1) retrieval performance
        Map<Integer, Room> roomMap = new HashMap<>();
        for (Room room : rooms) {
            roomAssignments.put(room, new ArrayList<>());
            roomMap.put(room.id(), room);
        }

        int[] genes = winningSolution.getGenes();

        // Loop using the simple index i that maps the reservations list to the genes array
        for (int i = 0; i < allReservations.size(); i++) {

            // O(1) retrieval (assuming it's an ArrayList)
            Reservation currentRes = allReservations.get(i);

            // The gene at index i contains the ID of the room assigned to the reservation at index i
            int assignedRoomId = genes[i];

            // Retrieve the assigned room from the dictionary in O(1)
            Room assignedRoom = roomMap.get(assignedRoomId);

            if (assignedRoom == null) {
                unassignedList.add(currentRes);
                continue;
            }

            boolean hasConflict = false;
            List<Reservation> existingRoomSchedule = roomAssignments.get(assignedRoom);

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

        result.put("ASSIGNMENTS", roomAssignments);
        result.put("UNASSIGNED", unassignedList);
        result.put("FITNESS_SCORE", winningSolution.getFitness());

        return result;
    }
}