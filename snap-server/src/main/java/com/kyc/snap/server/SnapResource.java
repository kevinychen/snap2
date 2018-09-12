package com.kyc.snap.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.kyc.snap.crossword.CrosswordSpreadsheetManager;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;

import lombok.Data;

@Data
public class SnapResource implements SnapService {

    private final GoogleAPIManager googleApi;
    private final GridParser gridParser;

    private Map<String, ImageSession> sessions = new HashMap<>();

    @Override
    public String createImageSession(InputStream fileInputStream) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        BufferedImage image = ImageIO.read(fileInputStream);
        ImageSession session = new ImageSession(sessionId, image);
        sessions.put(sessionId, session);
        return sessionId;
    }

    @Override
    public GridLines findGridLines(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        GridLines lines = gridParser.findGridLines(session.getImage(), session.getApproxGridSize());
        session.setLines(lines);
        return lines;
    }

    @Override
    public GridLines getInterpolatedGridLines(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        GridLines lines = gridParser.getInterpolatedGridLines(session.getLines());
        session.setLines(lines);
        return lines;
    }

    @Override
    public GridLines findImplicitGridLines(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        GridLines lines = gridParser.findImplicitGridLines(session.getImage());
        session.setLines(lines);
        return lines;
    }

    @Override
    public Grid setGridColors(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        Grid grid = session.getGrid();
        gridParser.setGridColors(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public Grid setGridText(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        Grid grid = session.getGrid();
        gridParser.setGridText(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public Grid setGridBorders(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        Grid grid = session.getGrid();
        gridParser.setGridBorders(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public String toSpreadsheet(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        SpreadsheetManager spreadsheets = googleApi.getSheet(session.getSpreadsheetId(), session.getSheetId());
        CrosswordSpreadsheetManager crosswordSpreadsheets = new CrosswordSpreadsheetManager(spreadsheets);
        crosswordSpreadsheets.toSpreadsheet(session.getGrid());
        return spreadsheets.getUrl();
    }
}
