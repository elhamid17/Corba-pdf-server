package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class SignatureImageHandlerTest {

    private final SignatureImageHandler handler = new SignatureImageHandler();

    @Test
    void addSignatureImage_onFirstPage_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");
        byte[] sig = PdfTestFixtures.minimalPng();

        PDFResult result = handler.addSignatureImage(pdf, sig, 1, 50f, 10f, 30f);

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }
}
