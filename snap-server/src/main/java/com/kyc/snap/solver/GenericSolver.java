package com.kyc.snap.solver;

import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;
import lombok.Data;

public interface GenericSolver<State> {

    int MAX_NUM_RESULTS = 100;

    /**
     * We're given a starting state and an object representing all possible transitions between states.
     *
     * <p>In each transition, a token is emitted with various probabilities. This method returns the most likely
     * sequence of tokens, given those probabilities, and the prior probabilities from the model.
     *
     * <p>For example, tokens may be letters, and a regex of "A." corresponds to a two-state FSM where the first state
     * emits A with 100% probability, and the second state emits all letters with equal probability. The prior would be
     * the likely n-grams and words in English.
     */
    List<Result> solve(State start, Transitions<State> transitions, PriorModel model);

    /**
     * For each state (first argument), calls the second argument on all possible next states (with the corresponding
     * token emission probabilities). For example,
     *
     * <pre>
     * (state, transitions) -> {
     *     // if we transition to state + 1, we emit token 0 or token 1 with equal probability
     *     transitions.add(state + 1, [0.5, 0.5]);
     *
     *     // if we transition to state + 2, we emit token 0 with 100% probability
     *     transitions.add(state + 2, [1.0, 0.0]);
     * }
     * </pre>
     */
    interface Transitions<State> extends BiConsumer<State, TransitionConsumer<State>> {}

    interface TransitionConsumer<State> {
        /**
         * Specifies a possible transition. A null nextState corresponds to the end state. A null tokenProbabilities
         * means that no token is emitted in this transition.
         */
        void add(@Nullable State nextState, @Nullable double[] tokenProbabilities);
    }

    interface PriorModel {
        double[] getProbabilities(List<Integer> tokens);

        String toMessage(List<Integer> tokens);
    }

    @Data
    class Result {
        final String message;
        final double score;
    }
}
