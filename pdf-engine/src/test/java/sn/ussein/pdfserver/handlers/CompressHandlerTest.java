package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.CompressOptions;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class CompressHandlerTest {

    private final CompressHandler handler = new CompressHandler();

    @Test
    void compress_removeMetadata_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("compress");

        CompressOptions opts = new CompressOptions();
        opts.compressImages = false;
        opts.removeMetadata = true;
        opts.imageQuality = 80;

        PDFResult result = handler.compress(pdf, opts);

        assertTrue(result.success);
        assertNotNull(result.data);
        assertTrue(result.data.length > 0);
    }

    @Test
    void compress_emptyPdf_throwsPdfException() {
        CompressOptions opts = new CompressOptions();
        assertThrows(PDFException.class, () -> handler.compress(new byte[0], opts));
    }
}
