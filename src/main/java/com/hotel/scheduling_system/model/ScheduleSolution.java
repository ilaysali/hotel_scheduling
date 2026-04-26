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

    // list of constrains
    private static final double W_HARD = 100000.0;
    private static final double W_DOWNGRADE_PER_NIGHT = 2000.0;
    private static final double W_UPGRADE_PER_NIGHT = 200.0;
    private static final double W_FRAGMENTATION = 1000.0;
    private static final double B_PERFECT_MATCH = 500.0;
    private static final double W_VIEW_MISMATCH = 400.0;

    private static final double AI_START_SCORE = 1_000_000.0;
    private static final double CAPACITY_BUFFER_MULTIPLIER = 1.1;

    /**
     * Initializes a new schedule solution with an empty set of genes.
     * Sets up the fast room lookup array and initializes the room schedules cache
     * based on the given list of rooms and the total number of reservations.
     *
     * @param numReservations the total number of reservations to be scheduled
     * @param rooms the list of available rooms in the hotel
     */
    public ScheduleSolution(int numReservations, List<Room> rooms) {
        this.genes = new int[numReservations];

        int maxRoomId = rooms.stream().mapToInt(Room::id).max().orElse(0);
        this.fastRoomLookup = new Room[maxRoomId + 1];
        this.roomSchedulesCache = new ArrayList[maxRoomId + 1];

        for (Room room : rooms) {
            this.fastRoomLookup[room.id()] = room;
            int initialCapacity = (int) ((numReservations / rooms.size()) * CAPACITY_BUFFER_MULTIPLIER);
            this.roomSchedulesCache[room.id()] = new ArrayList<>(initialCapacity);
        }
    }

    /**
     * Creates a new schedule solution based on an existing array of genes.
     * This is typically used when creating offspring during crossover or cloning a solution.
     *
     * @param genes the array representing the room assignments for reservations
     * @param rooms the list of available rooms in the hotel
     */
    public ScheduleSolution(int[] genes, List<Room> rooms) {
        this(genes.length, rooms);
        System.arraycopy(genes, 0, this.genes, 0, genes.length);
    }

    /**
     * Calculates the overall fitness score for this schedule solution.
     * It clears the cache, calculates penalties for room mismatches, evaluates
     * sequence penalties and bonuses (like gaps and overlaps), and updates the final fitness.
     *
     * @param reservations the list of all reservations to be scheduled
     */
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

        this.fitness = AI_START_SCORE - penalties + bonuses;
    }

    /**
     * Clears the current room schedules cache.
     * This ensures no leftover data from previous fitness calculations affects the current one.
     */
    private void clearCache() {
        for (List<Reservation> list : roomSchedulesCache) {
            if (list != null) {
                list.clear();
            }
        }
    }

    /**
     * Processes each reservation, assigns it to its designated room based on the genes,
     * and accumulates the penalty score for room mismatches (like upgrades, downgrades, or view issues).
     *
     * @param reservations the list of reservations to assign
     * @return the total penalty accumulated from individual room matches
     */
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

    /**
     * Evaluates how well a specific reservation matches the assigned room.
     * Calculates penalties based on room type downgrades, upgrades, and preferred view mismatches.
     *
     * @param res the reservation to evaluate
     * @param room the room assigned to the reservation
     * @return the calculated penalty for this specific assignment
     */
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

    /**
     * Iterates through the schedules of all rooms and calculates penalties and bonuses
     * based on the sequences of reservations (e.g., hard conflicts, fragmentation, perfect back-to-back matches).
     *
     * @return an array of two doubles: index 0 contains total sequence penalties, index 1 contains total sequence bonuses
     */
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

        return new double[]{seqPenalties, seqBonuses};
    }

    /**
     * Mutates the current solution by randomly changing the assigned room for each reservation
     * based on a given mutation probability.
     *
     * @param probability the chance (between 0.0 and 1.0) of a single gene being mutated
     * @param rooms the list of available rooms to pick from during mutation
     */
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