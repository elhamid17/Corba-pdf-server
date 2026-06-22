package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class StampHandlerTest {

    private final StampHandler handler = new StampHandler();

    @Test
    void addStamp_onFirstPage_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFResult result = handler.addStamp(pdf, "APPROUVE", 1, "center", "red", 24);

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void addStamp_invalidPage_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        assertThrows(InvalidPageException.class,
            () -> handler.addStamp(pdf, "OK", 99, "center", "red", 24));
    }
}
