package com.kyc.snap.solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.kyc.snap.antlr.PregexLexer;
import com.kyc.snap.antlr.PregexParser;
import com.kyc.snap.antlr.PregexParser.AnagramContext;
import com.kyc.snap.antlr.PregexParser.AndContext;
import com.kyc.snap.antlr.PregexParser.ChainContext;
import com.kyc.snap.antlr.PregexParser.ChoiceContext;
import com.kyc.snap.antlr.PregexParser.CountContext;
import com.kyc.snap.antlr.PregexParser.InterleaveContext;
import com.kyc.snap.antlr.PregexParser.ListContext;
import com.kyc.snap.antlr.PregexParser.MaybeContext;
import com.kyc.snap.antlr.PregexParser.OneOrMoreContext;
import com.kyc.snap.antlr.PregexParser.OrContext;
import com.kyc.snap.antlr.PregexParser.QuoteContext;
import com.kyc.snap.antlr.PregexParser.SymbolContext;
import com.kyc.snap.antlr.PregexParser.TermContext;
import com.kyc.snap.antlr.PregexParser.TermsContext;
import com.kyc.snap.antlr.PregexParser.WordBoundaryContext;
import com.kyc.snap.solver.GenericSolver.Result;
import com.kyc.snap.solver.GenericSolver.TransitionConsumer;

public class PregexSolver {

    private final EnglishModel model = new EnglishModel();

    public List<Result> solve(String pregex, List<Integer> wordLengths) {
        if (pregex.length() > 500)
            throw new IllegalArgumentException("Query too long");

        TermsContext termsContext = new PregexParser(
                new CommonTokenStream(new PregexLexer(CharStreams.fromString(pregex)))).terms();
        State start = new ListState(Stream.concat(
                        termsContext.term().stream().map(this::fromAntlr),
                        Stream.of(new WordBoundaryState()))
                .toList());
        if (wordLengths != null)
            start = new WordLengthsState(start, wordLengths);

        if (complexity(start) > 2000)
            throw new IllegalArgumentException("Query too complex");

        return new GenericSolverImpl<State>().solve(
                start,
                PregexSolver::getTransitions,
                model);
    }

    sealed interface State {}

    record SymbolState(char c, boolean used) implements State {}

    record AnagramState(List<State> children, int currentChild, boolean didPrevChildEmit) implements State {}

    record AndState(State child1, State child2, boolean require1) implements State {}

    record ChainState(List<State> children, boolean started) implements State {}

    record ChoiceState(List<State> children, int currentChild) implements State {}

    record CountState(State originalChild, State child, int count) implements State {}

    record InterleaveState(List<State> children1, List<State> children2, int processingChild) implements State {}

    record ListState(List<State> children) implements State {}

    record MaybeState(State child, boolean processingChild) implements State {}

    record OrMoreState(State originalChild, State child, int atLeast, boolean processingChild) implements State {}

    record QuoteState(List<State> children) implements State {}

    record WordBoundaryState() implements State {}

    record WordLengthsState(State child, List<Integer> wordLengths) implements State {}

