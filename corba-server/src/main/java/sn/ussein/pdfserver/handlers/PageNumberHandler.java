package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Ajoute des numeros de page sur chaque page du PDF.
 *
 * Positions supportees :
 *   top-left, top-center, top-right
 *   bottom-left, bottom-center, bottom-right
 *
 * Format : utilise les jokers %d et %t (page courante, total)
 *   "%d", "%d/%t", "Page %d", "Page %d sur %t"
 */
public class PageNumberHandler {

    private static final Logger log = LoggerFactory.getLogger(PageNumberHandler.class);
    private static final float MARGIN = 30f;

    public PDFResult addPageNumbers(byte[] pdf, String position, String format,
                                    int startNumber, int fontSize) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (fontSize <= 0) fontSize = 12;
        if (startNumber < 1) startNumber = 1;
        if (format == null || format.isBlank()) format = "%d";
        if (position == null || position.isBlank()) position = "bottom-center";

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int total = doc.getNumberOfPages();
                PDType1Font font = PDType1Font.HELVETICA;

                for (int i = 0; i < total; i++) {
                    PDPage page = doc.getPage(i);
                    PDRectangle box = page.getMediaBox();
                    String text = format
                        .replace("%d", String.valueOf(startNumber + i))
                        .replace("%t", String.valueOf(total));

                    float textWidth = font.getStringWidth(text) / 1000f * fontSize;
                    float x = computeX(position, box.getWidth(), textWidth);
                    float y = computeY(position, box.getHeight());

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                            PDPageContentStream.AppendMode.APPEND, true, true)) {
                        cs.beginText();
                        cs.setFont(font, fontSize);
                        cs.newLineAtOffset(x, y);
                        cs.showText(text);
                        cs.endText();
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Numerotation : {} pages annotees ({}, format '{}')",
                    total, position, format);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = total + " pages numerotees";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur addPageNumbers", e);
            throw new PDFException("PAGENUMBER_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private float computeX(String position, float pageWidth, float textWidth) {
        if (position.endsWith("-left")) return MARGIN;
        if (position.endsWith("-right")) return pageWidth - MARGIN - textWidth;
        return (pageWidth - textWidth) / 2f;
    }

    private float computeY(String position, float pageHeight) {
        return position.startsWith("top-") ? pageHeight - MARGIN : MARGIN;
    }
}
