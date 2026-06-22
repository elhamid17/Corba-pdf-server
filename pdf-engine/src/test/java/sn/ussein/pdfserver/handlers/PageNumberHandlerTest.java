package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class PageNumberHandlerTest {

    private final PageNumberHandler handler = new PageNumberHandler();

    @Test
    void addPageNumbers_keepsPageCount() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "doc");

        PDFResult result = handler.addPageNumbers(pdf, "bottom-center", "%d", 1, 12);

        assertTrue(result.success);
        assertEquals(2, PdfTestFixtures.pageCount(result.data));
    }
}
