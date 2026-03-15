package com.hotel.scheduling_system.model;

import lombok.Getter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ScheduleSolution {

    @Getter
    private final int[] genes;

    @Getter
    private double fitness;

    private final List<Reservation>[] roomSchedulesCache;
    private final Room[] fastRoomLookup;

    private static final double W_HARD = 100000.0;
    private static final double W_DOWNGRADE_PER_NIGHT = 2000.0;
    private static final double W_UPGRADE_PER_NIGHT = 200.0;
    private static final double W_FRAGMENTATION = 1000.0;
    private static final double B_PERFECT_MATCH = 500.0;
    private static final double W_VIEW_MISMATCH = 400.0;

    @SuppressWarnings("unchecked")
    public ScheduleSolution(int numReservations, List<Room> rooms) {
        this.genes = new int[numReservations];

        int maxRoomId = rooms.stream().mapToInt(Room::id).max().orElse(0);
        this.fastRoomLookup = new Room[maxRoomId + 1];
        this.roomSchedulesCache = new ArrayList[maxRoomId + 1];

        for (Room room : rooms) {
            this.fastRoomLookup[room.id()] = room;
            int initialCapacity = (int) ((numReservations / rooms.size()) * 1.1);
            this.roomSchedulesCache[room.id()] = new ArrayList<>(initialCapacity);        }
    }

    public ScheduleSolution(int[] genes, List<Room> rooms) {
        this(genes.length, rooms);
        System.arraycopy(genes, 0, this.genes, 0, genes.length);
    }

    public void calculateFitness(List<Reservation> reservations) {
        clearCache();

        double penalties = 0;
        double bonuses = 0;

        // 1. Assign reservations to rooms and calculate individual room match penalties
        penalties += calculateRoomAssignments(reservations);

        // 2. Calculate sequence penalties (overlaps and gaps within the same room)
        double[] sequenceScores = calculateSequenceScores();
        penalties += sequenceScores[0];
        bonuses += sequenceScores[1];

        this.fitness = 1_000_000.0 - penalties + bonuses;
    }

    private void clearCache() {
        for (List<Reservation> list : roomSchedulesCache) {
            if (list != null) {
                list.clear();
            }
        }
    }

    private double calculateRoomAssignments(List<Reservation> reservations) {
        double localPenalties = 0;

        for (int i = 0; i < reservations.size(); i++) {
            Reservation currentRes = reservations.get(i);
            int assignedRoomId = genes[i];

            if (assignedRoomId >= 0 && assignedRoomId < fastRoomLookup.length) {
                Room assignedRoom = fastRoomLookup[assignedRoomId];

                if (assignedRoom != null) {
                    roomSchedulesCache[assignedRoomId].add(currentRes);
                    localPenalties += evaluateRoomMatch(currentRes, assignedRoom);
                }
            }
        }
        return localPenalties;
    }

    private double evaluateRoomMatch(Reservation res, Room room) {
        double penalty = 0;

        // Check Downgrade/Upgrade
        if (res.roomType() != room.type()) {
            int requestedRank = res.roomType().getRank();
            int assignedRank = room.type().getRank();
            long nights = res.nights();

            if (nights <= 0) {
                throw new IllegalArgumentException("CRITICAL ERROR: Invalid dates for Reservation ID " + res.id());
            }

            if (assignedRank < requestedRank) {
                penalty += (W_DOWNGRADE_PER_NIGHT * nights * (requestedRank - assignedRank));
            } else {
                penalty += (W_UPGRADE_PER_NIGHT * nights * (assignedRank - requestedRank));
            }
        }

        // Check View Mismatch (assuming room.view() exists based on original code)
        if (res.getPreferredView() != null && !res.getPreferredView().equals(room.view())) {
            penalty += W_VIEW_MISMATCH;
        }

        return penalty;
    }

    private double[] calculateSequenceScores() {
        double seqPenalties = 0;
        double seqBonuses = 0;

        for (List<Reservation> schedule : roomSchedulesCache) {
            if (schedule != null && schedule.size() > 1) {
                schedule.sort(Comparator.comparing(Reservation::startDate));

                for (int i = 0; i < schedule.size() - 1; i++) {
                    Reservation current = schedule.get(i);
                    Reservation next = schedule.get(i + 1);

                    if (current.overlaps(next)) {
                        seqPenalties += W_HARD; // Collision found!
                    } else {
                        long gap = ChronoUnit.DAYS.between(current.endDate(), next.startDate());
                        if (gap == 1) {
                            seqPenalties += W_FRAGMENTATION; // Small gap is inefficient
                        } else if (gap == 0) {
                            seqBonuses += B_PERFECT_MATCH; // Back-to-back reservations are perfect
                        }
                    }
                }
            }
        }

        // Return array where index 0 is penalties and index 1 is bonuses
        return new double[]{seqPenalties, seqBonuses};
    }

    public void mutate(double probability, List<Room> rooms) {
        for (int i = 0; i < genes.length; i++) {
            // Using ThreadLocalRandom for efficiency
            if (ThreadLocalRandom.current().nextDouble() < probability) {
                Room randomRoom = rooms.get(ThreadLocalRandom.current().nextInt(rooms.size()));
                genes[i] = randomRoom.id();
            }
        }
    }
}