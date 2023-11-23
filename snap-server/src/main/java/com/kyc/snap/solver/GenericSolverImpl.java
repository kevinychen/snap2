package com.kyc.snap.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;

public class GenericSolverImpl<State> implements GenericSolver<State> {

    private static final int SEARCH_LIMIT = 10000;

    private final Random random = new Random(0);

    @Override
    public List<Result> solve(State start, Transitions<State> transitions, PriorModel model) {
        List<FullState<State>> currStates = List.of(new FullState<>(start, List.of(), 0));
        List<FullState<State>> nextStates = new ArrayList<>();
        TreeSet<Result> bestResults =
                new TreeSet<>(Comparator.comparingDouble(Result::getScore).thenComparing(Result::getMessage).reversed());
        for (int i = 0; i < 1000 && !currStates.isEmpty(); i++) {
            List<FullState<State>> newCurrStates = new ArrayList<>();
            List<FullState<State>> newNextStates = nextStates;
            double scoreThreshold = Math.max(approxScoreThreshold(currStates), minScoreThreshold(bestResults));
            for (FullState<State> state : currStates)
                if (state.score >= scoreThreshold) {
                    double[] priorProbabilities = model.getProbabilities(state.tokens);
                    transitions.accept(state.state, (nextState, posteriorProbabilities) -> {
                        if (posteriorProbabilities == null) {
                            if (nextState != null)
                                newCurrStates.add(new FullState<>(nextState, state.tokens, state.score));
                            else
                                bestResults.add(new Result(model.toMessage(state.tokens), state.score));
                            return;
                        }
                        for (int j = 0; j < priorProbabilities.length; j++) {
                            double probability = priorProbabilities[j] * posteriorProbabilities[j];
                            if (probability == 0)
                                continue;
                            List<Integer> newTokens = new ArrayList<>(state.tokens.size() + 1);
                            newTokens.addAll(state.tokens);
                            newTokens.add(j);
                            double newScore = state.score + Math.log(probability) + 2;
                            if (nextState != null)
                                newNextStates.add(new FullState<>(nextState, newTokens, newScore));
                            else
                                bestResults.add(new Result(model.toMessage(newTokens), newScore));
                        }
                    });
                }
            currStates = newCurrStates;
            if (currStates.isEmpty() || nextStates.size() > 1000000) {
                currStates = nextStates;
                nextStates = new ArrayList<>();
            }
            while (bestResults.size() > MAX_NUM_RESULTS)
                bestResults.pollLast();
        }
        return StreamEx.of(bestResults.stream())
                .distinct(Result::getMessage)
                .collect(Collectors.toList());
    }

    private double approxScoreThreshold(List<FullState<State>> states) {
        if (states.size() < SEARCH_LIMIT)
            return Double.NEGATIVE_INFINITY;
        double[] samples = new double[SEARCH_LIMIT];
        for (int i = 0; i < SEARCH_LIMIT; i++)
            samples[i] = states.get(random.nextInt(states.size())).score;
        Arrays.sort(samples);
        return samples[(int) (SEARCH_LIMIT - (long) SEARCH_LIMIT * SEARCH_LIMIT / states.size())];
    }

    private double minScoreThreshold(TreeSet<Result> bestFinalStates) {
        if (bestFinalStates.size() < MAX_NUM_RESULTS)
            return Double.NEGATIVE_INFINITY;
        return bestFinalStates.last().score;
    }

    record FullState<State>(State state, List<Integer> tokens, double score) {}
}
