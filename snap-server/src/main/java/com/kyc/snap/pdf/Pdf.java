package com.kyc.snap.pdf;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import lombok.Data;

public class Pdf {

    /**
     * The scale to expand the PDF by when rendering as PNG. This number is greater than 1 to avoid
     * losing high resolution details at the default scale of 72 DPI.
     */
    public static final float RENDER_SCALE = 4f;

    private final PDDocument doc;
    private final PDFRenderer renderer;

    public Pdf(File pdfFile) {
        try {
            doc = PDDocument.load(pdfFile);
            renderer = new PDFRenderer(doc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getNumPages() {
        return doc.getNumberOfPages();
    }

    public BufferedImage toImage(int page) {
        try {
            return renderer.renderImage(page, RENDER_SCALE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<TextWithBounds> getTexts(int page) {
        try {
            MyPDFTextStripper textStripper = new MyPDFTextStripper(page + 1);
            textStripper.getText(doc);
            return textStripper.allText;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class TextWithBounds {

        private final String text;
        private final Rectangle2D bounds;
    }

    private static class MyPDFTextStripper extends PDFTextStripper {

        private final int pageNo;
        private PDPage page;
        private List<TextWithBounds> allText;

        public MyPDFTextStripper(int pageNo) throws IOException {
            this.pageNo = pageNo;
        }

        @Override
        public void processPage(PDPage page) throws IOException {
            if (getCurrentPageNo() == pageNo) {
                this.page = page;
                allText = new ArrayList<>();
                super.processPage(page);
            }
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            for (TextPosition position : textPositions)
                allText.add(new TextWithBounds(position.getUnicode(), new Rectangle2D.Double(
                    position.getX() * RENDER_SCALE,
                    (page.getMediaBox().getHeight() - position.getY()) * RENDER_SCALE,
                    position.getWidth() * RENDER_SCALE,
                    position.getHeight() * RENDER_SCALE)));
        }
    }
}
