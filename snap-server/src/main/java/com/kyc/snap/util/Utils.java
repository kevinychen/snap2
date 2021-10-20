
package com.kyc.snap.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Utils {

    /**
     * Replaces sets of marks that are less than minDistance with a single mark at the average point.
     */
    public static TreeSet<Integer> deduplicateCloseMarks(TreeSet<Integer> marks, int minDistance) {
        List<Integer> orderedMarks = new ArrayList<>(marks);
        TreeSet<Integer> deduplicatedMarks = new TreeSet<>();
        int start = 0;
        while (start < orderedMarks.size()) {
            int end = start;
            int sumMarks = 0;
            while (end < orderedMarks.size() && orderedMarks.get(end) - orderedMarks.get(start) < minDistance) {
                sumMarks += orderedMarks.get(end);
                end++;
            }
            deduplicatedMarks.add(sumMarks / (end - start));
            start = end;
        }
        return deduplicatedMarks;
    }

    /**
     * Find the approximate period of the given marks.
     *
     * https://math.stackexchange.com/questions/914288/how-to-find-the-approximate-basic-period-or-
     * gcd-of-a-list-of-numbers
     */
    public static double findApproxPeriod(Collection<Integer> marks) {
        int maxMark = Collections.max(marks);

        Function<Double, Double> scoreFunction = x -> {
            double sin = 0, cos = 0;
            for (int mark : marks) {
                sin += Math.sin(2 * Math.PI * mark / x);
                cos += Math.cos(2 * Math.PI * mark / x);
            }
            return Math.hypot(sin, cos) * Math.sqrt(x);
        };

        double coarsePeriod = IntStream.range(1, maxMark / 4)
            .mapToObj(Double::valueOf)
            .max(Comparator.comparing(x -> scoreFunction.apply(x)))
            .get();

        double finePeriod = IntStream.range(-maxMark / 2, maxMark / 2)
            .mapToObj(x -> coarsePeriod + 4. * x / maxMark)
            .max(Comparator.comparing(x -> scoreFunction.apply(x)))
            .get();

        return finePeriod;
    }

    /**
     * Returns a list of increasing integers with roughly the same difference between adjacent
     * values and contains all the marks (other than outliers).
     */
    public static TreeSet<Integer> findInterpolatedSequence(Collection<Integer> marks) {
        int maxMark = Collections.max(marks);

        double period = findApproxPeriod(marks);

        double[] errors = marks.stream()
            .mapToDouble(x -> x % period)
            .sorted()
            .toArray();
        double offset = errors[errors.length / 2];

        TreeSet<Integer> bestSequence = new TreeSet<>();
        double bestScore = 0;
        for (double min = offset; min < maxMark; min += period)
            for (double max = min + period; max < maxMark + period; max += period) {
                TreeSet<Integer> sequence = new TreeSet<>();
                double score = 0;
                for (double fixedMark = min; fixedMark <= max; fixedMark += period) {
                    int closestMark = Integer.MAX_VALUE / 2;
                    for (int mark : marks)
                        if (Math.abs(mark - fixedMark) < Math.abs(closestMark - fixedMark))
                            closestMark = mark;
                    if (Math.abs(closestMark - fixedMark) < period / 4) {
                        sequence.add(closestMark);
                        score++;
                    } else
                        sequence.add((int) fixedMark);
                }
                score /= Math.sqrt(max - min);
                if (score > bestScore) {
                    bestSequence = sequence;
                    bestScore = score;
                }
            }
        return bestSequence;
    }

    public static <T> List<List<T>> findConnectedComponents(List<T> items, BiPredicate<T, T> areNeighbors) {
        boolean[] visited = new boolean[items.size()];
        List<List<T>> components = new ArrayList<>();
        for (int i = 0; i < items.size(); i++)
            if (!visited[i]) {
                Deque<Integer> queue = new ArrayDeque<>();
                List<T> component = new ArrayList<>();
                visited[i] = true;
                queue.add(i);
                while (!queue.isEmpty()) {
                    int j = queue.pop();
                    component.add(items.get(j));
                    for (int k = 0; k < items.size(); k++)
                        if (!visited[k] && areNeighbors.test(items.get(j), items.get(k))) {
                            visited[k] = true;
                            queue.add(k);
                        }
                }
                components.add(component);
            }
        return components;
    }

    private Utils() {
    }
}
