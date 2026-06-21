package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.ConvertOptions;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class ConvertHandlerTest {

    private final ConvertHandler handler = new ConvertHandler();

    @Test
    void convertToImages_pngFormat_returnsOneImagePerPage() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "img");

        ConvertOptions opts = new ConvertOptions();
        opts.format = "png";
        opts.dpi = 72;

        byte[][] images = handler.convertToImages(pdf, opts);

        assertEquals(2, images.length);
        assertTrue(images[0].length > 0);
    }

    @Test
    void convertToImages_invalidFormat_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");
        ConvertOptions opts = new ConvertOptions();
        opts.format = "bmp";

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.convertToImages(pdf, opts));
        assertEquals("INVALID_FORMAT", ex.code);
    }
}
