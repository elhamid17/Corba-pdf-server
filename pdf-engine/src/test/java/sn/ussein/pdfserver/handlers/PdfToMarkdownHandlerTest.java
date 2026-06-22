package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class PdfToMarkdownHandlerTest {

    private final PdfToMarkdownHandler handler = new PdfToMarkdownHandler();

    @Test
    void pdfToMarkdown_extractsTextContent() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("Hello markdown");

        PDFResult result = handler.pdfToMarkdown(pdf);

        assertTrue(result.success);
        assertTrue(new String(result.data).contains("Hello"));
    }
}
