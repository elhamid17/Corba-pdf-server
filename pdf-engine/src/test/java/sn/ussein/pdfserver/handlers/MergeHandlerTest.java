package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class MergeHandlerTest {

    private final MergeHandler handler = new MergeHandler();

    @Test
    void merge_twoPdfs_producesSingleDocumentWithCombinedPages() throws Exception {
        byte[] a = PdfTestFixtures.singlePagePdf("A");
        byte[] b = PdfTestFixtures.singlePagePdf("B");

        PDFResult result = handler.merge(new byte[][]{a, b});

        assertTrue(result.success);
        assertNotNull(result.data);
        assertTrue(result.data.length > 0);
        assertEquals(2, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void merge_singlePdf_throwsPdfException() {
        byte[] only = assertDoesNotThrow(() -> PdfTestFixtures.singlePagePdf("solo"));

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.merge(new byte[][]{only}));

        assertEquals("INVALID_INPUT", ex.code);
    }

    @Test
    void merge_threePdfs_producesThreePages() throws Exception {
        byte[] a = PdfTestFixtures.singlePagePdf("1");
        byte[] b = PdfTestFixtures.singlePagePdf("2");
        byte[] c = PdfTestFixtures.singlePagePdf("3");

        PDFResult result = handler.merge(new byte[][]{a, b, c});

        assertEquals(3, PdfTestFixtures.pageCount(result.data));
    }
}
