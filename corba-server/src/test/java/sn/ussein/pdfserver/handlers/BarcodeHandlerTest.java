package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class BarcodeHandlerTest {

    private final BarcodeHandler handler = new BarcodeHandler();

    @Test
    void addQrCode_onFirstPage_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFResult result = handler.addQrCode(pdf, "https://ussein.sn", 1, "bottom-right", 80);

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void addBarcode_code128_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFResult result = handler.addBarcode(pdf, "1234567890", 1, "bottom-right", "CODE_128", 120);

        assertTrue(result.success);
    }

    @Test
    void addQrCode_invalidPage_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        assertThrows(InvalidPageException.class,
            () -> handler.addQrCode(pdf, "x", 5, "center", 80));
    }
}
