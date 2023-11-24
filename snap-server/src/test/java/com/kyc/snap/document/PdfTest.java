package com.kyc.snap.document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.kyc.snap.document.Document.DocumentText;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfTest {

    final File pdfFile = new File("src/test/resources/dummy.pdf");
    final String expectedText = "Dummy PDF file";

    @Test
    public void test() throws IOException {
        try (Pdf pdf = new Pdf(new FileInputStream(pdfFile))) {
            assertThat(pdf.getNumPages()).isEqualTo(1);

            List<DocumentText> texts = pdf.getTexts(0);
            assertThat(texts.stream().map(DocumentText::text).reduce("", String::concat)).isEqualTo(expectedText);
        }
    }
}
