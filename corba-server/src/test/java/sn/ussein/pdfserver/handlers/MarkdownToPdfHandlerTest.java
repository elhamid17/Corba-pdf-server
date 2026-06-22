package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownToPdfHandlerTest {

    private final MarkdownToPdfHandler handler = new MarkdownToPdfHandler();

    @Test
    void markdownToPdf_rendersHeading() throws Exception {
        PDFResult result = handler.markdownToPdf("# Titre\n\nParagraphe.", "Doc MD");

        assertTrue(result.success);
        assertTrue(result.data.length > 100);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void markdownToPdf_emptyInput_throwsPdfException() {
        PDFException ex = assertThrows(PDFException.class,
            () -> handler.markdownToPdf("  ", "Doc"));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
