package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class ImagesToPdfHandlerTest {

    private final ImagesToPdfHandler handler = new ImagesToPdfHandler();

    @Test
    void imagesToPdf_singlePng_producesOnePage() throws Exception {
        byte[] png = PdfTestFixtures.minimalPng();

        PDFResult result = handler.imagesToPdf(new byte[][]{png});

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void imagesToPdf_emptyArray_throwsPdfException() {
        PDFException ex = assertThrows(PDFException.class,
            () -> handler.imagesToPdf(new byte[][]{}));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
