package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class StatsHandlerTest {

    private final StatsHandler handler = new StatsHandler();

    @Test
    void getDocumentStats_returnsJsonWithPages() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "Hello world document");

        String json = handler.getDocumentStats(pdf);

        assertTrue(json.contains("\"pages\":2"));
        assertTrue(json.contains("\"words\""));
    }
}
