package com.kyc.snap.crossword;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.kyc.snap.crossword.CrosswordClues.NumberedClue;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Pdf;

import static org.assertj.core.api.Assertions.assertThat;

public class CrosswordParserTest {

    final CrosswordParser crosswordParser = new CrosswordParser();

    @Test
    public void testParseClues() throws IOException {
        try (InputStream in = new FileInputStream("./src/test/resources/kitchen_rush.pdf"); Pdf pdf = new Pdf(in)) {
            List<DocumentText> texts = new ArrayList<>();
            for (int page = 0; page < pdf.getNumPages(); page++)
                texts.addAll(pdf.getTexts(page));
            CrosswordClues clues = crosswordParser.parseClues(86, 75, 17, texts);
            assertThat(clues.sections().get(0).clues())
                .extracting(NumberedClue::clueNumber)
                .containsExactly(1, 6, 11, 16, 17, 18, 19, 21, 22, 23, 25, 26, 30, 33, 35, 36, 38
                    , 39, 41, 43, 44, 45, 47, 49, 52, 54, 56, 57, 59, 61, 62, 63, 66, 67, 69, 71,
                    72, 76, 79, 81, 82, 83, 84, 85, 86);
            assertThat(clues.sections().get(1).clues())
                .extracting(NumberedClue::clueNumber)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 20, 24, 26, 27,
                    28, 29, 30, 31, 32, 34, 37, 40, 42, 46, 48, 50, 51, 53, 55, 57, 58, 60, 64,
                    65, 68, 70, 72, 73, 74, 75, 76, 77, 78, 80);
        }
        try (InputStream in = new FileInputStream("./src/test/resources/meatballs.pdf"); Pdf pdf = new Pdf(in)) {
            List<DocumentText> texts = new ArrayList<>();
            for (int page = 0; page < pdf.getNumPages(); page++)
                texts.addAll(pdf.getTexts(page));
            CrosswordClues clues = crosswordParser.parseClues(83, 77, 17, texts);
            assertThat(clues.sections().get(0).clues())
                .extracting(NumberedClue::clueNumber)
                .containsExactly(1, 6, 12, 16, 17, 18, 19, 20, 21, 22, 23, 24, 26, 28, 30, 32, 33
                    , 34, 35, 39, 40, 41, 43, 47, 48, 49, 50, 51, 52, 53, 55, 57, 60, 62, 63, 64,
                    66, 68, 69, 71, 74, 75, 77, 78, 79, 80, 81, 82, 83);
            assertThat(clues.sections().get(1).clues())
                .extracting(NumberedClue::clueNumber)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 23, 25, 27, 29,
                    30, 31, 36, 37, 38, 42, 43, 44, 45, 46, 48, 49, 52, 53, 54, 56, 57, 58, 59, 61,
                    65, 66, 67, 70, 72, 73, 76, 77);
        }
    }
}
