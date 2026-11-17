package org.example;

public class FibRunnable implements Runnable {
    private final int n;

    public FibRunnable(int n) {
        this.n = n;
    }

    @Override
    public void run() {
        try {
            if (n > 1) {
                Main.startIfNeeded(Main.threads[n - 2]);
            }
            if (n > 2) {
                Main.startIfNeeded(Main.threads[n - 3]);
            }

            // Join outside the synchronized helper so dependencies can start.
            if (n > 1) {
                Main.threads[n - 2].join();
            }
            if (n > 2) {
                Main.threads[n - 3].join();
            }

            if (n <= 2) {
                Main.results[n - 1] = n - 1;
            } else {
                Main.results[n - 1] = Main.results[n - 2] + Main.results[n - 3];
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
