package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class CoverHandlerTest {

    private final CoverHandler handler = new CoverHandler();

    @Test
    void addCoverPage_increasesPageCount() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");
        byte[] cover = PdfTestFixtures.minimalPng();

        PDFResult result = handler.addCoverPage(pdf, cover);

        assertTrue(result.success);
        assertEquals(2, PdfTestFixtures.pageCount(result.data));
    }
}
