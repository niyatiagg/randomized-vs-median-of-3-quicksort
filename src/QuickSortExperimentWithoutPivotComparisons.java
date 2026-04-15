import java.util.Arrays;
import java.util.Random;

public class QuickSortExperimentWithoutPivotComparisons {

    // ---------- Stats container ----------
    static class TrialStats {
        long comparisons;
        int maxDepth;
        long runtimeNs;

        TrialStats(long comparisons, int maxDepth, long runtimeNs) {
            this.comparisons = comparisons;
            this.maxDepth = maxDepth;
            this.runtimeNs = runtimeNs;
        }
    }

    static class Summary {
        double avgComparisons;
        long minComparisons;
        long maxComparisons;

        double avgDepth;
        int minDepth;
        int maxDepth;

        double avgRuntimeMs;
        double minRuntimeMs;
        double maxRuntimeMs;
    }

    // ---------- QuickSort instrumented ----------
    static class QS {
        long comparisons = 0;
        int maxDepth = 0;
        Random rnd;

        QS(Random rnd) {
            this.rnd = rnd;
        }

        TrialStats runStandardRandomPivot(int[] arr) {
            comparisons = 0;
            maxDepth = 0;
            long start = System.nanoTime();
            quicksortStandard(arr, 0, arr.length - 1, 1);
            long end = System.nanoTime();
            return new TrialStats(comparisons, maxDepth, end - start);
        }

        TrialStats runMedianOfThree(int[] arr) {
            comparisons = 0;
            maxDepth = 0;
            long start = System.nanoTime();
            quicksortMedian3(arr, 0, arr.length - 1, 1);
            long end = System.nanoTime();
            return new TrialStats(comparisons, maxDepth, end - start);
        }

        // Standard randomized pivot: choose one random index
        private void quicksortStandard(int[] a, int lo, int hi, int depth) {
            if (lo >= hi) return;
            maxDepth = Math.max(maxDepth, depth);

            int pivotIndex = lo + rnd.nextInt(hi - lo + 1);
            swap(a, pivotIndex, hi); // move pivot to end
            int p = partitionLomuto(a, lo, hi);

            quicksortStandard(a, lo, p - 1, depth + 1);
            quicksortStandard(a, p + 1, hi, depth + 1);
        }

        // Median-of-three pivot: pick 3 random indices, pivot = median value among them
        private void quicksortMedian3(int[] a, int lo, int hi, int depth) {
            if (lo >= hi) return;
            maxDepth = Math.max(maxDepth, depth);

            int pivotIndex = medianOfThreeRandomIndex(a, lo, hi);
            swap(a, pivotIndex, hi);
            int p = partitionLomuto(a, lo, hi);

            quicksortMedian3(a, lo, p - 1, depth + 1);
            quicksortMedian3(a, p + 1, hi, depth + 1);
        }

        // Lomuto partition: pivot is at hi
        private int partitionLomuto(int[] a, int lo, int hi) {
            int pivot = a[hi];
            int i = lo;
            for (int j = lo; j < hi; j++) {
                // comparison: a[j] <= pivot
                comparisons++;
                if (a[j] <= pivot) {
                    swap(a, i, j);
                    i++;
                }
            }
            swap(a, i, hi);
            return i;
        }

        // Pick 3 random indices and return index of median by value.
        // We count the comparisons used to determine the median.
        private int medianOfThreeRandomIndex(int[] a, int lo, int hi) {
            int i1 = lo + rnd.nextInt(hi - lo + 1);
            int i2 = lo + rnd.nextInt(hi - lo + 1);
            int i3 = lo + rnd.nextInt(hi - lo + 1);

            int x = a[i1], y = a[i2], z = a[i3];

            // Find median of (x,y,z) with counted comparisons.
            // We'll do it with a small decision tree.
            //comparisons++; // x < y ?
            if (x < y) {
                //comparisons++; // y < z ?
                if (y < z) {
                    // x < y < z => median is y
                    return i2;
                } else {
                    //comparisons++; // x < z ?
                    if (x < z) {
                        // x < z <= y => median is z
                        return i3;
                    } else {
                        // z <= x < y => median is x
                        return i1;
                    }
                }
            } else { // x >= y
                //comparisons++; // x < z ?
                if (x < z) {
                    // y <= x < z => median is x
                    return i1;
                } else {
                    //comparisons++; // y < z ?
                    if (y < z) {
                        // y < z <= x => median is z
                        return i3;
                    } else {
                        // z <= y <= x => median is y
                        return i2;
                    }
                }
            }
        }

