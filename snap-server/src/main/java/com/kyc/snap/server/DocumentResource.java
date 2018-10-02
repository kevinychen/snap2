package com.kyc.snap.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.kyc.snap.document.Document;
import com.kyc.snap.document.Document.DocumentPage;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Pdf;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.store.Store;

import lombok.Data;

@Data
public class DocumentResource implements DocumentService {

    private final Store store;

    @Override
    public Document createDocumentFromPdf(InputStream pdfStream) throws IOException {
        byte[] blob = IOUtils.toByteArray(pdfStream);
        Pdf pdf = new Pdf(blob);
        List<DocumentPage> pages = new ArrayList<>();
        for (int page = 0; page < pdf.getNumPages(); page++) {
            String imageId = store.storeBlob(ImageUtils.toBytes(pdf.toImage(page)));
            List<DocumentText> texts = pdf.getTexts(page);
            pages.add(new DocumentPage(imageId, texts));
        }
        String id = UUID.randomUUID().toString();
        Document doc = new Document(id, pages);
        store.updateObject(id, doc);
        return doc;
    }
}
