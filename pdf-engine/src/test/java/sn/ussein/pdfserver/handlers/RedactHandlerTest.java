package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class RedactHandlerTest {

    private final RedactHandler handler = new RedactHandler();

    @Test
    void redactText_matchingTerm_succeeds() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("SECRET document");

        PDFResult result = handler.redactText(pdf, new String[]{"SECRET"});

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void redactText_noTerms_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.redactText(pdf, new String[]{}));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
