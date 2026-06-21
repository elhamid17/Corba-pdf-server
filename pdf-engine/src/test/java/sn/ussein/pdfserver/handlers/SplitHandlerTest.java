package sn.ussein.pdfserver.handlers;

import org.junit.jupiter.api.Test;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfserver.support.PdfTestFixtures;

import static org.junit.jupiter.api.Assertions.*;

class SplitHandlerTest {

    private final SplitHandler handler = new SplitHandler();

    @Test
    void split_validRanges_returnsTwoSegments() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(4, "split");

        byte[][] parts = handler.split(pdf, new int[]{1, 2, 3, 4});

        assertEquals(2, parts.length);
        assertEquals(2, PdfTestFixtures.pageCount(parts[0]));
        assertEquals(2, PdfTestFixtures.pageCount(parts[1]));
    }

    @Test
    void split_oddRangesLength_throwsPdfException() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(3, "split");

        PDFException ex = assertThrows(PDFException.class,
            () -> handler.split(pdf, new int[]{1, 2, 3}));

        assertEquals("INVALID_INPUT", ex.code);
    }

    @Test
    void split_invalidInterval_throwsInvalidPageException() throws Exception {
        byte[] pdf = PdfTestFixtures.multiPagePdf(2, "split");

        assertThrows(InvalidPageException.class,
            () -> handler.split(pdf, new int[]{1, 5}));
    }
}
