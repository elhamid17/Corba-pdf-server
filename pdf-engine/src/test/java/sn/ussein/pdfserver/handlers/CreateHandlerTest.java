package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class CreateHandlerTest {

    private final CreateHandler handler = new CreateHandler();

    @Test
    void createFromText_producesValidPdf() throws Exception {
        PDFResult result = handler.createFromText("Bonjour CORBA PDF\nLigne deux.", "Titre test");

        assertTrue(result.success);
        assertEquals(1, PdfTestFixtures.pageCount(result.data));
    }

    @Test
    void createFromText_blankText_throwsPdfException() {
        PDFException ex = assertThrows(PDFException.class,
            () -> handler.createFromText("   ", "Titre"));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