        private void swap(int[] a, int i, int j) {
            if (i == j) return;
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    // ---------- Experiment runner ----------
    public static void main(String[] args) {
        final int n = 1000;
        final int trials = 100;

        // fixed seed = reproducible.
        Random master = new Random(42);

        TrialStats[] standard = new TrialStats[trials];
        TrialStats[] median3 = new TrialStats[trials];

        for (int t = 0; t < trials; t++) {
            int[] base = new int[n];
            for (int i = 0; i < n; i++) {
                base[i] = master.nextInt(1_000_000); // random integers
            }

            int[] a1 = Arrays.copyOf(base, n);
            int[] a2 = Arrays.copyOf(base, n);

            // Use a per-trial RNG so both algorithms get randomness but are comparable across trials
            Random trialRnd1 = new Random(master.nextLong());
            Random trialRnd2 = new Random(master.nextLong());

            QS qs1 = new QS(trialRnd1);
            QS qs2 = new QS(trialRnd2);

            standard[t] = qs1.runStandardRandomPivot(a1);
            median3[t] = qs2.runMedianOfThree(a2);

            // Optional correctness check
            if (!isSorted(a1) || !isSorted(a2)) {
                throw new RuntimeException("Sorting failed on trial " + t);
            }
        }

        Summary sStd = summarize(standard);
        Summary sMed = summarize(median3);

        printSummary("Standard Randomized QuickSort", sStd);
        printSummary("Median-of-Three QuickSort", sMed);

        // If you need exactly the table fields:
        System.out.println("\n=== Table Values (n=1000, trials=100) ===");
        System.out.printf("Standard: AvgComp=%.2f, MinComp=%d, MaxComp=%d, AvgRuntime(ms)=%.3f%n",
                sStd.avgComparisons, sStd.minComparisons, sStd.maxComparisons, sStd.avgRuntimeMs);
        System.out.printf("Median3 : AvgComp=%.2f, MinComp=%d, MaxComp=%d, AvgRuntime(ms)=%.3f%n",
                sMed.avgComparisons, sMed.minComparisons, sMed.maxComparisons, sMed.avgRuntimeMs);

        System.out.println("\n(Extra) Recursion depth:");
        System.out.printf("Standard: AvgDepth=%.2f, MinDepth=%d, MaxDepth=%d%n",
                sStd.avgDepth, sStd.minDepth, sStd.maxDepth);
        System.out.printf("Median3 : AvgDepth=%.2f, MinDepth=%d, MaxDepth=%d%n",
                sMed.avgDepth, sMed.minDepth, sMed.maxDepth);
    }

    private static boolean isSorted(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) return false;
        }
        return true;
    }

    private static Summary summarize(TrialStats[] stats) {
        Summary s = new Summary();

        long sumComp = 0;
        long minComp = Long.MAX_VALUE;
        long maxComp = Long.MIN_VALUE;

        long sumNs = 0;
        long minNs = Long.MAX_VALUE;
        long maxNs = Long.MIN_VALUE;

        long sumDepth = 0;
        int minDepth = Integer.MAX_VALUE;
        int maxDepth = Integer.MIN_VALUE;

        for (TrialStats ts : stats) {
            sumComp += ts.comparisons;
            minComp = Math.min(minComp, ts.comparisons);
            maxComp = Math.max(maxComp, ts.comparisons);

            sumNs += ts.runtimeNs;
            minNs = Math.min(minNs, ts.runtimeNs);
            maxNs = Math.max(maxNs, ts.runtimeNs);

            sumDepth += ts.maxDepth;
            minDepth = Math.min(minDepth, ts.maxDepth);
            maxDepth = Math.max(maxDepth, ts.maxDepth);
        }

        int trials = stats.length;
        s.avgComparisons = (double) sumComp / trials;
        s.minComparisons = minComp;
        s.maxComparisons = maxComp;

        s.avgRuntimeMs = (sumNs / (double) trials) / 1_000_000.0;
        s.minRuntimeMs = minNs / 1_000_000.0;
        s.maxRuntimeMs = maxNs / 1_000_000.0;

        s.avgDepth = (double) sumDepth / trials;
        s.minDepth = minDepth;
        s.maxDepth = maxDepth;

        return s;
    }

    private static void printSummary(String title, Summary s) {
        System.out.println("\n=== " + title + " ===");
        System.out.printf("Comparisons: avg=%.2f, min=%d, max=%d%n",
                s.avgComparisons, s.minComparisons, s.maxComparisons);
        System.out.printf("Runtime(ms): avg=%.3f, min=%.3f, max=%.3f%n",
                s.avgRuntimeMs, s.minRuntimeMs, s.maxRuntimeMs);
        System.out.printf("Recursion depth: avg=%.2f, min=%d, max=%d%n",
                s.avgDepth, s.minDepth, s.maxDepth);
    }
}
