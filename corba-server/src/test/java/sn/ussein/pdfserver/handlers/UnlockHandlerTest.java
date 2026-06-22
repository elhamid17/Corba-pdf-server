package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdf.PasswordException;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class UnlockHandlerTest {

    private final PasswordHandler protect = new PasswordHandler();
    private final UnlockHandler handler = new UnlockHandler();

    @Test
    void unlock_removesEncryption() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("secret");
        PDFResult protectedPdf = protect.addPassword(pdf, "open123", "open123");

        PDFResult unlocked = handler.unlockPdf(protectedPdf.data, "open123");

        assertTrue(unlocked.success);
        try (PDDocument doc = PDDocument.load(unlocked.data)) {
            assertFalse(doc.isEncrypted());
        }
    }

    @Test
    void unlock_wrongPassword_throwsPasswordException() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("secret");
        PDFResult protectedPdf = protect.addPassword(pdf, "good", "good");

        assertThrows(PasswordException.class,
            () -> handler.unlockPdf(protectedPdf.data, "bad"));
    }
}