    private State fromAntlr(TermContext genericContext) {
        if (genericContext instanceof SymbolContext context)
            return new SymbolState(context.SYMBOL().getSymbol().getText().charAt(0), false);
        if (genericContext instanceof AnagramContext context)
            return new AnagramState(context.terms().term().stream()
                    .map(this::fromAntlr)
                    .sorted(Comparator.comparingInt(State::hashCode))
                    .toList(), -1, true);
        if (genericContext instanceof AndContext context)
            return new AndState(fromAntlr(context.term(0)), fromAntlr(context.term(1)), false);
        if (genericContext instanceof ChainContext context)
            return new ChainState(context.terms().term().stream().map(this::fromAntlr).toList(), false);
        if (genericContext instanceof ChoiceContext context)
            return new ChoiceState(context.terms().term().stream().map(this::fromAntlr).toList(), -1);
        if (genericContext instanceof CountContext context) {
            State child = fromAntlr(context.term());
            return new CountState(child, child, Integer.parseInt(context.COUNT().getSymbol().getText()));
        }
        if (genericContext instanceof InterleaveContext context)
            return new InterleaveState(
                    context.terms().get(0).term().stream().map(this::fromAntlr).toList(),
                    context.terms().get(1).term().stream().map(this::fromAntlr).toList(),
                    -1);
        if (genericContext instanceof ListContext context)
            return new ListState(context.terms().term().stream().map(this::fromAntlr).toList());
        if (genericContext instanceof MaybeContext context)
            return new MaybeState(fromAntlr(context.term()), false);
        if (genericContext instanceof OneOrMoreContext context) {
            State child = fromAntlr(context.term());
            return new OrMoreState(child, child, 1, false);
        }
        if (genericContext instanceof OrContext context)
            return new ChoiceState(List.of(fromAntlr(context.term(0)), fromAntlr(context.term(1))), -1);
        if (genericContext instanceof QuoteContext context)
            return new QuoteState(context.terms().term().stream().map(this::fromAntlr).toList());
        if (genericContext instanceof WordBoundaryContext)
            return new WordBoundaryState();
        throw new IllegalStateException();
    }

    private int complexity(State genericState) {
        if (genericState instanceof SymbolState state)
            return state.c >= 'A' && state.c <= 'Z' ? 1 : 'Z' - 'A';
        if (genericState instanceof AnagramState state)
            return state.children.stream().mapToInt(this::complexity).sum();
        if (genericState instanceof AndState state)
            return complexity(state.child1) * complexity(state.child2);
        if (genericState instanceof ChainState state)
            return state.children.stream().mapToInt(this::complexity).sum();
        if (genericState instanceof ChoiceState state)
            return state.children.stream().mapToInt(this::complexity).sum();
        if (genericState instanceof CountState state)
            return complexity(state.child);
        if (genericState instanceof InterleaveState state)
            return Stream.of(state.children1, state.children2)
                    .mapToInt(child -> child.stream().mapToInt(this::complexity).max().orElse(0))
                    .sum();
        if (genericState instanceof ListState state)
            return state.children.stream().mapToInt(this::complexity).max().orElse(0);
        if (genericState instanceof MaybeState state)
            return complexity(state.child);
        if (genericState instanceof OrMoreState state)
            return complexity(state.child);
        if (genericState instanceof QuoteState state)
            return state.children.stream().mapToInt(this::complexity).max().orElse(0);
        if (genericState instanceof WordBoundaryState)
            return 1;
        if (genericState instanceof WordLengthsState state)
            return complexity(state.child);
        throw new IllegalStateException();
    }

