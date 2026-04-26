package com.hotel.scheduling_system.model;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GeneticOptimizer {

    // Configuration constants
    private static final int MAX_GENERATIONS = 1000;
    private static final long MAX_TIME_MS = 5000;
    private static final int MAX_STAGNATION = 100;
    private static final int POPULATION_SIZE = 100;
    private static final int TOURNAMENT_SIZE = 5;
    private static final double MUTATION_RATE = 0.01;

    /**
     * Executes the genetic algorithm to find the optimal schedule solution.
     * The algorithm runs until it reaches the maximum number of generations,
     * hits the time limit, or stagnates for too many generations.
     *
     * @param reservations the list of all reservations to be scheduled
     * @param rooms the list of available rooms
     * @return the best ScheduleSolution found during the execution
     */
    public ScheduleSolution solve(List<Reservation> reservations, List<Room> rooms) {
        List<ScheduleSolution> population = initializePopulation(reservations, rooms);

        long startTime = System.currentTimeMillis();
        int generation = 0;
        int stagnationCounter = 0;
        double bestFitnessSoFar = -1;

        ScheduleSolution overallBestSolution = new ScheduleSolution(population.get(0).getGenes(), rooms);
        overallBestSolution.calculateFitness(reservations);

        // Loop runs as long as limits (generations, stagnation, and time) are not exceeded
        while (generation < MAX_GENERATIONS && stagnationCounter < MAX_STAGNATION && (System.currentTimeMillis() - startTime <= MAX_TIME_MS)) {
            ScheduleSolution currentBest = population.get(0);

            // Update overall best if the current generation found a better solution
            if (currentBest.getFitness() > overallBestSolution.getFitness()) {
                overallBestSolution = new ScheduleSolution(currentBest.getGenes(), rooms);
                overallBestSolution.calculateFitness(reservations);
            }

            // Update stagnation counter
            if (currentBest.getFitness() <= bestFitnessSoFar) {
                stagnationCounter++;
            } else {
                bestFitnessSoFar = currentBest.getFitness();
                stagnationCounter = 0;
            }

            // Proceed with evolution
            System.out.printf("Generation %d - Best Fitness: %.2f%n", generation, currentBest.getFitness());
            population = evolve(population, reservations, rooms);
            generation++;
        }

        // Log the reason for stopping after the loop finishes
        if (stagnationCounter >= MAX_STAGNATION) {
            System.out.println("Algorithm stopped: Stagnation for " + MAX_STAGNATION + " generations.");
        } else {
            System.out.println("Algorithm stopped: Reached max generation or time limit.");
        }

        return overallBestSolution;
    }

    /**
     * Creates the next generation of solutions from the current population.
     * Uses elitism to keep the best solutions, and applies tournament selection,
     * crossover, and mutation to generate the rest of the new population.
     *
     * @param currentPopulation the current list of schedule solutions
     * @param reservations the list of reservations
     * @param rooms the list of available rooms
     * @return a new list of schedule solutions representing the next generation
     */
    private List<ScheduleSolution> evolve(List<ScheduleSolution> currentPopulation, List<Reservation> reservations, List<Room> rooms) {
        List<ScheduleSolution> nextGen = new ArrayList<>(POPULATION_SIZE);

        // Elitism - keep the best 2 solutions automatically
        currentPopulation.sort(Comparator.comparingDouble(ScheduleSolution::getFitness).reversed());
        nextGen.add(currentPopulation.get(0));
        nextGen.add(currentPopulation.get(1));

        while (nextGen.size() < currentPopulation.size()) {
            ScheduleSolution p1 = tournamentSelection(currentPopulation);
            ScheduleSolution p2 = tournamentSelection(currentPopulation);

            ScheduleSolution child = crossover(p1, p2, rooms);
            child.mutate(MUTATION_RATE, rooms);
            child.calculateFitness(reservations);

            nextGen.add(child);
        }

        return nextGen;
    }

    /**
     * Selects a single solution from the population using tournament selection.
     * Randomly picks a subset of solutions and returns the one with the highest fitness.
     *
     * @param currentPopulation the list of schedule solutions to select from
     * @return the solution that won the tournament
     */
    private ScheduleSolution tournamentSelection(List<ScheduleSolution> currentPopulation) {
        ScheduleSolution best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            // Using ThreadLocalRandom for better performance
            int randomIndex = ThreadLocalRandom.current().nextInt(currentPopulation.size());
            ScheduleSolution competitor = currentPopulation.get(randomIndex);

            if (best == null || competitor.getFitness() > best.getFitness()) {
                best = competitor;
            }
        }
        return best;
    }

    /**
     * Performs a single-point crossover between two parent solutions to produce a child solution.
     * The child receives a portion of its genes from the first parent and the rest from the second parent.
     *
     * @param p1 the first parent solution
     * @param p2 the second parent solution
     * @param rooms the list of available rooms used to construct the child solution
     * @return a new child ScheduleSolution resulting from the crossover
     */
    private ScheduleSolution crossover(ScheduleSolution p1, ScheduleSolution p2, List<Room> rooms) {
        int cut = ThreadLocalRandom.current().nextInt(p1.getGenes().length);
        int[] childGenes = new int[p1.getGenes().length];

        System.arraycopy(p1.getGenes(), 0, childGenes, 0, cut);
        System.arraycopy(p2.getGenes(), cut, childGenes, cut, p1.getGenes().length - cut);

        return new ScheduleSolution(childGenes, rooms);
    }

    /**
     * Generates the initial population of solutions with random room assignments.
     * Each solution in the population is assigned a random room for every reservation.
     *
     * @param reservations the list of reservations to assign
     * @param rooms the list of available rooms to randomly assign from
     * @return a sorted list of initially randomized schedule solutions
     */
    private List<ScheduleSolution> initializePopulation(List<Reservation> reservations, List<Room> rooms) {
        List<ScheduleSolution> initialPopulation = new ArrayList<>(POPULATION_SIZE);

        for (int i = 0; i < POPULATION_SIZE; i++) {
            ScheduleSolution sol = new ScheduleSolution(reservations.size(), rooms);

            for (int j = 0; j < reservations.size(); j++) {
                Room randomRoom = rooms.get(ThreadLocalRandom.current().nextInt(rooms.size()));
                sol.getGenes()[j] = randomRoom.id();
            }

            sol.calculateFitness(reservations);
            initialPopulation.add(sol);
        }

        initialPopulation.sort(Comparator.comparingDouble(ScheduleSolution::getFitness).reversed());
        return initialPopulation;
    }
}