package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class ResizeHandlerTest {

    private final ResizeHandler handler = new ResizeHandler();

    @Test
    void resize_toA4_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFResult result = handler.resizePages(pdf, "A4");

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void resize_unknownFormat_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.resizePages(pdf, "UNKNOWN"));
        assertEquals("INVALID_SIZE", ex.code);
    }
}
