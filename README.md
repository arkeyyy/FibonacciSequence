# Fibonacci Sequence Using Threads

This console program asks for an integer `num` and prints the first `num` Fibonacci numbers, starting with `0` and `1`. Each subsequent number is the sum of the two preceding numbers.

The complete Java solution is included below as a self-contained example. Copy the two code blocks into files named `Main.java` and `FibRunnable.java` in the same separate folder to run it.

## Input and output

Enter an integer from **1 to 47**. The results use an `int[]`, so 47 terms is the largest supported sequence: its last value is `1836311903`. The next Fibonacci number, `2971215073`, exceeds Java's maximum `int` value of `2147483647`.

Nonnumeric input, decimals, and numbers outside the supported range produce an error message and end the program.

Example:

```text
Enter the number of Fibonacci terms (1-47): 7
0, 1, 1, 2, 3, 5, 8
```

## How the threads work

1. `Main` creates a static `Thread[]` and a static `int[]`, both with length `num`.
2. Every thread receives a `FibRunnable` with a one-based field `n`: the first has `n = 1`, the second has `n = 2`, and so on. All threads are constructed before any thread starts.
3. `main` starts only the last thread. Each worker starts its available predecessors, which in turn start their own predecessors.
4. The synchronized `startIfNeeded` helper checks for the `NEW` state and starts a thread only once, even when two workers request the same predecessor.
5. Each worker calls `join()` on its predecessors before reading their results. The joins happen outside the helper's synchronized lock, allowing other workers to start their dependencies.
6. Worker 1 stores `0`. Worker 2 waits for worker 1 and stores `1`. Every later worker adds the two preceding results and stores the sum at index `n - 1`.
7. `main` joins the last thread, then prints the results in order. Because each worker waits for its predecessors, completion of the last thread means the entire sequence is ready.

Each worker writes to its own array slot. Joining a predecessor also makes its completed result visible to the waiting thread. Workers never print sequence values, so scheduling cannot change the output order.

This approach demonstrates thread coordination. Creating one thread per term has overhead and is not intended to outperform a simple iterative Fibonacci calculation.

## Java solution

### Main.java

```java
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
```

### FibRunnable.java

```java
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
```

If the main thread is interrupted while waiting, it requests interruption of the workers, restores its interrupt status, and exits without printing a partial sequence. Workers also restore their interrupt status if a dependency wait is interrupted.

## Compile and run

Use a Java Development Kit (JDK) with `javac` and `java` available on your PATH. No external libraries or Maven installation are needed for this example.

After copying the examples into `Main.java` and `FibRunnable.java` in a separate folder, open a terminal in that folder and run:

```shell
javac -d out Main.java FibRunnable.java
java -cp out org.example.Main
```

Both files declare `package org.example`. The `-d out` option places the compiled classes in the appropriate package directory under `out`.

## Example checks

| Input | Expected result |
| --- | --- |
| `1` | `0` |
| `2` | `0, 1` |
| `7` | `0, 1, 1, 2, 3, 5, 8` |
| `47` | 47 terms, ending with `1836311903` |
| `0`, `-1`, or `48` | Range error; no sequence |
| `hello`, `2.5`, or `2147483648` | Whole-number error; no sequence |
