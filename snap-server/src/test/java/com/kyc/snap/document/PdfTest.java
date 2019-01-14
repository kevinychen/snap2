package com.kyc.snap.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.opencv.OpenCvManager.OcrMode;

public class PdfTest {

    File pdfFile = new File("src/test/resources/dummy.pdf");
    String expectedText = "Dummy PDF file";

    @Test
    public void test() throws IOException {
        try (Pdf pdf = new Pdf(Files.toByteArray(pdfFile))) {
            assertThat(pdf.getNumPages()).isEqualTo(1);

            List<DocumentText> texts = pdf.getTexts(0);
            assertThat(texts.stream().map(text -> text.getText()).reduce("", String::concat)).isEqualTo(expectedText);

            BufferedImage image = pdf.toImage(0);
            assertThat(new OpenCvManager().batchFindText(ImmutableList.of(image), OcrMode.DEFAULT).get(image).trim())
                .isEqualTo(expectedText);
        }
    }
}
