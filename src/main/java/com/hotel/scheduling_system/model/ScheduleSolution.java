package com.hotel.scheduling_system.model;

import lombok.Getter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class ScheduleSolution {
    @Getter
    private final int[] genes;
    @Getter
    private double fitness;
    private final Random random = new Random();

    // OPTIMIZATION: Reuse these structures instead of creating them millions of times
    private final List<Reservation>[] roomSchedulesCache;
    private final Room[] fastRoomLookup;

    @SuppressWarnings("unchecked")
    public ScheduleSolution(int numReservations, List<Room> rooms) {
        this.genes = new int[numReservations];

        // Initialize lookup arrays once
        int maxRoomId = rooms.stream().mapToInt(Room::id).max().orElse(0);
        this.fastRoomLookup = new Room[maxRoomId + 1];
        this.roomSchedulesCache = new ArrayList[maxRoomId + 1];

        for (Room room : rooms) {
            this.fastRoomLookup[room.id()] = room;
            this.roomSchedulesCache[room.id()] = new ArrayList<>(numReservations / rooms.size() + 2); // Initial capacity guess
        }
    }

    public ScheduleSolution(int[] genes, List<Room> rooms) {
        this(genes.length, rooms);
        System.arraycopy(genes, 0, this.genes, 0, genes.length);
    }

    public void calculateFitness(List<Reservation> reservations, List<Room> rooms) {
        double penalties = 0;
        double bonuses = 0;

        double W_HARD = 100000.0;
        double W_DOWNGRADE_PER_NIGHT = 2000.0;
        double W_UPGRADE_PER_NIGHT = 200.0;
        double W_FRAGMENTATION = 1000.0;
        double B_PERFECT_MATCH = 500.0;
        double W_VIEW_MISMATCH = 400.0;

        // OPTIMIZATION: Clear the lists instead of re-instantiating the HashMap and ArrayLists
        for (List<Reservation> list : roomSchedulesCache) {
            if (list != null) {
                list.clear();
            }
        }

        // First pass: Build schedule per room
        for (int i = 0; i < reservations.size(); i++) {
            Reservation currentRes = reservations.get(i);
            int assignedRoomId = genes[i];

            // OPTIMIZATION: Fast array lookup instead of HashMap
            if (assignedRoomId >= 0 && assignedRoomId < fastRoomLookup.length) {
                Room assignedRoom = fastRoomLookup[assignedRoomId];

                if (assignedRoom != null) {
                    roomSchedulesCache[assignedRoomId].add(currentRes);

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

                    if (currentRes.getPreferredView() != null && !currentRes.getPreferredView().equals(assignedRoom.getView())) {
                        penalties += W_VIEW_MISMATCH;
                    }
                }
            }
        }

        // Second pass: Checks overlaps, gaps, and sequences
        for (List<Reservation> schedule : roomSchedulesCache) {
            if (schedule != null && schedule.size() > 1) {
                schedule.sort(Comparator.comparing(Reservation::startDate));

                for (int i = 0; i < schedule.size() - 1; i++) {
                    Reservation current = schedule.get(i);
                    Reservation next = schedule.get(i + 1);

                    if (current.overlaps(next)) {
                        penalties += W_HARD;
                    } else {
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