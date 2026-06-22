package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class ReorderHandlerTest {

    private final ReorderHandler handler = new ReorderHandler();

    @Test
    void reorder_swapsPages() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(3, "p");

        PDFResult result = handler.reorderPages(pdf, new int[]{3, 1, 2});

        assertTrue(result.success);
        assertEquals(3, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void reorder_invalidPage_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");

        assertThrows(InvalidPageException.class,
            () -> handler.reorderPages(pdf, new int[]{5}));
    }

    @Test
    void reorder_emptyOrder_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.reorderPages(pdf, new int[]{}));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
