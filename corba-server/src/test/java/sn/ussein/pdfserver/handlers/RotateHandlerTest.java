package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class RotateHandlerTest {

    private final RotateHandler handler = new RotateHandler();

    @Test
    void rotate_allPages_by90_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "doc");

        PDFResult result = handler.rotate(pdf, new int[0], 90);

        assertTrue(result.success);
        assertNotNull(result.data);
        assertEquals(2, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void rotate_invalidAngle_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.rotate(pdf, new int[0], 45));

        assertEquals("INVALID_ANGLE", ex.code);
    }

    @Test
    void rotate_outOfRangePage_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");

        InvalidPageException ex = assertThrows(InvalidPageException.class,
            () -> handler.rotate(pdf, new int[]{99}, 90));

        assertEquals(99, ex.requestedPage);
        assertEquals(1, ex.totalPages);
    }
}
