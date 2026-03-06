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

        // יצירת HashMap לחדרים - שומר על ביצועי O(1) בשליפת החדר
        Map<Integer, Room> roomMap = new HashMap<>();
        for (Room room : rooms) {
            roomAssignments.put(room, new ArrayList<>());
            roomMap.put(room.getId(), room);
        }

        int[] genes = winningSolution.getGenes();

        // חוזרים לרוץ על האינדקס הפשוט i שמתאים בין הרשימה למערך הגנים
        for (int i = 0; i < allReservations.size(); i++) {

            // שליפה ב-O(1) (בהנחה שזה ArrayList)
            Reservation currentRes = allReservations.get(i);

            // הגן במקום ה-i מכיל את ה-ID של החדר שהוקצה להזמנה במקום ה-i
            int assignedRoomId = genes[i];

            // שליפת החדר המוקצה מתוך המילון ב-O(1)
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