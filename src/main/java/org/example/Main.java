package org.example;

import java.util.Scanner;

public class Main {
    public static Thread[] threads;
    public static int[] results;

    static synchronized void startIfNeeded(Thread thread) {
        if (thread.getState() == Thread.State.NEW) {
            thread.start();
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the number of Fibonacci terms (1-47): ");

            if (!scanner.hasNextInt()) {
                System.out.println("Error: enter a whole number from 1 to 47.");
                return;
            }

            int num = scanner.nextInt();
            if (num < 1 || num > 47) {
                System.out.println("Error: the number of terms must be from 1 to 47.");
                return;
            }

            threads = new Thread[num];
            results = new int[num];

            for (int i = 0; i < num; i++) {
                threads[i] = new Thread(new FibRunnable(i + 1), "Fib-" + (i + 1));
            }

            Thread lastThread = threads[num - 1];
            lastThread.start();

            try {
                lastThread.join();
            } catch (InterruptedException exception) {
                for (Thread thread : threads) {
                    thread.interrupt();
                }
                Thread.currentThread().interrupt();
                System.out.println("Calculation interrupted. No sequence was printed.");
                return;
            }

            StringBuilder sequence = new StringBuilder();
            for (int i = 0; i < results.length; i++) {
                if (i > 0) {
                    sequence.append(", ");
                }
                sequence.append(results[i]);
            }
            System.out.println(sequence);
        }
    }
}
