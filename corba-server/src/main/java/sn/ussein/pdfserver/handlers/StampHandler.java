package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Appose un tampon textuel (APPROUVE, CONFIDENTIEL, etc.) sur une page,
 * encadre, semi-transparent, en couleur, a la position demandee.
 */
public class StampHandler {

    private static final Logger log = LoggerFactory.getLogger(StampHandler.class);
    private static final float MARGIN = 40f;
    private static final float PADDING = 12f;

    private static final Map<String, Color> COLORS = new HashMap<>();
    static {
        COLORS.put("red",    new Color(220, 38, 38));
        COLORS.put("green",  new Color(34, 197, 94));
        COLORS.put("blue",   new Color(37, 99, 235));
        COLORS.put("black",  Color.BLACK);
        COLORS.put("orange", new Color(234, 88, 12));
    }

    public PDFResult addStamp(byte[] pdf, String text, int page, String position,
                              String colorName, int fontSize)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (text == null || text.isBlank()) {
            throw new PDFException("INVALID_INPUT", "Le texte du tampon est vide");
        }
        if (fontSize <= 0) fontSize = 36;
        if (position == null || position.isBlank()) position = "center";

        Color color = COLORS.getOrDefault(colorName == null ? "red" : colorName.toLowerCase(),
            COLORS.get("red"));

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int total = doc.getNumberOfPages();
                if (page < 1 || page > total) {
                    InvalidPageException ex = new InvalidPageException();
                    ex.requestedPage = page;
                    ex.totalPages = total;
                    ex.message = "Page " + page + " hors limites (total : " + total + ")";
                    throw ex;
                }

                PDPage target = doc.getPage(page - 1);
                PDRectangle box = target.getMediaBox();
                PDType1Font font = PDType1Font.HELVETICA_BOLD;

                float textWidth = font.getStringWidth(text) / 1000f * fontSize;
                float textHeight = fontSize;

                float boxWidth = textWidth + PADDING * 2;
                float boxHeight = textHeight + PADDING * 2;

                float[] xy = computePosition(position, box, boxWidth, boxHeight);
                float x = xy[0], y = xy[1];

                // Transparence pour effet "tampon"
                PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                gs.setNonStrokingAlphaConstant(0.6f);
                gs.setStrokingAlphaConstant(0.85f);

                try (PDPageContentStream cs = new PDPageContentStream(doc, target,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.setGraphicsStateParameters(gs);

                    // Rectangle d'encadrement
                    cs.setStrokingColor(color);
                    cs.setLineWidth(3f);
                    cs.addRect(x, y, boxWidth, boxHeight);
                    cs.stroke();

                    // Texte du tampon
                    cs.setNonStrokingColor(color);
                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(x + PADDING, y + PADDING + 2);
                    cs.showText(text);
                    cs.endText();
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Tampon '{}' appose page {} en {} ({})", text, page, position, colorName);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = "Tampon '" + text + "' apposé sur la page " + page;
                return res;
            }
        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur addStamp", e);
            throw new PDFException("STAMP_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private float[] computePosition(String position, PDRectangle box, float boxWidth, float boxHeight) {
        float w = box.getWidth(), h = box.getHeight();
        float x, y;

        // Horizontal
        if (position.endsWith("-left")) x = MARGIN;
        else if (position.endsWith("-right")) x = w - boxWidth - MARGIN;
        else x = (w - boxWidth) / 2f;

        // Vertical — PDF origin bottom-left
        if (position.startsWith("top-")) y = h - boxHeight - MARGIN;
        else if (position.startsWith("bottom-")) y = MARGIN;
        else y = (h - boxHeight) / 2f;  // center

        return new float[]{x, y};
    }
}
