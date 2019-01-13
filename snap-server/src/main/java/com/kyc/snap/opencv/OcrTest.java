package com.kyc.snap.opencv;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;

public class OcrTest {

    public static void main(String[] args) throws Exception {
        try (TessBaseAPI api = new TessBaseAPI()) {
            if (api.Init("./data", "eng") != 0) {
                System.err.println("Could not initialize tesseract.");
                System.exit(1);
            }

            for (int i = 0; i < 100; i++) {
                PIX image = lept.pixRead("./src/test/resources/digit.png");
                api.SetImage(image);
                BytePointer outText = api.GetUTF8Text();
                System.out.println(outText.getString());
                outText.deallocate();
                lept.pixDestroy(image);
            }

            api.End();
        }
    }
}
