package com.kyc.snap.words;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import lombok.Data;

@Data
public class WordsearchSolver {

    private static final int MAX_RESULTS = 500;

    private final DictionaryManager dictionary;

    public List<Result> find(List<String> grid, Set<String> wordbank, boolean boggle) {
        int width = grid.stream().mapToInt(String::length).max().orElse(0);
        for (int i = 0; i < grid.size(); i++)
            grid.set(i, Strings.padEnd(grid.get(i).toUpperCase(), width, '.'));

        Map<String, Long> wordFrequencies = dictionary.getWordFrequencies();
        Set<String> words = Sets.union(wordFrequencies.keySet(), wordbank);

        List<Point> positions = new ArrayList<>();
        List<Result> results = new ArrayList<>();
        if (boggle) {
            boolean[][] used = new boolean[grid.size()][width];
            for (String word : words)
                for (int i = 0; i < grid.size(); i++)
                    for (int j = 0; j < width; j++)
                        if (grid.get(i).charAt(j) == word.charAt(0))
                            findBoggleHelper(grid, word, wordbank.contains(word), 1, i, j, positions, used, results);
        } else {
            for (int i = 0; i < grid.size(); i++)
                for (int j = 0; j < width; j++)
                    for (int di = -1; di <= 1; di++)
                        for (int dj = -1; dj <= 1; dj++)
                            if (di != 0 || dj != 0) {
                                String word = "";
                                for (int row = i, col = j; row >= 0 && row < grid.size() && col >= 0
                                        && col < width; row += di, col += dj) {
                                    word += grid.get(row).charAt(col);
                                    positions.add(new Point(col, row));
                                    if (word.length() >= 2 && words.contains(word))
                                        results.add(new Result(word, new ArrayList<>(positions), wordbank.contains(word)));
                                }
                                positions.clear();
                            }
        }
        return results.stream()
        		.sorted(Comparator.<Result, Boolean>comparing(result -> !result.inWordbank)
                        .thenComparing(result -> (result.inWordbank ? -result.word.length() : -Math.sqrt(wordFrequencies.getOrDefault(result.word, 1L))) * Math.pow(26, result.word.length())))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
    }

    private void findBoggleHelper(List<String> grid, String word, boolean inWordbank, int index, int row, int col, List<Point> positions, boolean[][] used,
            List<Result> results) {
        positions.add(new Point(col, row));
        used[row][col] = true;
        if (index == word.length()) {
            results.add(new Result(word, new ArrayList<>(positions), inWordbank));
        } else {
            for (int nrow = row - 1; nrow <= row + 1; nrow++)
                for (int ncol = col - 1; ncol <= col + 1; ncol++)
                    if (nrow >= 0 && nrow < grid.size() && ncol >= 0 && ncol < grid.get(nrow).length()
                    && !used[nrow][ncol] && grid.get(nrow).charAt(ncol) == word.charAt(index))
                        findBoggleHelper(grid, word, inWordbank, index + 1, nrow, ncol, positions, used, results);
        }
        positions.remove(positions.size() - 1);
        used[row][col] = false;
    }

    @Data
    public static class Result {

        private final String word;
        private final List<Point> positions;
        private final boolean inWordbank;
    }
}
