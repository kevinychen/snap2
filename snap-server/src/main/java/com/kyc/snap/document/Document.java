package com.kyc.snap.document;

import java.util.List;

import lombok.Data;

@Data
public class Document {

    private final String id;
    private final List<DocumentPage> pages;

    @Data
    public static class DocumentPage {

        private final String imageId;
        private final List<DocumentText> texts;
    }

    @Data
    public static class DocumentText {

        private final String text;
        private final Rectangle bounds;
    }
}
