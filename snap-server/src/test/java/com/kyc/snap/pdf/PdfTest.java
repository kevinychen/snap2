package com.kyc.snap.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.pdf.Pdf.TextWithBounds;

public class PdfTest {

    File pdfFile = new File("src/test/resources/dummy.pdf");
    String expectedText = "Dummy PDF file";

    @Test
    public void test() {
        Pdf pdf = new Pdf(pdfFile);
        assertThat(pdf.getNumPages()).isEqualTo(1);

        List<TextWithBounds> texts = pdf.getTexts(0);
        assertThat(texts.stream().map(text -> text.getText()).reduce("", String::concat)).isEqualTo(expectedText);

        BufferedImage image = pdf.toImage(0);
        assertThat(new GoogleAPIManager().batchFindText(ImmutableList.of(image)).get(image).trim()).isEqualTo(expectedText);
    }
}
