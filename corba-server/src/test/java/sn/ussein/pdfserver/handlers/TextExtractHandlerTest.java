package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class TextExtractHandlerTest {

    private final TextExtractHandler handler = new TextExtractHandler();

    @Test
    void extractText_returnsEmbeddedLabel() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("Hello PDF");

        String text = handler.extractText(pdf);

        assertNotNull(text);
        assertTrue(text.contains("Hello PDF"));
    }

    @Test
    void extractText_emptyPdf_throwsPdfException() {
        assertThrows(PDFException.class, () -> handler.extractText(new byte[0]));
    }
}
