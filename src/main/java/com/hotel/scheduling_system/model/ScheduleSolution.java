package com.hotel.scheduling_system.model;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ScheduleSolution {
    private final int[] genes;
    private double fitness;
    private final Random random = new Random();

    public ScheduleSolution(int numReservations) {
        this.genes = new int[numReservations];
    }

    public ScheduleSolution(int[] genes) {
        this.genes = genes.clone();
    }

    private int getRoomRank(RoomType type) {
        return switch (type.name()) {
            case "SINGLE" -> 1;
            case "DOUBLE" -> 2;
            case "SUITE" -> 3;
            default -> 0;
        };
    }

    public void calculateFitness(List<Reservation> reservations, List<Room> rooms) {
        double penalties = 0;
        double bonuses = 0;

        double W_HARD = 100000.0;
        double W_DOWNGRADE_PER_NIGHT = 2000.0;
        double W_UPGRADE_PER_NIGHT = 200.0;
        double W_FRAGMENTATION = 1000.0;
        double B_PERFECT_MATCH = 500.0;

        // אופטימיזציה 1: מילון חדרים מהיר כדי להימנע מחיפושי Stream איטיים
        Map<Integer, Room> roomMap = new HashMap<>();
        Map<Integer, List<Reservation>> roomSchedules = new HashMap<>();

        for (Room room : rooms) {
            roomMap.put(room.getId(), room);
            roomSchedules.put(room.getId(), new ArrayList<>());
        }

        // --- מעבר ראשון: בניית הלוז לכל חדר ובדיקת אילוצים רכים (שדרוגים) ב-O(N) ---
        for (int i = 0; i < reservations.size(); i++) {
            Reservation currentRes = reservations.get(i);
            int assignedRoomId = genes[i];

            Room assignedRoom = roomMap.get(assignedRoomId); // שליפה ב-O(1) במקום O(N)

            if (assignedRoom != null) {
                roomSchedules.get(assignedRoomId).add(currentRes);

                if (currentRes.roomType() != assignedRoom.getType()) {
                    int requestedRank = getRoomRank(currentRes.roomType());
                    int assignedRank = getRoomRank(assignedRoom.getType());

                    // טיפ: אם תוכל להוסיף את מספר הלילות למחלקה Reservation, תוכל לחסוך את הקריאה הזו לחלוטין!
                    long nights = currentRes.nights();

                    if (nights <= 0) {
                        throw new IllegalArgumentException("CRITICAL ERROR: Invalid dates for Reservation ID " + currentRes.id());
                    }

                    if (assignedRank < requestedRank) {
                        penalties += (W_DOWNGRADE_PER_NIGHT * nights * (requestedRank - assignedRank));
                    } else {
                        penalties += (W_UPGRADE_PER_NIGHT * nights * (assignedRank - requestedRank));
                    }
                }
            }
        }

        // --- מעבר שני: בדיקת חפיפות (HARD CONSTRAINT) יחד עם חורים ורצפים ---
        // אופטימיזציה 2: חסכנו את הלולאה הכפולה האיומה!
        for (List<Reservation> schedule : roomSchedules.values()) {
            if (schedule.size() > 1) {
                // מיון פעם אחת
                schedule.sort(Comparator.comparing(Reservation::startDate));

                for (int i = 0; i < schedule.size() - 1; i++) {
                    Reservation current = schedule.get(i);
                    Reservation next = schedule.get(i + 1);

                    // בגלל שהמערך ממוין, מספיק לבדוק חפיפה עם הבא בתור! O(1) לפעולה
                    if (current.overlaps(next)) {
                        penalties += W_HARD;
                    } else {
                        // אם אין חפיפה, בודקים רצפים וחורים
                        long gap = ChronoUnit.DAYS.between(current.endDate(), next.startDate());
                        if (gap == 1) {
                            penalties += W_FRAGMENTATION;
                        } else if (gap == 0) {
                            bonuses += B_PERFECT_MATCH;
                        }
                    }
                }
            }
        }

        this.fitness = 1_000_000.0 - penalties + bonuses;
    }

    public void mutate(double probability, List<Room> rooms) {
        for (int i = 0; i < genes.length; i++) {
            if (random.nextDouble() < probability) {
                Room randomRoom = rooms.get(random.nextInt(rooms.size()));
                genes[i] = randomRoom.getId();
            }
        }
    }

    public int[] getGenes() { return genes; }
    public double getFitness() { return fitness; }
}