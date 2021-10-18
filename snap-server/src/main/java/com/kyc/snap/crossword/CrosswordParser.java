package com.kyc.snap.crossword;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.kyc.snap.crossword.Crossword.Entry;
import com.kyc.snap.crossword.CrosswordClues.ClueSection;
import com.kyc.snap.crossword.CrosswordClues.NumberedClue;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

public class CrosswordParser {

    private static final ClueDirection[] DIRECTIONS = { ClueDirection.ACROSS, ClueDirection.DOWN, ClueDirection.UNKNOWN };

    public Crossword parseCrossword(Grid grid) {
        Square[][] squares = grid.getSquares();
        CrosswordSquare[][] crosswordSquares = new CrosswordSquare[grid.getNumRows()][grid.getNumCols()];
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                boolean isOpen = ImageUtils.isLight(squares[i][j].getRgb());
                boolean canGoAcross = isOpen && j < grid.getNumCols() - 1 && ImageUtils.isLight(squares[i][j + 1].getRgb())
                        && squares[i][j].getRightBorder().getStyle().compareTo(Style.THIN) <= 0;
                boolean canGoDown = isOpen && i < grid.getNumRows() - 1 && ImageUtils.isLight(squares[i + 1][j].getRgb())
                        && squares[i][j].getBottomBorder().getStyle().compareTo(Style.THIN) <= 0;
                crosswordSquares[i][j] = new CrosswordSquare(isOpen, canGoAcross, canGoDown);
            }

