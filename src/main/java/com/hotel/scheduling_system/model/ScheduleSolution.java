package com.hotel.scheduling_system.model;

import lombok.Getter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ScheduleSolution {
    @Getter
    private final int[] genes;
    @Getter
    private double fitness;
    private final Random random = new Random();

    public ScheduleSolution(int numReservations) {
        this.genes = new int[numReservations];
    }

    public ScheduleSolution(int[] genes) {
        this.genes = genes.clone();
    }

    public void calculateFitness(List<Reservation> reservations, List<Room> rooms) {
        double penalties = 0;
        double bonuses = 0;

        double W_HARD = 100000.0;
        double W_DOWNGRADE_PER_NIGHT = 2000.0;
        double W_UPGRADE_PER_NIGHT = 200.0;
        double W_FRAGMENTATION = 1000.0;
        double B_PERFECT_MATCH = 500.0;

        Map<Integer, Room> roomMap = new HashMap<>();
        Map<Integer, List<Reservation>> roomSchedules = new HashMap<>();

        for (Room room : rooms) {
            roomMap.put(room.id(), room);
            roomSchedules.put(room.id(), new ArrayList<>());
        }

        // First pass: Build schedule per room and check soft constraints
        for (int i = 0; i < reservations.size(); i++) {
            Reservation currentRes = reservations.get(i);
            int assignedRoomId = genes[i];

            Room assignedRoom = roomMap.get(assignedRoomId);

            if (assignedRoom != null) {
                roomSchedules.get(assignedRoomId).add(currentRes);

                if (currentRes.roomType() != assignedRoom.type()) {
                    int requestedRank = currentRes.roomType().getRank();
                    int assignedRank = assignedRoom.type().getRank();
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

        // Second pass: Checks overlaps gaps and sequences
        for (List<Reservation> schedule : roomSchedules.values()) {
            if (schedule.size() > 1) {
                schedule.sort(Comparator.comparing(Reservation::startDate));

                for (int i = 0; i < schedule.size() - 1; i++) {
                    Reservation current = schedule.get(i);
                    Reservation next = schedule.get(i + 1);

                    // Because array is sorted, we only need to check overlap with the next one! O(1) operation
                    if (current.overlaps(next)) {
                        penalties += W_HARD;
                    } else {
                        // If no overlap, check sequences and gaps
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
                genes[i] = randomRoom.id();
            }
        }
    }
}