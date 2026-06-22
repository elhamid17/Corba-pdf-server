package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdf.PDFMetadata;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class AnonymizeHandlerTest {

    private final AnonymizeHandler handler = new AnonymizeHandler();

    @Test
    void anonymize_clearsMetadata() throws Exception {
        byte[] pdf = PdfTestFixtures.singlePagePdf("doc");
        MetadataHandler metaHandler = new MetadataHandler();
        PDFMetadata in = new PDFMetadata();
        in.author = "Alice";
        pdf = metaHandler.setMetadata(pdf, in).data;

        PDFResult result = handler.anonymize(pdf);

        assertTrue(result.success);
        PDFMetadata meta = metaHandler.getMetadata(result.data);
        assertTrue(meta.author.isEmpty());
    }
}
