package com.kyc.snap.cromulence;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.cromulence.CromulenceSolver.Context;
import com.kyc.snap.cromulence.CromulenceSolver.State;
import com.kyc.snap.cromulence.TermNodes.TermNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Data;

class TermStates {

    private static final int NUM_LETTERS = 26;
    private static final double MIDDLE_CONFIDENCE = .8;

    interface TermState {
        List<State> newStates(State state, Context context);
    }

    @Data
    static class SymbolState implements TermState {

        final TermState parent;
        final char c;

        @Override
        public List<State> newStates(State state, Context context) {
            double[] probs = context.getNextLetterProbabilities(state.words, state.prefix);
            if (state.quoteLevel > 0)
                probs[NUM_LETTERS] = 0;
            boolean hasWordLengths = state.wordLengths != null && !state.wordLengths.isEmpty();
            if (hasWordLengths) {
                if (state.wordLengths.get(0) == state.prefix.length())
                    for (int i = 0; i < NUM_LETTERS; i++)
                        probs[i] = 0;
                else
                    probs[NUM_LETTERS] = 0;
            }
            if (c == ' ') {
                for (int i = 0; i < NUM_LETTERS; i++)
                    probs[i] = 0;
            } else if (c == '.' || c == '*') {
                // do nothing
            } else if (c >= 'A' && c <= 'Z') {
                for (int i = 0; i < NUM_LETTERS; i++)
                    if (c != 'A' + i)
                        probs[i] = 0;
            } else if (c >= 'a' && c <= 'z') {
                for (int i = 0; i < NUM_LETTERS; i++)
                    if (c == 'a' + i)
                        probs[i] *= MIDDLE_CONFIDENCE;
                    else
                        probs[i] *= (1 - MIDDLE_CONFIDENCE) / (NUM_LETTERS - 1);
            } else {
                for (int i = 0; i <= NUM_LETTERS; i++)
                    probs[i] = 0;
            }
            List<State> newStates = new ArrayList<>();
            for (int i = 0; i < NUM_LETTERS; i++)
                if (probs[i] > 0) {
                    newStates.add(state.toBuilder()
                        .termState(parent)
                        .prefix(state.prefix + (char) ('A' + i))
                        .len(state.len + 1)
                        .score(state.score + Math.log(probs[i]))
                        .build());
                }
            if (probs[NUM_LETTERS] > 0) {
                newStates.add(state.toBuilder()
                    .termState(c == ' ' ? parent : this)
                    .words(ImmutableList.<String>builder()
                        .addAll(state.words)
                        .add(state.prefix)
                        .build())
                    .prefix("")
                    .score(state.score + Math.log(probs[NUM_LETTERS]))
                    .wordLengths(hasWordLengths ? state.wordLengths.subList(1, state.wordLengths.size()) : null)
                    .build());
            }
            return newStates;
        }
    }

    @Data
    static class AnagramState implements TermState {

        final TermState parent;
        final List<TermNode> sortedChildren;
        final long usedBitset;

        static AnagramState of(TermState parent, List<TermNode> children) {
            List<TermNode> sortedChildren = children.stream()
                .sorted(Comparator.comparing(TermNode::hashCode))
                .collect(Collectors.toList());
            return new AnagramState(parent, sortedChildren, 0);
        }

        @Override
        public List<State> newStates(State state, Context context) {
            if (usedBitset + 1 == (1L << sortedChildren.size()))
                return List.of(state.toBuilder().termState(parent).build());
            return IntStream.range(0, sortedChildren.size())
                .filter(i -> (usedBitset >> i) % 2 == 0)
                // optimization: only return the first child if multiple are identical
                .filter(i -> i == 0
                    || ((usedBitset >> (i - 1)) % 2 == 1)
                    || !sortedChildren.get(i).equals(sortedChildren.get(i - 1)))
                .mapToObj(i -> state.toBuilder()
                    .termState(sortedChildren.get(i).toTermState(
                        new AnagramState(parent, sortedChildren, usedBitset | (1L << i))))
                    .build())
                .collect(Collectors.toList());
        }
    }

    @Data
    static class ChoiceState implements TermState {

        final TermState parent;
        final List<TermNode> children;
        final boolean used;

        @Override
        public List<State> newStates(State state, Context context) {
            if (used)
                return List.of(state.toBuilder().termState(parent).build());
            return children.stream()
                .map(node -> state.toBuilder()
                    .termState(node.toTermState(
                        new ChoiceState(parent, children, true)))
                    .build())
                .collect(Collectors.toList());
        }
    }

    @Data
    static class CountState implements TermState {

        final TermState parent;
        final TermNode child;
        final int count;

        @Override
        public List<State> newStates(State state, Context context) {
            if (count == 0)
                return List.of(state.toBuilder().termState(parent).build());
            return List.of(state.toBuilder()
                .termState(child.toTermState(new CountState(parent, child, count - 1)))
                .build());
        }
    }

    @Data
    static class ListState implements TermState {

        final TermState parent;
        final List<TermNode> children;
        final int index;

        @Override
        public List<State> newStates(State state, Context context) {
            if (index == children.size())
                return List.of(state.toBuilder().termState(parent).build());
            return List.of(state.toBuilder()
                .termState(children.get(index).toTermState(
                    new ListState(parent, children, index + 1)))
                .build());
        }
    }

    @Data
    static class MaybeState implements TermState {

        final TermState parent;
        final TermNode child;
        final boolean used;

        @Override
        public List<State> newStates(State state, Context context) {
            List<State> newStates = new ArrayList<>();
            newStates.add(state.toBuilder().termState(parent).build());
            if (!used)
                newStates.add(state.toBuilder()
                    .termState(child.toTermState(
                        new MaybeState(parent, child, true)))
                    .build());
            return newStates;
        }
    }

    @Data
    static class OrMoreState implements TermState {

        final TermState parent;
        final TermNode child;
        final int atLeast;

        @Override
        public List<State> newStates(State state, Context context) {
            List<State> newStates = new ArrayList<>();
            if (atLeast == 0)
                newStates.add(state.toBuilder().termState(parent).build());
            newStates.add(state.toBuilder()
                .termState(child.toTermState(
                    new OrMoreState(parent, child, Math.max(atLeast - 1, 0))))
                .build());
            return newStates;
        }
    }

    @Data
    static class QuoteState implements TermState {

        final TermState parent;
        final List<TermNode> children;
        final int index;

        @Override
        public List<State> newStates(State state, Context context) {
            if (index == children.size())
                return List.of(state.toBuilder().termState(parent).quoteLevel(state.quoteLevel - 1).build());
            return List.of(state.toBuilder()
                .termState(children.get(index).toTermState(
                    new QuoteState(parent, children, index + 1)))
                .quoteLevel(index == 0 ? state.quoteLevel + 1 : state.quoteLevel)
                .build());
        }
    }
}
