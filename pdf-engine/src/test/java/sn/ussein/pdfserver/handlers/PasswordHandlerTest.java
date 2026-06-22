package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfengine.model.PasswordException;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHandlerTest {

    private final PasswordHandler handler = new PasswordHandler();

    @Test
    void addPassword_protectsPdf() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("secret");

        PDFResult result = handler.addPassword(pdf, "user123", "owner123");

        assertTrue(result.success);
        try (PDDocument doc = PDDocument.load(result.data, "user123")) {
            assertTrue(doc.isEncrypted());
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void addPassword_alreadyEncrypted_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");
        PDFResult protectedPdf = handler.addPassword(pdf, "pass", "pass");

        assertThrows(PDFException.class,
            () -> handler.addPassword(protectedPdf.data, "other", "other"));
    }

    @Test
    void addPassword_emptyUserPassword_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");
        PDFException ex = assertThrows(PDFException.class,
            () -> handler.addPassword(pdf, "", "owner"));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
