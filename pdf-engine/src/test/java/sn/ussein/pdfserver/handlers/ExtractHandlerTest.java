package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class ExtractHandlerTest {

    private final ExtractHandler handler = new ExtractHandler();

    @Test
    void extractPages_subset_returnsOnePage() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(3, "doc");

        PDFResult result = handler.extractPages(pdf, new int[]{2});

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void extractPages_invalidPage_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");

        assertThrows(InvalidPageException.class,
            () -> handler.extractPages(pdf, new int[]{5}));
    }

    @Test
    void extractPages_emptyList_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.extractPages(pdf, new int[]{}));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
