package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFMetadata;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class MetadataHandlerTest {

    private final MetadataHandler handler = new MetadataHandler();

    @Test
    void getMetadata_readsPageCount() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "doc");

        PDFMetadata meta = handler.getMetadata(pdf);

        assertEquals(2, meta.pageCount);
    }

    @Test
    void setMetadata_updatesTitle() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("x");
        PDFMetadata in = new PDFMetadata();
        in.title = "Nouveau titre";

        PDFResult result = handler.setMetadata(pdf, in);

        assertTrue(result.success);
        PDFMetadata updated = handler.getMetadata(result.data);
        assertEquals("Nouveau titre", updated.title);
    }

    @Test
    void getPageCount_returnsCorrectValue() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(4, "p");

        assertEquals(4, handler.getPageCount(pdf));
    }

    @Test
    void getMetadata_emptyPdf_throwsPdfException() {
        PDFException ex = assertThrows(PDFException.class,
            () -> handler.getMetadata(new byte[0]));
        assertEquals("INVALID_INPUT", ex.code);
    }
}
