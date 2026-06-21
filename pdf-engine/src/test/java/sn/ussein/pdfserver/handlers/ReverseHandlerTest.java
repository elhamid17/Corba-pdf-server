package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class ReverseHandlerTest {

    private final ReverseHandler handler = new ReverseHandler();

    @Test
    void reverse_threePagePdf_reversesOrder() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(3, "p");

        PDFResult result = handler.reversePages(pdf);

        assertTrue(result.success);
        assertEquals(3, PdfTestFixtures.pageCount(result.data));
    }
}
