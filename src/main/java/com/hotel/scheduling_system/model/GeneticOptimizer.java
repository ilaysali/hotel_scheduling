package com.hotel.scheduling_system.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GeneticOptimizer {
    private List<ScheduleSolution> population = new ArrayList<>();
    private final Random random = new Random();

    private static final int MAX_GENERATIONS = 1000;
    private static final long MAX_TIME_MS = 5000;
    private static final int MAX_STAGNATION = 100;
    private static final int POPULATION_SIZE = 100;
    private static final int TOURNAMENT_SIZE = 5;
    private static final double MUTATION_RATE = 0.01;

    public ScheduleSolution solve(List<Reservation> reservations, List<Room> rooms) {
        // 1. Initialization
        initializePopulation(reservations, rooms);

        long startTime = System.currentTimeMillis();
        int generation = 0;
        int stagnationCounter = 0;
        double bestFitnessSoFar = -1;

        ScheduleSolution overallBestSolution = population.get(0);

        while (true) {
            // Condition 1: Generation limit
            if (generation >= MAX_GENERATIONS) {
                System.out.println("Algorithm stopped: Reached generation limit.");
                break;
            }

            // Condition 2: Time limit
            if (System.currentTimeMillis() - startTime > MAX_TIME_MS) {
                System.out.println("Algorithm stopped: Reached time limit.");
                break;
            }

            evolve(reservations, rooms);
            generation++;

            ScheduleSolution currentBest = population.stream()
                    .max(Comparator.comparingDouble(ScheduleSolution::getFitness))
                    .orElseThrow();

            if (currentBest.getFitness() > overallBestSolution.getFitness()) {
                overallBestSolution = new ScheduleSolution(currentBest.getGenes());
                overallBestSolution.calculateFitness(reservations, rooms);
            }

            // Condition 3: Stagnation
            if (currentBest.getFitness() <= bestFitnessSoFar) {
                stagnationCounter++;
                if (stagnationCounter >= MAX_STAGNATION) {
                    System.out.println("Algorithm stopped: Stagnation. No improvement for " + MAX_STAGNATION + " generations.");
                    break;
                }
            } else {
                bestFitnessSoFar = currentBest.getFitness();
                stagnationCounter = 0;
            }
        }

        return overallBestSolution;
    }

    private void evolve(List<Reservation> reservations, List<Room> rooms) {
        List<ScheduleSolution> nextGen = new ArrayList<>();

        // Elitism - Sort population from best to worst
        population.sort(Comparator.comparingDouble(ScheduleSolution::getFitness).reversed());

        nextGen.add(population.get(0));
        nextGen.add(population.get(1));

        while (nextGen.size() < population.size()) {
            ScheduleSolution p1 = tournamentSelection();
            ScheduleSolution p2 = tournamentSelection();

            ScheduleSolution child = crossover(p1, p2);
            child.mutate(MUTATION_RATE, rooms);

            // Only new children need calculations
            child.calculateFitness(reservations, rooms);
            nextGen.add(child);
        }
        this.population = nextGen;
    }

    private ScheduleSolution tournamentSelection() {
        ScheduleSolution best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            ScheduleSolution competitor = population.get(random.nextInt(population.size()));
            if (best == null || competitor.getFitness() > best.getFitness()) {
                best = competitor;
            }
        }
        return best;
    }

    private ScheduleSolution crossover(ScheduleSolution p1, ScheduleSolution p2) {
        int cut = random.nextInt(p1.getGenes().length);
        int[] childGenes = new int[p1.getGenes().length];
        System.arraycopy(p1.getGenes(), 0, childGenes, 0, cut);
        System.arraycopy(p2.getGenes(), cut, childGenes, cut, p1.getGenes().length - cut);
        return new ScheduleSolution(childGenes);
    }

    private void initializePopulation(List<Reservation> reservations, List<Room> rooms) {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            ScheduleSolution sol = new ScheduleSolution(reservations.size());
            for (int j = 0; j < reservations.size(); j++) {
                Room randomRoom = rooms.get(random.nextInt(rooms.size()));
                sol.getGenes()[j] = randomRoom.id();
            }
            sol.calculateFitness(reservations, rooms);
            population.add(sol);
        }
    }
}