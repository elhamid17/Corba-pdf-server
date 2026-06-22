package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class HtmlToPdfHandlerTest {

    private final HtmlToPdfHandler handler = new HtmlToPdfHandler();

    @Test
    void htmlToPdf_rendersParagraph() throws Exception {
        PDFResult result = handler.htmlToPdf(
            "<html><body><h1>Titre</h1><p>Contenu HTML.</p></body></html>",
            "Doc HTML");

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void htmlToPdf_emptyInput_throwsPdfException() {
        PDFException ex = assertThrows(PDFException.class,
            () -> handler.htmlToPdf("", "Doc"));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
