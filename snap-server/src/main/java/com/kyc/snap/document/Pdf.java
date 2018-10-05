package com.kyc.snap.document;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import com.kyc.snap.document.Document.DocumentText;

public class Pdf implements Closeable {

    /**
     * The scale to expand the PDF by when rendering as PNG. This number is greater than 1 to avoid
     * losing high resolution details at the default scale of 72 DPI.
     */
    public static final float RENDER_SCALE = 4f;

    private final PDDocument doc;
    private final PDFRenderer renderer;

    public Pdf(byte[] pdf) {
        try {
            doc = PDDocument.load(pdf);
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

    public List<DocumentText> getTexts(int page) {
        try {
            MyPDFTextStripper textStripper = new MyPDFTextStripper(page + 1);
            textStripper.getText(doc);
            return textStripper.allText;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        doc.close();
    }

    private static class MyPDFTextStripper extends PDFTextStripper {

        private final int pageNo;
        private PDPage page;
        private List<DocumentText> allText;

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
                allText.add(new DocumentText(position.getUnicode(), new Rectangle(
                    position.getX() * RENDER_SCALE,
                    (page.getMediaBox().getHeight() - position.getY()) * RENDER_SCALE,
                    position.getWidth() * RENDER_SCALE,
                    position.getHeight() * RENDER_SCALE)));
        }
    }
}
