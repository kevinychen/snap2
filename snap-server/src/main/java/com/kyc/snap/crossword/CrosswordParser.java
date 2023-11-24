package com.kyc.snap.crossword;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;
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

public class CrosswordParser {

    public Crossword parseCrossword(Grid grid) {
        return Stream.of(Style.THIN, Style.THICK)
            .map(maxBorderStyle -> parseCrossword(grid, maxBorderStyle))
            .max(Comparator.comparing(crossword -> crossword.entries().size()))
            .get();
    }

    private Crossword parseCrossword(Grid grid, Style maxBorderStyle) {
        Square[][] squares = grid.squares();
        CrosswordSquare[][] crosswordSquares = new CrosswordSquare[grid.numRows()][grid.numCols()];
        for (int i = 0; i < grid.numRows(); i++)
            for (int j = 0; j < grid.numCols(); j++) {
                boolean isOpen = ImageUtils.isLight(squares[i][j].rgb);
                boolean canGoAcross = isOpen && j < grid.numCols() - 1 && ImageUtils.isLight(squares[i][j + 1].rgb)
                        && squares[i][j].rightBorder.style.compareTo(maxBorderStyle) <= 0;
                boolean canGoDown = isOpen && i < grid.numRows() - 1 && ImageUtils.isLight(squares[i + 1][j].rgb)
                        && squares[i][j].bottomBorder.style.compareTo(maxBorderStyle) <= 0;
                crosswordSquares[i][j] = new CrosswordSquare(isOpen, canGoAcross, canGoDown);
            }

        int clueNumber = 1;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < grid.numRows(); i++)
            for (int j = 0; j < grid.numCols(); j++) {
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

        return new Crossword(grid.numRows(), grid.numCols(), entries);
    }

    public CrosswordClues parseClues(String text) {
        return parseClues(
            new CrosswordStats(0, 0, 1000000),
            List.of(new Block(Arrays.asList(text.split("\n")))));
    }

    public CrosswordClues parseClues(Document document, Crossword crossword) {
        List<DocumentText> texts = new ArrayList<>();
        for (DocumentPage page : document.pages())
            texts.addAll(page.texts());

        int maxAcrossClueNumber = 0;
        int maxDownClueNumber = 0;
        int maxDimension = Math.max(crossword.numRows(), crossword.numCols());
        for (Entry crosswordEntry : crossword.entries()) {
            if (crosswordEntry.direction() == ClueDirection.ACROSS)
                maxAcrossClueNumber = Math.max(maxAcrossClueNumber, crosswordEntry.clueNumber());
            else if (crosswordEntry.direction() == ClueDirection.DOWN)
                maxDownClueNumber = Math.max(maxDownClueNumber, crosswordEntry.clueNumber());
        }
        return parseClues(maxAcrossClueNumber, maxDownClueNumber, maxDimension, texts);
    }

