package com.hotel.scheduling_system.model;

import java.util.List;
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

    /**
     * Calculates the fitness score by penalizing constraint violations.
     * We now pass in the List of Rooms to check the Room Type!
     */
    public void calculateFitness(List<Reservation> reservations, List<Room> rooms) {
        double penalties = 0;
        double W_HARD = 10000.0; // Penalty for Overbooking
        double W_SOFT = 1000.0;  // Penalty for Wrong Room Type

        for (int i = 0; i < reservations.size(); i++) {
            Reservation currentRes = reservations.get(i);
            int assignedRoomId = genes[i];

            // 1. SOFT CONSTRAINT: Check if the assigned room matches the requested type
            Room assignedRoom = rooms.stream()
                    .filter(r -> r.getId() == assignedRoomId)
                    .findFirst()
                    .orElse(null);

            if (assignedRoom != null && currentRes.roomType() != assignedRoom.getType()) {
                penalties += W_SOFT; // Dock 1000 points if they get the wrong room!
            }

            // 2. HARD CONSTRAINT: Check for date overlaps
            for (int j = i + 1; j < reservations.size(); j++) {
                if (genes[i] == genes[j] && currentRes.overlaps(reservations.get(j))) {
                    penalties += W_HARD; // Dock 10000 points for a double-booking
                }
            }
        }
        // Higher fitness is better
        this.fitness = 1_000_000.0 - penalties;
    }

    public void mutate(double probability, List<Room> rooms) {
        for (int i = 0; i < genes.length; i++) {
            if (random.nextDouble() < probability) {
                // Safely pick a valid Room ID directly from the database list
                Room randomRoom = rooms.get(random.nextInt(rooms.size()));
                genes[i] = randomRoom.getId();
            }
        }
    }

    public int[] getGenes() { return genes; }
    public double getFitness() { return fitness; }
}