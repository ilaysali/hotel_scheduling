package com.hotel.scheduling_system.model;

import java.util.List;
import java.util.Random;

public class ScheduleSolution {
    private final int[] genes; // Index = Reservation ID, Value = Assigned Room Number
    private double fitness;
    private final Random random = new Random();

    // Standard constructor for new random solutions
    public ScheduleSolution(int numReservations) {
        this.genes = new int[numReservations];
    }

    // Constructor used for cloning (Elitism)
    public ScheduleSolution(int[] genes) {
        this.genes = genes.clone();
    }

    /**
     * Calculates the fitness score by penalizing constraint violations.
     */
    public void calculateFitness(List<Reservation> reservations) {
        double penalties = 0;
        double W_HARD = 10000.0; // Penalty for Overbooking

        for (int i = 0; i < reservations.size(); i++) {
            for (int j = i + 1; j < reservations.size(); j++) {
                // If two reservations overlap and share the same room
                if (genes[i] == genes[j] && reservations.get(i).overlaps(reservations.get(j))) {
                    penalties += W_HARD;
                }
            }
        }
        // Higher fitness is better; subtract penalties from a base max value
        this.fitness = 1_000_000.0 - penalties;
    }

    public void mutate(double probability, int totalRooms) {
        for (int i = 0; i < genes.length; i++) {
            if (random.nextDouble() < probability) {
                genes[i] = random.nextInt(totalRooms) + 1; // Random Resetting
            }
        }
    }

    public int[] getGenes() { return genes; }
    public double getFitness() { return fitness; }
}