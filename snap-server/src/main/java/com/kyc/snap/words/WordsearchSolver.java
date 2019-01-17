package com.kyc.snap.words;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import lombok.Data;

@Data
public class WordsearchSolver {

    private static final int MAX_RESULTS = 500;

    private final DictionaryManager dictionary;

    public List<Result> find(List<String> grid, boolean boggle) {
        int width = grid.stream().mapToInt(String::length).max().orElse(0);
        for (int i = 0; i < grid.size(); i++)
            grid.set(i, Strings.padEnd(grid.get(i), width, '.'));

        Set<String> words = dictionary.getWords();
        List<Point> positions = new ArrayList<>();
        List<Result> results = new ArrayList<>();
        if (boggle) {
            boolean[][] used = new boolean[grid.size()][width];
            for (String word : words)
                for (int i = 0; i < grid.size(); i++)
                    for (int j = 0; j < width; j++)
                        if (grid.get(i).charAt(j) == word.charAt(0))
                            findBoggleHelper(grid, word, 1, i, j, positions, used, results);
        } else {
            for (int i = 0; i < grid.size(); i++)
                for (int j = 0; j < width; j++)
                    for (int di = -1; di <= 1; di++)
                        for (int dj = -1; dj <= 1; dj++)
                            if (di != 0 || dj != 0) {
                                String word = "";
                                for (int row = i, col = j; row >= 0 && row < grid.size() && col >= 0
                                        && col < grid.size(); row += di, col += dj) {
                                    word += grid.get(row).charAt(col);
                                    positions.add(new Point(col, row));
                                    if (words.contains(word))
                                        results.add(new Result(word, new ArrayList<>(positions)));
                                }
                                positions.clear();
                            }
        }
        return results.stream()
                .sorted(Comparator.comparing(result -> -result.positions.size()))
                .limit(MAX_RESULTS)
                .collect(Collectors.toList());
    }

    private void findBoggleHelper(List<String> grid, String word, int index, int row, int col, List<Point> positions, boolean[][] used,
            List<Result> results) {
        if (index == word.length()) {
            results.add(new Result(word, new ArrayList<>(positions)));
            return;
        }
        positions.add(new Point(col, row));
        used[row][col] = true;
        for (int nrow = row - 1; nrow <= row + 1; nrow++)
            for (int ncol = col - 1; ncol <= col + 1; ncol++)
                if (nrow >= 0 && nrow < grid.size() && ncol >= 0 && ncol < grid.get(nrow).length()
                        && !used[nrow][ncol] && grid.get(nrow).charAt(ncol) == word.charAt(index))
                    findBoggleHelper(grid, word, index + 1, nrow, ncol, positions, used, results);
        positions.remove(positions.size() - 1);
        used[row][col] = false;
    }

    @Data
    public static class Result {

        private final String word;
        private final List<Point> positions;
    }
}
