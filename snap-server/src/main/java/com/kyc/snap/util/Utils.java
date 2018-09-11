
package com.kyc.snap.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.IntStream;

public class Utils {

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

        double coarsePeriod = IntStream.range(1, maxMark / 2)
            .<Double>mapToObj(Double::valueOf)
            .max(Comparator.comparing(x -> scoreFunction.apply(x)))
            .get();

        double finePeriod = IntStream.range(-maxMark / 2, maxMark / 2)
            .<Double>mapToObj(x -> coarsePeriod + 4. * x / maxMark)
            .max(Comparator.comparing(x -> scoreFunction.apply(x)))
            .get();

        return finePeriod;
    }

    public static TreeSet<Integer> findInterpolatedSequence(Collection<Integer> marks) {
        int maxMark = Collections.max(marks);

        double period = findApproxPeriod(marks);

        double[] errors = marks.stream()
            .mapToDouble(x -> x % period)
            .sorted()
            .toArray();
        double offset = errors[errors.length / 2];

        List<Integer> goodClosestMarks = new ArrayList<>();
        List<Integer> goodFixedMarks = new ArrayList<>();
        for (double fixedMark = offset; fixedMark < maxMark + period; fixedMark += period) {
            int closestMark = Integer.MAX_VALUE / 2;
            for (int mark : marks)
                if (Math.abs(mark - fixedMark) < Math.abs(closestMark - fixedMark))
                    closestMark = mark;
            if (Math.abs(closestMark - fixedMark) < period / 4)
                goodClosestMarks.add(closestMark);
            else
                goodFixedMarks.add((int) fixedMark);
        }

        TreeSet<Integer> sequence = new TreeSet<>(goodClosestMarks);
        for (int mark : goodFixedMarks)
            if (mark > sequence.first() && mark < sequence.last())
                sequence.add(mark);
        return sequence;
    }

    private Utils() {
    }
}
