package com.hotel.scheduling_system.model;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ScheduleSolution {
    private final int[] genes; // Index = Reservation ID, Value = Assigned Room ID
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

    /**
     * פונקציית ה-Fitness המעודכנת: מונעת "חורים", מחשבת לפי לילות, ושומרת על Overbooking כגרוע מכל.
     */
    public void calculateFitness(List<Reservation> reservations, List<Room> rooms) {
        double penalties = 0;

        // --- מערכת המשקלים המאוזנת החדשה ---
        double W_HARD = 100000.0;                 // קנס קריטי על חפיפה - שום שנמוך לא יעקוף את זה
        double W_DOWNGRADE_PER_NIGHT = 2000.0;    // לקוח זועם - מחושב לפי כמות הלילות
        double W_UPGRADE_PER_NIGHT = 200.0;       // אובדן רווח קל משדרוג
        double W_FRAGMENTATION = 1000.0;          // יצירת חור בלתי ניתן למכירה של לילה אחד

        // מילון עזר שיעזור לנו לבדוק חורים: ממפה מזהה חדר לרשימת ההזמנות ששובצו בו
        Map<Integer, List<Reservation>> roomSchedules = new HashMap<>();
        for (Room room : rooms) {
            roomSchedules.put(room.getId(), new ArrayList<>());
        }

        // --- מעבר ראשון: בדיקת אילוצים על כל הזמנה בנפרד (חפיפות ושדרוגים/שנמוכים) ---
        for (int i = 0; i < reservations.size(); i++) {
            Reservation currentRes = reservations.get(i);
            int assignedRoomId = genes[i];

            Room assignedRoom = rooms.stream()
                    .filter(r -> r.getId() == assignedRoomId)
                    .findFirst()
                    .orElse(null);

            if (assignedRoom != null) {
                // הוספת ההזמנה לרשימת החדר לבדיקת חורים בהמשך
                roomSchedules.get(assignedRoomId).add(currentRes);

                // 1. SOFT CONSTRAINTS (שדרוג מול שנמוך לפי אורך שהייה)
                if (currentRes.roomType() != assignedRoom.getType()) {
                    int requestedRank = getRoomRank(currentRes.roomType());
                    int assignedRank = getRoomRank(assignedRoom.getType());
                    long nights = ChronoUnit.DAYS.between(currentRes.startDate(), currentRes.endDate());
                    // העיקרון שלך: Fail-Fast במקום לטאטא מתחת לשטיח
                    if (nights <= 0) {
                        throw new IllegalArgumentException(
                                "CRITICAL ERROR: Reservation ID " + currentRes.id() + " has invalid dates! " +
                                        "End date (" + currentRes.endDate() + ") cannot be before or equal to Start date (" + currentRes.startDate() + ")."
                        );
                    }

                    if (assignedRank < requestedRank) {
                        penalties += (W_DOWNGRADE_PER_NIGHT * nights * (requestedRank - assignedRank));
                    } else {
                        penalties += (W_UPGRADE_PER_NIGHT * nights * (assignedRank - requestedRank));
                    }
                }
            }

            // 2. HARD CONSTRAINT (חפיפת זמנים - כפילות הזמנות)
            for (int j = i + 1; j < reservations.size(); j++) {
                if (genes[i] == genes[j] && currentRes.overlaps(reservations.get(j))) {
                    penalties += W_HARD;
                }
            }
        }

        // --- מעבר שני: בדיקת Anti-Fragmentation (מניעת חורים בלוח השנה) ---
        for (List<Reservation> schedule : roomSchedules.values()) {
            if (schedule.size() > 1) {
                // מסדרים את כל ההזמנות של החדר לפי תאריך ההתחלה שלהן
                schedule.sort(Comparator.comparing(Reservation::startDate));

                for (int i = 0; i < schedule.size() - 1; i++) {
                    Reservation current = schedule.get(i);
                    Reservation next = schedule.get(i + 1);

                    // בודקים חורים רק אם ההזמנות לא חופפות (על חפיפה כבר קנסנו ב-W_HARD)
                    if (!current.overlaps(next)) {
                        long gap = ChronoUnit.DAYS.between(current.endDate(), next.startDate());
                        if (gap == 1) {
                            // מצאנו חור של יום אחד בדיוק! האלגוריתם יחטוף קנס כדי שינסה לסדר אותן צמודות
                            penalties += W_FRAGMENTATION;
                        }
                    }
                }
            }
        }

        this.fitness = 1_000_000.0 - penalties;
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