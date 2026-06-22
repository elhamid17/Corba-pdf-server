package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdf.WatermarkOptions;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class WatermarkHandlerTest {

    private final WatermarkHandler handler = new WatermarkHandler();

    @Test
    void addWatermark_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("wm");

        WatermarkOptions opts = new WatermarkOptions();
        opts.text = "CONFIDENTIEL";
        opts.opacity = 0.3f;
        opts.fontSize = 36;
        opts.diagonal = true;

        PDFResult result = handler.addWatermark(pdf, opts);

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void addWatermark_emptyText_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("wm");
        WatermarkOptions opts = new WatermarkOptions();
        opts.text = "   ";

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.addWatermark(pdf, opts));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