        int clueNumber = 1;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                CrosswordSquare square = crosswordSquares[i][j];
                boolean hasEntry = false;
                if (square.canGoAcross && (j == 0 || !crosswordSquares[i][j - 1].canGoAcross)) {
                    int numSquares = 2;
                    while (crosswordSquares[i][j + numSquares - 1].canGoAcross)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, ClueDirection.ACROSS, clueNumber));
                    hasEntry = true;
                }
                if (square.canGoDown && (i == 0 || !crosswordSquares[i - 1][j].canGoDown)) {
                    int numSquares = 2;
                    while (crosswordSquares[i + numSquares - 1][j].canGoDown)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, ClueDirection.DOWN, clueNumber));
                    hasEntry = true;
                }
                if (hasEntry)
                    clueNumber++;
            }

        return new Crossword(grid.getNumRows(), grid.getNumCols(), entries);
    }

    public CrosswordClues parseClues(String text) {
        ClueDirection currentDirection = ClueDirection.UNKNOWN;
        int currentClueNumber = -1;
        String currentClue = "";
        List<Clue> clues = new ArrayList<>();
        for (String line : text.split("\n")) {
            String normalizedLine = line.trim().toUpperCase();
            boolean isDirection = false;
            for (ClueDirection direction : DIRECTIONS)
                if (normalizedLine.replaceAll("[^A-Z]", "").equals(direction.toString())) {
                    clues.add(new Clue(currentDirection, currentClueNumber, currentClue));
                    currentDirection = direction;
                    currentClue = "";
                    isDirection = true;
                }
            if (!normalizedLine.isEmpty() && Character.isDigit(normalizedLine.charAt(0))) {
                clues.add(new Clue(currentDirection, currentClueNumber, currentClue));
                currentClue = "";
                currentClueNumber = Integer.parseInt(normalizedLine.split("\\D+")[0]);
            }
            if (!normalizedLine.isEmpty() && !isDirection)
                currentClue += normalizedLine + " ";
        }
        clues.add(new Clue(currentDirection, currentClueNumber, currentClue));

        Multimap<ClueDirection, Clue> cluesByDirection = Multimaps.index(clues, clue -> clue.getDirection());
        List<ClueSection> sections = new ArrayList<>();
        for (ClueDirection direction : DIRECTIONS) {
            List<NumberedClue> sectionClues = cluesByDirection.get(direction).stream()
                .filter(clue -> !clue.clue.isEmpty())
                .sorted(Comparator.comparing(clue -> clue.clueNumber))
                .map(clue -> new NumberedClue(clue.clueNumber, clue.clue.trim()))
                .collect(Collectors.toList());
            sections.add(new ClueSection(direction, sectionClues));
        }
        return new CrosswordClues(sections);
    }

    public List<CrosswordFormula> getFormulas(Grid grid, Crossword crossword, CrosswordClues clues) {
        TreeMultimap<ClueDirection, Integer> numbersByDirection = TreeMultimap.create();
        for (Entry crosswordEntry : crossword.getEntries())
            numbersByDirection.put(crosswordEntry.getDirection(), crosswordEntry.getClueNumber());

        Map<ClueKey, String> cluesByNumber = new HashMap<>();
        for (ClueSection section : clues.getSections())
            for (NumberedClue clue : section.getClues())
                cluesByNumber.put(
                    new ClueKey(section.getDirection(), clue.getClueNumber()),
                    clue.getClue());
        for (Entry crosswordEntry : crossword.getEntries()) {
            ClueKey clueKey = new ClueKey(crosswordEntry.getDirection(),
                crosswordEntry.getClueNumber());
            if (!cluesByNumber.containsKey(clueKey)) {
                cluesByNumber.put(clueKey, "Clue " + crosswordEntry.getClueNumber());
            }
        }

        // map from each square in the crossword to the 1 or 2 answers containing it
        Multimap<Point, AnswerPosition> gridToAnswers = ArrayListMultimap.create();

        List<CrosswordFormula> formulas = new ArrayList<>();
        for (Entry crosswordEntry : crossword.getEntries()) {
            ClueKey clueKey = new ClueKey(crosswordEntry.getDirection(), crosswordEntry.getClueNumber());
            int clueRow = numbersByDirection.get(clueKey.direction).headSet(clueKey.clueNumber).size();
            int clueCol = grid.getNumCols() + 1 + 4 * clueKey.direction.ordinal();
            formulas.add(new CrosswordFormula(clueRow, clueCol, false, cluesByNumber.get(clueKey) , null));

            List<String> relativeRefs = new ArrayList<>();
            for (int k = 0; k < crosswordEntry.getNumSquares(); k++) {
                int row = crosswordEntry.getStartRow();
                int col = crosswordEntry.getStartCol();
                if (clueKey.direction == ClueDirection.ACROSS)
                    col += k;
                else if (clueKey.direction == ClueDirection.DOWN)
                    row += k;
                relativeRefs.add(
                    String.format("IF(%1$s=\"\", \".\", %1$s)",
                        String.format("R[%d]C[%d]", row - clueRow, col - (clueCol + 1))));
                gridToAnswers.put(
                    new Point(col, row),
                    new AnswerPosition(clueRow, clueCol + 2, k, clueKey.clueNumber));
            }
            formulas.add(new CrosswordFormula(clueRow, clueCol + 1, true,
                String.format(
                    "=CONCATENATE(%s, \" (%d)\")",
                    Joiner.on(',').join(relativeRefs),
                    crosswordEntry.getNumSquares()), null));
        }

        for (Point p : gridToAnswers.keySet()) {
            Collection<AnswerPosition> answers = gridToAnswers.get(p);
            String refArray = Joiner.on(";").join(answers.stream()
                .map(answer -> nthCharFormula(answer, p))
                .collect(Collectors.toList()));
            String filterArray = Joiner.on(";").join(answers.stream()
                .map(answer -> nthCharFormula(answer, p) + "<>\"\"")
                .collect(Collectors.toList()));
            String allCharsExpression = String.format(
                "UNIQUE(FILTER({%s},{%s}))",
                refArray,
                filterArray);
            Set<Integer> clueNumbers = answers.stream()
                .filter(answer -> answer.index == 0)
                .map(AnswerPosition::getClueNumber)
                .collect(Collectors.toSet());
            formulas.add(new CrosswordFormula(p.y, p.x, true,
                String.format("=IFERROR(IF(COUNTA(%1$s)>1,CONCATENATE(\"[\",JOIN(\"/\",%1$s),\"]\"),%1$s),\"\")",
                    allCharsExpression),
                clueNumbers.size() == 1 ? Iterables.getOnlyElement(clueNumbers) : null));
        }
        return formulas;
    }

    private static String nthCharFormula(AnswerPosition answer, Point formulaCell) {
        // the regex expression takes the nth character of a given string, treating (...) as a single character
        return String.format(
            "CONCATENATE(REGEXEXTRACT(%s,\"(?:[^\\(\\)]|\\([^\\)]*\\)){%d}(?:([^\\(\\)])|\\(([^\\)]*)\\))\"))",
            String.format("R[%d]C[%d]", answer.row - formulaCell.y, answer.col - formulaCell.x),
            answer.index);
    }

    @Data
    private static class CrosswordSquare {

        private final boolean isOpen;
        private final boolean canGoAcross;
        private final boolean canGoDown;
    }

    @Data
    private static class Clue {

        private final ClueDirection direction;
        private final int clueNumber;
        private final String clue;
    }

    @Data
    private static class ClueKey {

        private final ClueDirection direction;
        private final int clueNumber;
    }

    @Data
    private static class AnswerPosition {

        private final int row;
        private final int col;
        private final int index;
        private final int clueNumber;
    }
}
