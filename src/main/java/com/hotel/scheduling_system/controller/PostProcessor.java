package com.hotel.scheduling_system.controller;

import com.hotel.scheduling_system.model.Reservation;
import com.hotel.scheduling_system.model.ScheduleSolution;
import java.util.*;

public class PostProcessor {

    public Map<String, List<Reservation>> filterConflicts(ScheduleSolution winningSolution, List<Reservation> allReservations) {
        Map<String, List<Reservation>> result = new HashMap<>();
        List<Reservation> validAssignments = new ArrayList<>();
        List<Reservation> unassignedList = new ArrayList<>();

        int[] genes = winningSolution.getGenes();
        // Track which rooms are occupied per date to find physical conflicts
        Map<Integer, List<Reservation>> roomSchedules = new HashMap<>();

        for (int i = 0; i < allReservations.size(); i++) {
            Reservation currentRes = allReservations.get(i);
            int assignedRoomId = genes[i]; // Room number from the winning chromosome

            boolean hasConflict = false;
            List<Reservation> existingRoomSchedule = roomSchedules.getOrDefault(assignedRoomId, new ArrayList<>());

            for (Reservation other : existingRoomSchedule) {
                if (currentRes.overlaps(other)) { // Use our custom overlap logic
                    hasConflict = true;
                    break;
                }
            }

            if (hasConflict) {
                unassignedList.add(currentRes); // Move to "To-be-handled"
            } else {
                validAssignments.add(currentRes);
                existingRoomSchedule.add(currentRes);
                roomSchedules.put(assignedRoomId, existingRoomSchedule);
            }
        }

        result.put("VALID", validAssignments);
        result.put("UNASSIGNED", unassignedList);
        return result;
    }
}