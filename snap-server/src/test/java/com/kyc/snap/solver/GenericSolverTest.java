package com.kyc.snap.solver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.kyc.snap.solver.GenericSolver.Result;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


public class GenericSolverTest {

    @Test
    public void anagram() {
        String s = "AADDDEGILNNOORRRRUU";
        GenericSolverImpl<Integer> solver = new GenericSolverImpl<>();
        List<Result> results = solver.solve(
                0,
                (state, transitions) -> {
                    for (int i = 0; i < s.length(); i++)
                        if ((state & 1 << i) == 0) {
                            char c = s.charAt(i);
                            if (i > 0 && s.charAt(i - 1) == c && (state & 1 << (i - 1)) == 0)
                                continue;
                            transitions.add(state | (1 << i), EnglishTokens.is(c));
                        }
                    transitions.add(
                            Integer.bitCount(state) == s.length() ? null : state,
                            EnglishTokens.wordDelimiter());
                },
                new EnglishModel());
        assertThat(results.get(0).message().split(" ")).containsExactlyInAnyOrder("UNDERGROUND", "RAILROAD");
    }

    @Test
    public void wordsearch() {
        record Point(int x, int y) {}
        record State(Set<Point> points, Point curr) {}

        GenericSolverImpl<State> solver = new GenericSolverImpl<>();
        var points = ImmutableMap.<Point, double[]>builder()
                .put(new Point(0, 0), EnglishTokens.is('J'))
                .put(new Point(0, 1), EnglishTokens.is('A'))
                .put(new Point(1, 0), EnglishTokens.wildcard())
                .put(new Point(1, 1), EnglishTokens.is('I'))
                .build();
        List<Result> results = solver.solve(
                new State(Set.of(), null),
                (state, transitions) -> {
                    if (state.points.size() == points.size())
                        transitions.add(null, EnglishTokens.wordDelimiter());
                    points.forEach((p, probabilities) -> {
                        if (state.points.contains(p))
                            return;
                        if (state.curr == null || Math.abs(state.curr.x - p.x) + Math.abs(state.curr.y - p.y) == 1) {
                            Set<Point> newPoints = new HashSet<>(state.points);
                            newPoints.add(p);
                            transitions.add(new State(newPoints, p), probabilities);
                        }
                    });
                },
                new EnglishModel());
        assertThat(results.get(0).message()).isEqualTo("JAIL");
    }

    // for profiling
    public static void main(String[] args) {
        new GenericSolverTest().anagram();
    }
}