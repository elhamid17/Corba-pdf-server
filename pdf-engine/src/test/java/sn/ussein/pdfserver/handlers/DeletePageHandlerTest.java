package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class DeletePageHandlerTest {

    private final DeletePageHandler handler = new DeletePageHandler();

    @Test
    void deletePages_removesRequestedPage() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(3, "del");

        PDFResult result = handler.deletePages(pdf, new int[]{2});

        assertTrue(result.success);
        assertEquals(2, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void deletePages_allPages_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("solo");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.deletePages(pdf, new int[]{1}));
        assertTrue(ex.message.contains("Impossible de supprimer toutes les pages")
            || "INVALID_INPUT".equals(ex.code)
            || "DELETE_ERROR".equals(ex.code));
    }

    @Test
    void deletePages_invalidPage_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "del");

        assertThrows(InvalidPageException.class,
            () -> handler.deletePages(pdf, new int[]{9}));
    }
}
