package com.kyc.snap.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.grid.GridSpreadsheetWrapper;

import lombok.Data;

@Data
public class SnapResource implements SnapService {

    private final GoogleAPIManager googleApi;
    private final GridParser gridParser;

    private Map<String, ImageSession> sessions = new HashMap<>();

    @Override
    public String createImageSession(InputStream imageStream) throws IOException {
        String sessionId = UUID.randomUUID().toString();
        BufferedImage image = ImageIO.read(imageStream);
        ImageSession session = new ImageSession(sessionId, image);
        sessions.put(sessionId, session);
        return sessionId;
    }

    @Override
    public GridLines findGridLines(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        GridLines lines = gridParser.findGridLines(session.getImage(), session.getApproxGridSize());
        setLines(session, lines);
        return lines;
    }

    @Override
    public GridLines getInterpolatedGridLines(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        GridLines lines = gridParser.getInterpolatedGridLines(session.getLines());
        setLines(session, lines);
        return lines;
    }

    @Override
    public GridLines findImplicitGridLines(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        GridLines lines = gridParser.findImplicitGridLines(session.getImage());
        setLines(session, lines);
        return lines;
    }

    @Override
    public Grid findGridColors(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        Grid grid = session.getGrid();
        gridParser.findGridColors(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public Grid findGridText(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        Grid grid = session.getGrid();
        gridParser.findGridText(session.getImage(), session.getPos(), grid);
        return grid;
    }

    @Override
    public Grid findGridBorders(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        Grid grid = session.getGrid();
        gridParser.findGridBorders(session.getImage(), session.getPos(), grid);
        gridParser.findGridBorderStyles(grid);
        return grid;
    }

    @Override
    public String exportToSpreadsheet(String sessionId) {
        ImageSession session = sessions.get(sessionId);
        SpreadsheetManager spreadsheets = googleApi.getSheet(session.getSpreadsheetId(), session.getSheetId());
        GridSpreadsheetWrapper gridSpreadsheets = new GridSpreadsheetWrapper(spreadsheets);
        gridSpreadsheets.toSpreadsheet(session.getGrid());
        return spreadsheets.getUrl();
    }

    private void setLines(ImageSession session, GridLines lines) {
        session.setLines(lines);
        GridPosition pos = gridParser.getGridPosition(lines);
        session.setPos(pos);
        session.setGrid(new Grid(pos.getNumRows(), pos.getNumCols()));
    }
}
