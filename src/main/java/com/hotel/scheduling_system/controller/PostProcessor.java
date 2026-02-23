package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.Room;
import com.hotel.scheduling_system.model.ScheduleSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostProcessor {

    // Upgraded to accept the Rooms list and return a Map<String, Object>
    public Map<String, Object> process(ScheduleSolution winningSolution, List<Reservation> allReservations, List<Room> rooms) {
        Map<String, Object> result = new HashMap<>();
        List<Reservation> unassignedList = new ArrayList<>();

        // This links the Physical Room to the List of Reservations assigned to it
        Map<Room, List<Reservation>> roomAssignments = new HashMap<>();

        // Initialize a lane for every room, even if it's empty
        for (Room room : rooms) {
            roomAssignments.put(room, new ArrayList<>());
        }

        int[] genes = winningSolution.getGenes();

        for (int i = 0; i < allReservations.size(); i++) {
            Reservation currentRes = allReservations.get(i);
            int assignedRoomId = genes[i];

            Room assignedRoom = rooms.stream().filter(r -> r.getId() == assignedRoomId).findFirst().orElse(null);

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
        return result;
    }
}