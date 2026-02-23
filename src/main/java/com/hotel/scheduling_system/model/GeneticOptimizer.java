package com.hotel.scheduling_system.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class GeneticOptimizer {
    private List<ScheduleSolution> population = new ArrayList<>();
    private final Random random = new Random();

    // הגדרת תנאי העצירה כפי שהוגדרו במסמך האפיון של הפרויקט!
    private static final int MAX_GENERATIONS = 1000;         // מגבלת דורות
    private static final long MAX_TIME_MS = 5000;            // מגבלת זמן (5 שניות)
    private static final int MAX_STAGNATION = 100;           // מגבלת קיפאון
    private static final double TARGET_FITNESS = 1_000_000.0;// יעד איכות (ציון מושלם)

    public ScheduleSolution solve(List<Reservation> reservations, List<Room> rooms) {
        // 1. אתחול
        initializePopulation(100, reservations, rooms);

        long startTime = System.currentTimeMillis();
        int generation = 0;
        int stagnationCounter = 0;
        double bestFitnessSoFar = -1;

        ScheduleSolution overallBestSolution = population.get(0);

        // הלולאה החכמה - בקרת עצירה משולבת
        while (true) {
            // תנאי 1: מגבלת דורות
            if (generation >= MAX_GENERATIONS) {
                System.out.println("Algorithm stopped: Reached generation limit.");
                break;
            }

            // תנאי 2: מגבלת זמן (חווית משתמש)
            if (System.currentTimeMillis() - startTime > MAX_TIME_MS) {
                System.out.println("Algorithm stopped: Reached time limit.");
                break;
            }

            // ביצוע תהליך האבולוציה לדור הנוכחי
            evolve(reservations, rooms);
            generation++;

            // מציאת הפתרון הטוב ביותר בדור הנוכחי
            ScheduleSolution currentBest = population.stream()
                    .max(Comparator.comparingDouble(ScheduleSolution::getFitness))
                    .orElseThrow();

            // שמירת הפתרון הטוב ביותר שנמצא אי פעם (אליטיזם למעקב)
            if (currentBest.getFitness() > overallBestSolution.getFitness()) {
                overallBestSolution = new ScheduleSolution(currentBest.getGenes());
                overallBestSolution.calculateFitness(reservations, rooms);
            }

            // תנאי 3: יעד איכות (נמצא פתרון מושלם ללא חפיפות או בעיות)
            if (currentBest.getFitness() >= TARGET_FITNESS) {
                System.out.println("Algorithm stopped: Found perfect schedule at generation " + generation + "!");
                break;
            }

            // תנאי 4: קיפאון (אין שיפור בציון)
            if (currentBest.getFitness() == bestFitnessSoFar) {
                stagnationCounter++;
                if (stagnationCounter >= MAX_STAGNATION) {
                    System.out.println("Algorithm stopped: Stagnation. No improvement for " + MAX_STAGNATION + " generations.");
                    break;
                }
            } else {
                bestFitnessSoFar = currentBest.getFitness();
                stagnationCounter = 0; // איפוס המונה כי היה שיפור
            }
        }

        return overallBestSolution;
    }

    private void evolve(List<Reservation> reservations, List<Room> rooms) {
        List<ScheduleSolution> nextGen = new ArrayList<>();

        // אליטיזם - שמירת 2 ההורים הטובים ביותר ללא שינוי
        population.sort(Comparator.comparingDouble(ScheduleSolution::getFitness).reversed());
        nextGen.add(new ScheduleSolution(population.get(0).getGenes()));
        nextGen.add(new ScheduleSolution(population.get(1).getGenes()));

        while (nextGen.size() < population.size()) {
            ScheduleSolution p1 = tournamentSelection(5);
            ScheduleSolution p2 = tournamentSelection(5);

            ScheduleSolution child = crossover(p1, p2);
            child.mutate(0.01, rooms);
            child.calculateFitness(reservations, rooms);
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
        int cut = random.nextInt(p1.getGenes().length);
        int[] childGenes = new int[p1.getGenes().length];
        System.arraycopy(p1.getGenes(), 0, childGenes, 0, cut);
        System.arraycopy(p2.getGenes(), cut, childGenes, cut, p1.getGenes().length - cut);
        return new ScheduleSolution(childGenes);
    }

    private void initializePopulation(int size, List<Reservation> reservations, List<Room> rooms) {
        for (int i = 0; i < size; i++) {
            ScheduleSolution sol = new ScheduleSolution(reservations.size());
            for (int j = 0; j < reservations.size(); j++) {
                Room randomRoom = rooms.get(random.nextInt(rooms.size()));
                sol.getGenes()[j] = randomRoom.getId();
            }
            sol.calculateFitness(reservations, rooms);
            population.add(sol);
        }
    }
}