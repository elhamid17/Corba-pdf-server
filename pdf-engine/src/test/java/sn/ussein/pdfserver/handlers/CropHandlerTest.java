package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class CropHandlerTest {

    private final CropHandler handler = new CropHandler();

    @Test
    void crop_smallMargins_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFResult result = handler.cropPages(pdf, 5f, 5f, 5f, 5f);

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void crop_excessiveMargins_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.cropPages(pdf, 60f, 0f, 0f, 0f));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
