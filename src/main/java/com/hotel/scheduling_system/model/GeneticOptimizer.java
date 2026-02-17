package com.hotel.scheduling_system.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GeneticOptimizer {
    private List<ScheduleSolution> population = new ArrayList<>();
    private final Random random = new Random();

    public ScheduleSolution solve(List<Reservation> reservations, int numRooms) {
        // 1. Initialization
        initializePopulation(100, reservations.size(), numRooms);

        int generation = 0;
        // 2. Evolution loop with termination criteria (Generation limit)
        while (generation < 500) {
            evolve(reservations, numRooms);
            generation++;
        }

        // 3. Return the best solution found
        return population.stream()
                .max(Comparator.comparingDouble(ScheduleSolution::getFitness))
                .orElse(null);
    }

    private void evolve(List<Reservation> reservations, int numRooms) {
        List<ScheduleSolution> nextGen = new ArrayList<>();

        // Elitism: Preserve the top 2 solutions
        population.sort(Comparator.comparingDouble(ScheduleSolution::getFitness).reversed());
        nextGen.add(new ScheduleSolution(population.get(0).getGenes()));
        nextGen.add(new ScheduleSolution(population.get(1).getGenes()));

        // Fill remaining population with offspring
        while (nextGen.size() < population.size()) {
            ScheduleSolution p1 = tournamentSelection(5);
            ScheduleSolution p2 = tournamentSelection(5);

            ScheduleSolution child = crossover(p1, p2);
            child.mutate(0.01, numRooms);
            child.calculateFitness(reservations);
            nextGen.add(child);
        }
        this.population = nextGen;
    }

    private ScheduleSolution tournamentSelection(int k) {
        List<ScheduleSolution> tournament = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            tournament.add(population.get(random.nextInt(population.size())));
        }
        return tournament.stream()
                .max(Comparator.comparingDouble(ScheduleSolution::getFitness))
                .get();
    }

    private ScheduleSolution crossover(ScheduleSolution p1, ScheduleSolution p2) {
        int cut = random.nextInt(p1.getGenes().length); // One-Point Crossover
        int[] childGenes = new int[p1.getGenes().length];
        System.arraycopy(p1.getGenes(), 0, childGenes, 0, cut);
        System.arraycopy(p2.getGenes(), cut, childGenes, cut, p1.getGenes().length - cut);
        return new ScheduleSolution(childGenes);
    }

    private void initializePopulation(int size, int resCount, int rooms) {
        for (int i = 0; i < size; i++) {
            ScheduleSolution sol = new ScheduleSolution(resCount);
            for (int j = 0; j < resCount; j++) {
                sol.getGenes()[j] = random.nextInt(rooms) + 1;
            }
            population.add(sol);
        }
    }
}
