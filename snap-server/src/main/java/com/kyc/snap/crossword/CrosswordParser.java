package com.kyc.snap.crossword;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.kyc.snap.crossword.Crossword.Entry;
import com.kyc.snap.crossword.CrosswordClues.ClueSection;
import com.kyc.snap.crossword.CrosswordClues.NumberedClue;
import com.kyc.snap.document.Document;
import com.kyc.snap.document.Document.DocumentPage;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

public class CrosswordParser {

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
        return parseClues(
            new CrosswordStats(0, 0, 1000000),
            List.of(new Block(Arrays.asList(text.split("\n")))));
    }

    public CrosswordClues parseClues(Document document, Crossword crossword) {
        List<DocumentText> texts = new ArrayList<>();
        for (DocumentPage page : document.getPages())
            texts.addAll(page.getTexts());

        int maxAcrossClueNumber = 0;
        int maxDownClueNumber = 0;
        int maxDimension = Math.max(crossword.getNumRows(), crossword.getNumCols());
        for (Entry crosswordEntry : crossword.getEntries()) {
            if (crosswordEntry.getDirection() == ClueDirection.ACROSS)
                maxAcrossClueNumber = Math.max(maxAcrossClueNumber, crosswordEntry.getClueNumber());
            else if (crosswordEntry.getDirection() == ClueDirection.DOWN)
                maxDownClueNumber = Math.max(maxDownClueNumber, crosswordEntry.getClueNumber());
        }

        List<Block> blocks = new ArrayList<>();
        LinkedList<String> lines = new LinkedList<>();
        StringBuilder currLine = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            DocumentText text = texts.get(i);
            currLine.append(text.getText());
            if (i == texts.size() - 1) {
                lines.add(currLine.toString());
                blocks.add(new Block(lines));
                continue;
            }
            DocumentText nextText = texts.get(i + 1);
            if (nextText.getBounds().getY() + nextText.getBounds().getHeight() < text.getBounds().getY()) {
                lines.add(currLine.toString());
                currLine.setLength(0);
                while (!lines.isEmpty() && !isClueStart(lines.get(0)))
                    lines.removeFirst();
                if (!lines.isEmpty())
                    blocks.add(new Block(new ArrayList<>(lines)));
                lines.clear();
            } else if (nextText.getBounds().getX() + nextText.getBounds().getWidth() < text.getBounds().getX()) {
                lines.add(currLine.toString());
                currLine.setLength(0);
            }
        }

        return parseClues(new CrosswordStats(maxAcrossClueNumber, maxDownClueNumber, maxDimension), blocks);
    }

    private static CrosswordClues parseClues(CrosswordStats stats, List<Block> blocks) {
        AtomicReference<List<Clue>> solution = new AtomicReference<>(new ArrayList<>());
        if (blocks.size() <= 10)
            findBestBlockOrder(stats, ClueDirection.UNKNOWN, 0, blocks, 0, new ArrayList<>(), solution);
        List<ClueSection> sections = new ArrayList<>();
        for (ClueDirection clueDirection : ClueDirection.values())
            if (clueDirection != ClueDirection.UNKNOWN) {
                sections.add(new ClueSection(clueDirection, solution.get().stream()
                    .filter(clue -> clue.direction == clueDirection)
                    .map(clue -> new NumberedClue(clue.clueNumber, clue.clue.toString()))
                    .collect(Collectors.toList())));
            }
        return new CrosswordClues(sections);
    }

    private static boolean isClueStart(String line) {
        return line.equalsIgnoreCase("ACROSS")
            || line.equalsIgnoreCase("DOWN")
            || line.matches("^[1-9][0-9]*.{5,}");
    }

    private static void findBestBlockOrder(
        CrosswordStats stats,
        ClueDirection clueDirection,
        int clueNumber,
        List<Block> blocks,
        long usedBitset,
        List<Clue> clues,
        AtomicReference<List<Clue>> solution) {
        if (clueDirection == ClueDirection.DOWN && clueNumber >= stats.maxDownClueNumber - stats.maxDimension) {
            solution.set(new ArrayList<>(clues));
            return;
        }
        for (int i = 0; i < blocks.size(); i++)
            if ((usedBitset & (1L << i)) == 0) {
                List<String> lines = blocks.get(i).getLines();
                int currCluesSize = clues.size();
                ClueDirection newClueDirection = clueDirection;
                int newClueNumber = clueNumber;
                boolean addedNewClue = false;
                for (String line : lines) {
                    if (newClueDirection == ClueDirection.UNKNOWN
                        && line.equalsIgnoreCase("ACROSS")) {
                        newClueDirection = ClueDirection.ACROSS;
                        newClueNumber = 0;
                    } else if (newClueDirection == ClueDirection.ACROSS
                        && newClueNumber >= stats.maxAcrossClueNumber - stats.maxDimension
                        && line.equalsIgnoreCase("DOWN")) {
                        newClueDirection = ClueDirection.DOWN;
                        newClueNumber = 0;
                    } else if (newClueDirection != ClueDirection.UNKNOWN) {
                        if (!line.isEmpty() && Character.isDigit(line.charAt(0))) {
                            int num = Integer.parseInt(line.split("\\D+")[0]);
                            if (num > newClueNumber && num <= newClueNumber + stats.maxDimension) {
                                newClueNumber = num;
                                addedNewClue = true;
                                clues.add(new Clue(newClueDirection, newClueNumber, new StringBuilder(line)));
                                continue;
                            }
                        }
                        if (addedNewClue)
                            clues.get(clues.size() - 1).getClue().append(" " + line);
                    }
                }
                if (addedNewClue)
                    findBestBlockOrder(stats, newClueDirection, newClueNumber, blocks, usedBitset | (1L << i), clues, solution);
                while (clues.size() > currCluesSize)
                    clues.remove(clues.size() - 1);
            }
    }

    public List<CrosswordFormula> getFormulas(Crossword crossword, CrosswordClues clues) {
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
        formulas.add(new CrosswordFormula(
            0, crossword.getNumCols() + 2, false, "Type answers here", null));
        formulas.add(new CrosswordFormula(
            0, crossword.getNumCols() + 5, false, "and here", null));
        for (Entry crosswordEntry : crossword.getEntries()) {
            ClueKey clueKey = new ClueKey(crosswordEntry.getDirection(), crosswordEntry.getClueNumber());
            int clueRow = numbersByDirection.get(clueKey.direction).headSet(clueKey.clueNumber).size() + 1;
            int clueCol = crossword.getNumCols() + 3 * clueKey.direction.ordinal();
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
    private static class CrosswordStats {

        private final int maxAcrossClueNumber;
        private final int maxDownClueNumber;
        private final int maxDimension;
    }

    @Data
    private static class Clue {

        private final ClueDirection direction;
        private final int clueNumber;
        private final StringBuilder clue;
    }

    @Data
    private static class Block {

        private final List<String> lines;
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