    private static void getTransitions(State genericState, TransitionConsumer<State> transitions) {
        if (genericState instanceof SymbolState state) {
            if (state.used) {
                transitions.add(null, null);
                transitions.add(null, EnglishTokens.wordDelimiter());
            } else if (state.c >= 'A' && state.c <= 'Z')
                transitions.add(new SymbolState(state.c, true), EnglishTokens.is(state.c));
            else if (state.c >= 'a' && state.c <= 'z')
                transitions.add(new SymbolState(state.c, true), EnglishTokens.probably(Character.toUpperCase(state.c)));
            else if (state.c == '.' || state.c == '*')
                transitions.add(new SymbolState(state.c, true), EnglishTokens.wildcard());
        }
        if (genericState instanceof AnagramState state) {
            if (state.children.isEmpty())
                transitions.add(null, null);
            else if (state.currentChild == -1) {
                // Optimization: if the previous child didn't emit any tokens, then end this state.
                // Without loss of generality, we could have processed that previous child last.
                // This is important to avoid processing large numbers of permutations of empty strings.
                if (!state.didPrevChildEmit)
                    return;

                for (int i = 0; i < state.children.size(); i++)
                    if (state.children.get(i) != null) {
                        // Optimization: if multiple children are the same, process them from left to right
                        if (i > 0 && state.children.get(i - 1).equals(state.children.get(i)))
                            continue;

                        transitions.add(new AnagramState(state.children, i, false), null);
                    }
            } else
                getTransitions(state.children.get(state.currentChild), (nextState, tokenProbabilities) -> {
                    transitions.add(
                            new AnagramState(
                                    replace(state.children, state.currentChild, nextState),
                                    nextState == null ? -1 : state.currentChild,
                                    state.didPrevChildEmit || tokenProbabilities != null),
                            tokenProbabilities);
                });
        }
        if (genericState instanceof AndState state) {
            Multimap<State, double[]> transitions1 = ArrayListMultimap.create();
            Multimap<State, double[]> transitions2 = ArrayListMultimap.create();
            if (state.child1 != null)
                getTransitions(state.child1, transitions1::put);
            if (state.child2 != null)
                getTransitions(state.child2, transitions2::put);
            if (state.child1 == null && state.child2 == null)
                transitions.add(null, null);
            transitions1.forEach((nextState, tokenProbabilities) -> {
                // Optimization: if both children emit no token, only process the order child1 -> child2
                if (tokenProbabilities == null && !state.require1)
                    transitions.add(new AndState(nextState, state.child2, false), null);
            });
            transitions2.forEach((nextState, tokenProbabilities) -> {
                if (tokenProbabilities == null)
                    transitions.add(new AndState(state.child1, nextState, true), null);
            });
            transitions1.forEach((nextState1, tokenProbabilities1) -> {
                transitions2.forEach((nextState2, tokenProbabilities2) -> {
                    if (tokenProbabilities1 != null && tokenProbabilities2 != null) {
                        double[] tokenProbabilities =
                                Arrays.copyOf(tokenProbabilities1, tokenProbabilities1.length);
                        for (int i = 0; i < tokenProbabilities1.length; i++)
                            tokenProbabilities[i] *= tokenProbabilities2[i];
                        if (Arrays.stream(tokenProbabilities).sum() > 0)
                            transitions.add(new AndState(nextState1, nextState2, false), tokenProbabilities);
                    }
                });
            });
        }
        if (genericState instanceof ChainState state) {
            if (state.children.isEmpty())
                transitions.add(null, null);
            else if (!state.started) {
                for (int i = 0; i < state.children.size(); i++) {
                    List<State> orderedChildren = Stream.concat(
                                    state.children.subList(i, state.children.size()).stream(),
                                    state.children.subList(0, i).stream())
                            .toList();
                    transitions.add(new ChainState(orderedChildren, true), null);
                    transitions.add(new ChainState(Lists.reverse(orderedChildren), true), null);
                }
            } else
                getTransitions(state.children.get(0), (nextState, tokenProbabilities) -> {
                    transitions.add(
                            new ChainState(replace(state.children, 0, nextState), true),
                            tokenProbabilities);
                });
        }
        if (genericState instanceof ChoiceState state) {
            if (state.currentChild == -2)
                transitions.add(null, null);
            else if (state.currentChild == -1)
                for (int i = 0; i < state.children.size(); i++)
                    transitions.add(new ChoiceState(state.children, i), null);
            else
                getTransitions(state.children.get(state.currentChild), (nextState, tokenProbabilities) -> {
                    transitions.add(
                            new ChoiceState(
                                    replace(state.children, state.currentChild, nextState),
                                    nextState == null ? -2 : state.currentChild),
                            tokenProbabilities);
                });
        }
        if (genericState instanceof CountState state) {
            if (state.count == 0)
                transitions.add(null, null);
            else
                getTransitions(state.child, (nextState, tokenProbabilities) -> {
                    if (nextState == null)
                        transitions.add(
                                new CountState(state.originalChild, state.originalChild, state.count - 1),
                                tokenProbabilities);
                    else
                        transitions.add(
                                new CountState(state.originalChild, nextState, state.count),
                                tokenProbabilities);
                });
        }
        if (genericState instanceof InterleaveState state) {
            if (state.children1.isEmpty() && state.children2.isEmpty())
                transitions.add(null, null);
            else if (state.processingChild == -1) {
                if (!state.children1.isEmpty())
                    transitions.add(new InterleaveState(state.children1, state.children2, 1), null);
                if (!state.children2.isEmpty())
                    transitions.add(new InterleaveState(state.children1, state.children2, 2), null);
            } else if (state.processingChild == 1)
                getTransitions(state.children1.get(0), (nextState, tokenProbabilities) -> {
                    transitions.add(
                            new InterleaveState(
                                    replace(state.children1, 0, nextState),
                                    state.children2,
                                    nextState == null ? -1 : 1),
                            tokenProbabilities);
                });
            else if (state.processingChild == 2)
                getTransitions(state.children2.get(0), (nextState, tokenProbabilities) -> {
                    transitions.add(
                            new InterleaveState(
                                    state.children1,
                                    replace(state.children2, 0, nextState),
                                    nextState == null ? -1 : 2),
                            tokenProbabilities);
                });
        }
        if (genericState instanceof ListState state) {
            if (state.children.isEmpty())
                transitions.add(null, null);
            else
                getTransitions(state.children.get(0), (nextState, tokenProbabilities) -> {
                    transitions.add(
                            new ListState(replace(state.children, 0, nextState)),
                            tokenProbabilities);
                });
        }
        if (genericState instanceof MaybeState state) {
            if (!state.processingChild)
                transitions.add(null, null);
            getTransitions(state.child, (nextState, tokenProbabilities) -> {
                transitions.add(
                        nextState == null ? null : new MaybeState(nextState, true),
                        tokenProbabilities);
            });
        }
        if (genericState instanceof OrMoreState state) {
            if (!state.processingChild && state.atLeast <= 0)
                transitions.add(null, null);
            getTransitions(state.child, (nextState, tokenProbabilities) -> {
                if (nextState == null)
                    transitions.add(
                            new OrMoreState(state.originalChild, state.originalChild, state.atLeast - 1, false),
                            tokenProbabilities);
                else
                    transitions.add(
                            new OrMoreState(state.originalChild, nextState, state.atLeast, true),
                            tokenProbabilities);
            });
        }
        if (genericState instanceof QuoteState state) {
            if (state.children.isEmpty())
                transitions.add(null, null);
            else
                getTransitions(state.children.get(0), (nextState, tokenProbabilities) -> {
                    List<State> newChildren = replace(state.children, 0, nextState);
                    if (tokenProbabilities == null) {
                        transitions.add(new QuoteState(newChildren), null);
                        return;
                    }
                    double[] newTokenProbabilities = Arrays.copyOf(tokenProbabilities, tokenProbabilities.length);
                    newTokenProbabilities[0] = 0;
                    transitions.add(new QuoteState(newChildren), newTokenProbabilities);
                });
        }
        if (genericState instanceof WordBoundaryState) {
            transitions.add(null, EnglishTokens.wordDelimiter());
        }
        if (genericState instanceof WordLengthsState state) {
            if (state.wordLengths.isEmpty())
                transitions.add(null, null);
            if (state.child != null)
                getTransitions(state.child, (nextState, tokenProbabilities) -> {
                    if (tokenProbabilities == null)
                        transitions.add(new WordLengthsState(nextState, state.wordLengths), null);
                    else if (!state.wordLengths.isEmpty()) {
                        int len = state.wordLengths.get(0);
                        if (len == 0) {
                            double[] newTokenProbabilities = new double[tokenProbabilities.length];
                            newTokenProbabilities[0] = tokenProbabilities[0];
                            transitions.add(
                                    new WordLengthsState(nextState, state.wordLengths.stream().skip(1).toList()),
                                    newTokenProbabilities);
                        } else {
                            double[] newTokenProbabilities = Arrays.copyOf(tokenProbabilities, tokenProbabilities.length);
                            newTokenProbabilities[0] = 0;
                            transitions.add(
                                    new WordLengthsState(nextState, replace(state.wordLengths, 0, len - 1)),
                                    newTokenProbabilities);
                        }
                    }
                });
        }
    }

    private static <T> List<T> replace(List<T> list, int index, T newValue) {
        List<T> newList = new ArrayList<>(list);
        if (newValue == null)
            newList.remove(index);
        else
            newList.set(index, newValue);
        return newList;
    }
}