    @VisibleForTesting
    CrosswordClues parseClues(int maxAcrossClueNumber, int maxDownClueNumber,
        int maxDimension, List<DocumentText> texts) {
        List<Block> blocks = new ArrayList<>();
        LinkedList<String> lines = new LinkedList<>();
        StringBuilder currLine = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            DocumentText text = texts.get(i);
            currLine.append(text.text());
            DocumentText nextText = i + 1 == texts.size() ? null : texts.get(i + 1);
            if (nextText == null
                || nextText.bounds().y() + nextText.bounds().height() < text.bounds().y()
                || nextText.bounds().x() > text.bounds().x() + 10 * text.bounds().width()) {
                lines.add(currLine.toString().replaceAll("(^\\h*)|(\\h*$)",""));
                currLine.setLength(0);
                while (!lines.isEmpty() && !isClueStart(lines.get(0)))
                    lines.removeFirst();
                if (!lines.isEmpty())
                    blocks.add(new Block(new ArrayList<>(lines)));
                lines.clear();
            } else if (nextText.bounds().y() > text.bounds().y() + text.bounds().height()) {
                lines.add(currLine.toString().replaceAll("(^\\h*)|(\\h*$)",""));
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
                        .toList()));
            }
        return new CrosswordClues(sections);
    }

    private static boolean isClueStart(String line) {
        return line.equalsIgnoreCase("ACROSS")
            || line.equalsIgnoreCase("DOWN")
            || line.matches("^[1-9][0-9]*[^0-9].{3,}");
    }

    private static void findBestBlockOrder(
        CrosswordStats stats,
        ClueDirection clueDirection,
        int clueNumber,
        List<Block> blocks,
        long usedBitset,
        List<Clue> clues,
        AtomicReference<List<Clue>> solution) {
        if (clueDirection == ClueDirection.DOWN
            && clueNumber >= stats.maxDownClueNumber - stats.maxDimension
            && clues.size() > solution.get().size()) {
            solution.set(new ArrayList<>(clues));
        }
        for (int i = 0; i < blocks.size(); i++)
            if ((usedBitset & (1L << i)) == 0) {
                List<String> lines = blocks.get(i).lines();
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
                            clues.get(clues.size() - 1).clue().append(" ").append(line);
                    }
                }
                if (newClueDirection != clueDirection || newClueNumber != clueNumber)
                    findBestBlockOrder(stats, newClueDirection, newClueNumber, blocks, usedBitset | (1L << i), clues, solution);
                while (clues.size() > currCluesSize)
                    clues.remove(clues.size() - 1);
            }
    }

    public List<CrosswordFormula> getFormulas(Crossword crossword, CrosswordClues clues) {
        TreeMultimap<ClueDirection, Integer> numbersByDirection = TreeMultimap.create();
        for (Entry crosswordEntry : crossword.entries())
            numbersByDirection.put(crosswordEntry.direction(), crosswordEntry.clueNumber());

        Map<ClueKey, String> cluesByNumber = new HashMap<>();
        for (ClueSection section : clues.sections())
            for (NumberedClue clue : section.clues())
                cluesByNumber.put(
                    new ClueKey(section.direction(), clue.clueNumber()),
                    clue.clue());
        for (Entry crosswordEntry : crossword.entries()) {
            ClueKey clueKey = new ClueKey(crosswordEntry.direction(),
                crosswordEntry.clueNumber());
            if (!cluesByNumber.containsKey(clueKey)) {
                cluesByNumber.put(clueKey, "Clue " + crosswordEntry.clueNumber());
            }
        }

        // map from each square in the crossword to the 1 or 2 answers containing it
        Multimap<Point, AnswerPosition> gridToAnswers = ArrayListMultimap.create();

        List<CrosswordFormula> formulas = new ArrayList<>();
        formulas.add(new CrosswordFormula(
                0, crossword.numCols() + 1, false, "Across", null));
        formulas.add(new CrosswordFormula(
                0, crossword.numCols() + 3, false, "Type here", null));
        formulas.add(new CrosswordFormula(
                0, crossword.numCols() + 4, false, "Down", null));
        formulas.add(new CrosswordFormula(
                0, crossword.numCols() + 6, false, "and here", null));
        for (Entry crosswordEntry : crossword.entries()) {
            ClueKey clueKey = new ClueKey(crosswordEntry.direction(), crosswordEntry.clueNumber());
            int clueRow = numbersByDirection.get(clueKey.direction).headSet(clueKey.clueNumber).size() + 1;
            int clueCol = crossword.numCols() + 3 * clueKey.direction.ordinal() + 1;
            formulas.add(new CrosswordFormula(clueRow, clueCol, false, cluesByNumber.get(clueKey) , null));

            List<String> relativeRefs = new ArrayList<>();
            for (int k = 0; k < crosswordEntry.numSquares(); k++) {
                int row = crosswordEntry.startRow();
                int col = crosswordEntry.startCol();
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
                    String.join(",", relativeRefs),
                    crosswordEntry.numSquares()), null));
        }

        for (Point p : gridToAnswers.keySet()) {
            Collection<AnswerPosition> answers = gridToAnswers.get(p);
            String refArray = answers.stream()
                    .map(answer -> nthCharFormula(answer, p))
                    .collect(Collectors.joining(";"));
            String filterArray = answers.stream()
                    .map(answer -> nthCharFormula(answer, p) + "<>\"\"")
                    .collect(Collectors.joining(";"));
            String allCharsExpression = String.format(
                    "UNIQUE(FILTER({%s},{%s}))",
                    refArray,
                    filterArray);
            Set<Integer> clueNumbers = answers.stream()
                    .filter(answer -> answer.index == 0)
                    .map(AnswerPosition::clueNumber)
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

    private record CrosswordSquare(boolean isOpen, boolean canGoAcross, boolean canGoDown) {}

    private record CrosswordStats(int maxAcrossClueNumber, int maxDownClueNumber, int maxDimension) {}

    private record Clue(ClueDirection direction, int clueNumber, StringBuilder clue) {}

    private record Block(List<String> lines) {}

    private record ClueKey(ClueDirection direction, int clueNumber) {}

    private record AnswerPosition(int row, int col, int index, int clueNumber) {}
}
