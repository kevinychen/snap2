package com.kyc.snap.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeSet;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.crossword.CrosswordSpreadsheetWrapper;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.Dimension;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;
import com.kyc.snap.grid.GridSpreadsheetWrapper;
import com.kyc.snap.image.ImageSpreadsheetWrapper;
import com.kyc.snap.server.ImageSession.Parameters;

import lombok.Data;

@Data
public class ImageResource implements ImageService {

    public static final int DEFAULT_SHEET_ROW_OFFSET = 0;
    public static final int DEFAULT_SHEET_COL_OFFSET = 0;
    public static final int DEFAULT_CELL_LENGTH = 21;

    private final SnapConfiguration configuration;
    private final GoogleAPIManager googleApi;
    private final GridParser gridParser;
    private final CrosswordParser crosswordParser;

    private Cache<String, ImageSession> sessions = CacheBuilder.newBuilder()
            .maximumSize(64)
            .<String, ImageSession>build();

    @Override
    public StringJson createImageSession(InputStream imageStream) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        BufferedImage image = ImageIO.read(imageStream);
        ImageSession session = new ImageSession(sessionId, image);
        sessions.put(sessionId, session);
        return new StringJson(sessionId);
    }

    @Override
    public Parameters setParameters(String sessionId, Integer approxGridSize, String spreadsheetId, Integer sheetId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Parameters parameters = session.getParameters();
        if (approxGridSize != null)
            parameters.setApproxGridSize(approxGridSize);
        if (spreadsheetId != null && !spreadsheetId.isEmpty())
            parameters.setSpreadsheetId(spreadsheetId);
        if (sheetId != null)
            parameters.setSheetId(sheetId);
        return parameters;
    }

    @Override
    public GridLines findGridLines(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        GridLines lines = gridParser.findGridLines(session.getImage(), session.getParameters().getApproxGridSize());
        session.setLines(lines);
        return lines;
    }

    @Override
    public GridLines getInterpolatedGridLines(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        GridLines lines = gridParser.getInterpolatedGridLines(session.getLines());
        session.setLines(lines);
        return lines;
    }

    @Override
    public GridLines findImplicitGridLines(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        GridLines lines = gridParser.findImplicitGridLines(session.getImage());
        session.setLines(lines);
        return lines;
    }

    @Override
    public GridLines setManualGridLines(String sessionId, String horizontalLines, String verticalLines) {
        ImageSession session = sessions.getIfPresent(sessionId);
        GridLines lines = new GridLines(integerSetFromCSV(horizontalLines), integerSetFromCSV(verticalLines));
        session.setLines(lines);
        return lines;
    }

    @Override
    public GridPosition getGridPosition(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        GridPosition pos = gridParser.getGridPosition(session.getLines());
        session.setPos(pos);
        session.setGrid(Grid.create(pos.getNumRows(), pos.getNumCols()));
        return pos;
    }

    @Override
    public BufferedImage getSubimage(String sessionId, int row, int col) {
        ImageSession session = sessions.getIfPresent(sessionId);
        GridPosition pos = session.getPos();
        Row gridRow = pos.getRows().get(row);
        Col gridCol = pos.getCols().get(col);
        return session.getImage().getSubimage(gridCol.getStartX(), gridRow.getStartY(), gridCol.getWidth(), gridRow.getHeight());
    }

    @Override
    public Grid findGridColors(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Grid grid = session.getGrid();
        gridParser.findGridColors(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public Grid findGridText(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Grid grid = session.getGrid();
        gridParser.findGridText(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public Grid findGridBorders(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Grid grid = session.getGrid();
        gridParser.findGridBorders(session.getImage(), session.getPos(), grid);
        gridParser.findGridBorderStyles(grid);
        return grid;
    }

    @Override
    public StringJson exportGridToSpreadsheet(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Parameters parameters = session.getParameters();
        SpreadsheetManager spreadsheets = googleApi.getSheet(parameters.getSpreadsheetId(), parameters.getSheetId());
        GridSpreadsheetWrapper gridSpreadsheets = new GridSpreadsheetWrapper(spreadsheets, DEFAULT_SHEET_ROW_OFFSET,
            DEFAULT_SHEET_COL_OFFSET);
        spreadsheets.clear();
        spreadsheets.setAllRowOrColumnSizes(Dimension.ROWS, DEFAULT_CELL_LENGTH);
        spreadsheets.setAllRowOrColumnSizes(Dimension.COLUMNS, DEFAULT_CELL_LENGTH);
        gridSpreadsheets.toSpreadsheet(session.getPos(), session.getGrid(), GridSpreadsheetWrapper.DEFAULT_SCALE);
        return new StringJson(spreadsheets.getUrl());
    }

    @Override
    public StringJson exportImagesToSpreadsheet(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Parameters parameters = session.getParameters();
        SpreadsheetManager spreadsheets = googleApi.getSheet(parameters.getSpreadsheetId(), parameters.getSheetId());
        ImageSpreadsheetWrapper imageSpreadsheets = new ImageSpreadsheetWrapper(configuration, spreadsheets);
        imageSpreadsheets.toSpreadsheet(sessionId, session.getImage(), session.getPos());
        return new StringJson(spreadsheets.getUrl());
    }

    @Override
    public Crossword parseCrossword(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Crossword crossword = crosswordParser.parseCrossword(session.getGrid());
        session.setCrossword(crossword);
        return crossword;
    }

    @Override
    public CrosswordClues parseCrosswordClues(String sessionId, String unparsedClues) {
        ImageSession session = sessions.getIfPresent(sessionId);
        CrosswordClues clues = crosswordParser.parseClues(unparsedClues);
        session.setClues(clues);
        return clues;
    }

    @Override
    public StringJson exportCrosswordToSpreadsheet(String sessionId) {
        ImageSession session = sessions.getIfPresent(sessionId);
        Parameters parameters = session.getParameters();
        SpreadsheetManager spreadsheets = googleApi.getSheet(parameters.getSpreadsheetId(), parameters.getSheetId());
        CrosswordSpreadsheetWrapper crosswordSpreadsheets = new CrosswordSpreadsheetWrapper(spreadsheets, 0, 0);
        crosswordSpreadsheets.toSpreadsheet(session.getGrid(), session.getCrossword(), session.getClues());
        return new StringJson(spreadsheets.getUrl());
    }

    private TreeSet<Integer> integerSetFromCSV(String values) {
        TreeSet<Integer> set = new TreeSet<Integer>();
        for (String value : values.split(","))
            set.add(Integer.parseInt(value));
        return set;
    }
}
