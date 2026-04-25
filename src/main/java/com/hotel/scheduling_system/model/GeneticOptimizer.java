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

    public ScheduleSolution solve(List<Reservation> reservations, List<Room> rooms) {
        List<ScheduleSolution> population = initializePopulation(reservations, rooms);

        long startTime = System.currentTimeMillis();
        int generation = 0;
        int stagnationCounter = 0;
        double bestFitnessSoFar = -1;

        ScheduleSolution overallBestSolution = new ScheduleSolution(population.get(0).getGenes(), rooms);
        overallBestSolution.calculateFitness(reservations);

        while (true) {
            ScheduleSolution currentBest = population.get(0);

            // Update overall best if the current generation found a better solution
            if (currentBest.getFitness() > overallBestSolution.getFitness()) {
                overallBestSolution = new ScheduleSolution(currentBest.getGenes(), rooms);
                overallBestSolution.calculateFitness(reservations);
            }

            // Check for stagnation
            if (currentBest.getFitness() <= bestFitnessSoFar) {
                stagnationCounter++;
                if (stagnationCounter >= MAX_STAGNATION) {
                    System.out.println("Algorithm stopped: Stagnation for " + MAX_STAGNATION + " generations.");
                    break;
                }
            } else {
                bestFitnessSoFar = currentBest.getFitness();
                stagnationCounter = 0;
            }

            // Check absolute limits (time and generations)
            if (generation >= MAX_GENERATIONS || (System.currentTimeMillis() - startTime > MAX_TIME_MS)) {
                System.out.println("Algorithm stopped: Reached max generation or time limit.");
                break;
            }

            // Log the best fitness for the current generation
            System.out.printf("Generation %d - Best Fitness: %.2f%n", generation, currentBest.getFitness());

            population = evolve(population, reservations, rooms);
            generation++;
        }

        return overallBestSolution;
    }

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

    private ScheduleSolution crossover(ScheduleSolution p1, ScheduleSolution p2, List<Room> rooms) {
        int cut = ThreadLocalRandom.current().nextInt(p1.getGenes().length);
        int[] childGenes = new int[p1.getGenes().length];

        System.arraycopy(p1.getGenes(), 0, childGenes, 0, cut);
        System.arraycopy(p2.getGenes(), cut, childGenes, cut, p1.getGenes().length - cut);

        return new ScheduleSolution(childGenes, rooms);
    }

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