package com.kyc.snap.words;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import com.kyc.snap.solver.EnglishTokens;

public class WordSearchSolver {

    private final Cache<List<Integer>, EnglishTrie> CACHED_ENGLISH_DICTIONARY_TRIES = Caffeine.newBuilder()
            .maximumSize(3)
            .build();

    public AllResults find(
            List<String> grid,
            Dictionary dictionary,
            boolean boggle,
            List<Integer> minLengths) {
        return find(
                grid.stream()
                        .map(String::toCharArray)
                        .toArray(char[][]::new),
                (currentPath, consumer) -> {
                    if (currentPath.isEmpty()) {
                        for (int y = 0; y < grid.size(); y++)
                            for (int x = 0; x < grid.get(y).length(); x++)
                                consumer.accept(new Point(x, y));
                    } else if (boggle || currentPath.size() == 1) {
                        for (int dx = -1; dx <= 1; dx++)
                            for (int dy = -1; dy <= 1; dy++)
                                consumer.accept(new Point(
                                        currentPath.getLast().x + dx,
                                        currentPath.getLast().y + dy));
                    } else {
                        Iterator<Point> it = currentPath.iterator();
                        Point p0 = it.next();
                        Point p1 = it.next();
                        consumer.accept(new Point(
                                currentPath.getLast().x + (p1.x - p0.x),
                                currentPath.getLast().y + (p1.y - p0.y)));
                    }
                },
                dictionary,
                minLengths);
    }

    private AllResults find(
            char[][] grid,
            NeighborFunction neighborFunction,
            Dictionary dictionary,
            List<Integer> minLengths) {
        for (char[] row : grid)
            for (int i = 0; i < row.length; i++)
                row[i] = Character.toUpperCase(row[i]);

        int maxNumDeletions = minLengths.size() - 1;

        EnglishTrie trie = dictionary instanceof EnglishDictionary
                ? CACHED_ENGLISH_DICTIONARY_TRIES.get(
                        minLengths, _key -> EnglishTrie.of(dictionary, maxNumDeletions))
                : EnglishTrie.of(dictionary, maxNumDeletions);

        List<Result> results = new ArrayList<>();
        int[] limit = {10000000};
        new SolverInstance(
                grid,
                neighborFunction,
                minLengths,
                trie,
                new ArrayDeque<>(),
                new HashSet<>(),
                new StringBuilder(),
                new ArrayDeque<>(List.of(List.of(new State(trie.startNodeIndex(), 0)))),
                results,
                limit).run();
        results.sort(Comparator.comparing(result -> -dictionary.getWordFrequencies().get(result.word)
                * Math.pow(EnglishTokens.NUM_LETTERS, result.word.length() - 2 * result.levenshteinDistance)));

        List<Result> filteredResults = new ArrayList<>();
        for (Result result : results)
            if (filteredResults.stream().allMatch(otherResult -> !result.word.equals(otherResult.word)
                    || Sets.symmetricDifference(Set.copyOf(result.path), Set.copyOf(otherResult.path)).size() > 3)) {
                filteredResults.add(result);
                if (filteredResults.size() == 200)
                    break;
            }
        return new AllResults(filteredResults, limit[0] == 0);
    }

    public record AllResults(List<Result> results, boolean hitLimit) {}

    public record Result(List<Point> path, String word, int levenshteinDistance) {}

    interface NeighborFunction {
        void forEachNeighbor(Deque<Point> currentPath, Consumer<Point> consumer);
    }

    private record SolverInstance(
            char[][] grid,
            NeighborFunction neighborFunction,
            List<Integer> minLengths,
            EnglishTrie trie,
            Deque<Point> path,
            Set<Point> points,
            StringBuilder string,
            Deque<List<State>> allStates,
            List<Result> results,
            int[] limit) {

        void run() {
            if (limit[0] == 0)
                return;
            limit[0]--;

            if (allStates.getLast().isEmpty())
                return;

            for (State state : allStates.getLast())
                for (String word : trie.getWords(state.nodeIndex)) {
                    int d = StringUtil.levenshteinDistance(word, string);
                    if (d < minLengths.size() && word.length() >= minLengths.get(d))
                        results.add(new Result(List.copyOf(path), word, d));
                }

            neighborFunction.forEachNeighbor(path, neighbor -> {
                if (neighbor.y >= 0
                        && neighbor.y < grid.length
                        && neighbor.x >= 0
                        && neighbor.x < grid[neighbor.y].length
                        && !points.contains(neighbor)) {
                    char c = grid[neighbor.y][neighbor.x];
                    if (c < 'A' || c > 'Z')
                        return;

                    path.add(neighbor);
                    points.add(neighbor);
                    string.append(c);

                    List<State> newStates = new ArrayList<>();
                    for (State state : allStates.getLast()) {
                        int nodeIndex = trie.getNodeIndex(state.nodeIndex, c);
                        if (nodeIndex != 0)
                            newStates.add(new State(nodeIndex, state.numDeletions));
                        if (state.numDeletions < minLengths.size() - 1)
                            newStates.add(new State(state.nodeIndex, state.numDeletions + 1));
                    }
                    allStates.add(newStates);

                    run();

                    Point p = path.removeLast();
                    points.remove(p);
                    string.setLength(string.length() - 1);
                    allStates.removeLast();
                }
            });
        }
    }

    private record State(int nodeIndex, int numDeletions) {}
}
