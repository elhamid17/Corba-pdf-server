package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class CompareHandlerTest {

    private final CompareHandler handler = new CompareHandler();

    @Test
    void comparePdfs_identicalDocuments_reportsSamePages() throws Exception {
        byte[] a = PdfTestFixtures.singlePagePdf("Same");
        byte[] b = PdfTestFixtures.singlePagePdf("Same");

        String json = handler.comparePdfs(a, b);

        assertTrue(json.contains("\"pagesA\":1"));
        assertTrue(json.contains("\"pagesB\":1"));
    }
}
