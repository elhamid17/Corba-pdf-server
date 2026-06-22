package sn.ussein.pdfserver.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Fabrique de PDF minimaux pour les tests unitaires (PDFBox). */
public final class PdfTestFixtures {

    private PdfTestFixtures() {}

    public static byte[] singlePagePdf(String label) throws IOException {
        return multiPagePdf(1, label);
    }

    public static byte[] multiPagePdf(int pageCount, String labelPrefix) throws IOException {
        if (pageCount < 1) {
            throw new IllegalArgumentException("pageCount >= 1");
        }
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText(labelPrefix + " page " + (i + 1));
                    cs.endText();
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    public static int pageCount(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            return doc.getNumberOfPages();
        }
    }

    /** PNG 2x2 minimal pour ImagesToPdfHandler et similaires. */
    public static byte[] minimalPng() throws IOException {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }
}
