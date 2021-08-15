package com.kyc.snap.cromulence;

import com.google.common.collect.TreeMultiset;
import com.kyc.snap.antlr.PregexLexer;
import com.kyc.snap.antlr.PregexParser;
import com.kyc.snap.antlr.PregexParser.TermsContext;
import com.kyc.snap.cromulence.TermNodes.ListNode;
import com.kyc.snap.cromulence.TermNodes.SymbolNode;
import com.kyc.snap.cromulence.TermNodes.TermNode;
import com.kyc.snap.cromulence.TermStates.TermState;
import com.kyc.snap.words.DictionaryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

@Data
public class CromulenceSolver {

    static final int NUM_LETTERS = 26;

    private static final int SEARCH_LIMIT = 2000;
    private static final int NUM_RESULTS = 50;

    private final DictionaryManager dictionaryManager;
    private final Map<String, double[]> nextLetterFreqsCache = new HashMap<>();

    public List<CromulenceSolverResult> solve(String query, @Nullable List<Integer> wordLengths) {
        if (query.length() > 500)
            throw new IllegalArgumentException("Query too long");

        Context context = new Context();
        TermsContext termsContext = new PregexParser(
            new CommonTokenStream(new PregexLexer(CharStreams.fromString(query)))).terms();
        List<TermNode> parts = new ArrayList<>();
        termsContext.term().forEach(childContext -> parts.add(TermNodes.fromAntlr(childContext)));
        parts.add(new SymbolNode(' '));
        Comparator<State> scoreComparator = Comparator.comparingDouble(State::getScore).reversed();
        TermNode node = new ListNode(parts);
        if (node.complexity() > 1000)
            throw new IllegalArgumentException("Query too complex");

        TreeMap<Integer, List<State>> statesByLength = new TreeMap<>();
        statesByLength.computeIfAbsent(0, key -> new ArrayList<>()).add(State.builder()
            .termState(node.toTermState(null))
            .words(List.of())
            .prefix("")
            .len(0)
            .quoteLevel(0)
            .score(0.)
            .wordLengths(wordLengths)
            .build());
        TreeMultiset<State> bestFinalStates = TreeMultiset.create(scoreComparator);
        while (!statesByLength.isEmpty()) {
            statesByLength.remove(statesByLength.firstKey()).stream().sorted(scoreComparator).limit(SEARCH_LIMIT).forEach(state -> {
                if (bestFinalStates.size() >= NUM_RESULTS && state.score < bestFinalStates.lastEntry().getElement().score)
                    return;
                for (State newState : state.termState.newStates(state, context))
                    if (newState.termState != null)
                        statesByLength.computeIfAbsent(newState.len, key -> new ArrayList<>()).add(newState);
                    else
                        bestFinalStates.add(newState);
            });
            while (bestFinalStates.size() > NUM_RESULTS)
                bestFinalStates.pollLastEntry();
        }
        return bestFinalStates.stream()
            .limit(NUM_RESULTS)
            .map(state -> new CromulenceSolverResult(state.words, state.score))
            .collect(Collectors.toList());
    }

    @Data
    @Builder(toBuilder = true)
    static class State {
        final TermState termState;
        final List<String> words;
        final String prefix;
        final int len;
        final int quoteLevel;
        final List<Integer> wordLengths;
        final double score;
    }

    @Data
    class Context {

        double[] getNextLetterProbabilities(List<String> words, String prefix) {
            double[] freqs = Arrays.copyOf(getCachedFrequencies(prefix), NUM_LETTERS + 1);

            // bias toward words that appear in the biword list after the previous word
            if (!words.isEmpty())
                updateFrequencies(dictionaryManager.getWordFrequencies(words.get(words.size() - 1), prefix), prefix, freqs);
            double totalProb = 0;
            for (double prob : freqs)
                totalProb += prob;
            for (int i = 0; i < freqs.length; i++)
                freqs[i] /= totalProb;
            return freqs;
        }

        double[] getCachedFrequencies(String prefix) {
            if (nextLetterFreqsCache.containsKey(prefix))
                return nextLetterFreqsCache.get(prefix);
            double[] freqs = new double[NUM_LETTERS + 1];
            updateFrequencies(dictionaryManager.getWordFrequencies(prefix), prefix, freqs);
            if (prefix.length() <= 2)
                nextLetterFreqsCache.put(prefix, freqs);
            return freqs;
        }

        private void updateFrequencies(Map<String, Long> wordFreqs, String prefix, double[] freqs) {
            wordFreqs.forEach((word, frequency) -> {
                if (word.equals(prefix))
                    freqs[NUM_LETTERS] += frequency;
                else
                    freqs[word.charAt(prefix.length()) - 'A'] += frequency;
            });
        }
    }
}
