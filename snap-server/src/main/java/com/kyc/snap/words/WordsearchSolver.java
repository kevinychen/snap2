package com.kyc.snap.words;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class WordsearchSolver {

    private final DictionaryManager dictionary;

    public Set<Result> find(List<String> grid) {
        Set<Result> results = new HashSet<>();
        for (String word : dictionary.getWordFrequencies().keySet())
            for (int i = 0; i < grid.size(); i++)
                for (int j = 0; j < grid.get(i).length(); j++)
                    if (grid.get(i).charAt(j) == word.charAt(0))
                        findHelper(grid, word, 1, i, j, new ArrayList<>(), results);
        return results;
    }

    private void findHelper(List<String> grid, String word, int index, int row, int col, List<Point> positions, Set<Result> results) {
        positions.add(new Point(col, row));
        if (index == word.length()) {
            if (new HashSet<>(positions).size() == positions.size())
                results.add(new Result(word, new ArrayList<>(positions)));
            return;
        }
        for (int nrow = row - 1; nrow <= row + 1; nrow++)
            for (int ncol = col - 1; ncol <= col + 1; ncol++)
                if (nrow >= 0 && nrow < grid.size() && ncol >= 0 && ncol < grid.get(nrow).length()
                        && grid.get(nrow).charAt(ncol) == word.charAt(index))
                    findHelper(grid, word, index + 1, nrow, ncol, positions, results);
        positions.remove(positions.size() - 1);
    }

    @Data
    public static class Result {

        private final String word;
        private final List<Point> positions;
    }
}
