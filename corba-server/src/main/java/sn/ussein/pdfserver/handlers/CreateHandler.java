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

import java.io.ByteArrayOutputStream;

/**
 * Crée un nouveau PDF à partir d'un texte brut.
 * Gère automatiquement le découpage en lignes et la pagination.
 */
public class CreateHandler {

    private static final Logger log = LoggerFactory.getLogger(CreateHandler.class);

    private static final float FONT_SIZE    = 12f;
    private static final float MARGIN       = 50f;
    private static final float LEADING      = 16f;
    private static final float TITLE_SIZE   = 18f;

    public PDFResult createFromText(String text, String title)
            throws PDFException {

        if (text == null || text.trim().isEmpty()) {
            throw new PDFException("INVALID_INPUT", "Le texte fourni est vide");
        }

        try (PDDocument doc = new PDDocument()) {

            PDType1Font fontNormal = PDType1Font.HELVETICA;
            PDType1Font fontBold   = PDType1Font.HELVETICA_BOLD;

            float pageWidth  = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float maxWidth   = pageWidth - 2 * MARGIN;

            // Découper le texte en lignes respectant la largeur de page
            String[] paragraphs = text.split("\n");
            java.util.List<String> lines = new java.util.ArrayList<>();

            for (String paragraph : paragraphs) {
                if (paragraph.trim().isEmpty()) {
                    lines.add("");
                    continue;
                }
                // Découpage automatique si ligne trop longue
                String[] words = paragraph.split(" ");
                StringBuilder currentLine = new StringBuilder();
                for (String word : words) {
                    String test = currentLine.length() == 0
                        ? word : currentLine + " " + word;
                    float testWidth = fontNormal.getStringWidth(test)
                        / 1000 * FONT_SIZE;
                    if (testWidth > maxWidth && currentLine.length() > 0) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    } else {
                        currentLine = new StringBuilder(test);
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
            }

            // ── Écrire les pages ──
            float y = pageHeight - MARGIN;
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            boolean firstPage = true;

            for (String line : lines) {
                // Nouvelle page si nécessaire
                if (y < MARGIN + LEADING) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - MARGIN;
                    firstPage = false;
                }

                // Titre sur la première page
                if (firstPage && title != null && !title.isEmpty()) {
                    cs.beginText();
                    cs.setFont(fontBold, TITLE_SIZE);
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(title);
                    cs.endText();
                    y -= TITLE_SIZE + LEADING;
                    firstPage = false;
                    continue;
                }

                if (!line.isEmpty()) {
                    cs.beginText();
                    cs.setFont(fontNormal, FONT_SIZE);
                    cs.newLineAtOffset(MARGIN, y);
                    cs.showText(line);
                    cs.endText();
                }
                y -= LEADING;
            }

            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);

            byte[] result = out.toByteArray();
            log.info("PDF créé — '{}' | {} pages | {} octets",
                title, doc.getNumberOfPages(), result.length);

            PDFResult res = new PDFResult();
            res.success = true;
            res.data    = result;
            res.message = "PDF créé avec succès — "
                + doc.getNumberOfPages() + " page(s)";
            return res;

        } catch (PDFException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la création du PDF", e);
            throw new PDFException("CREATE_ERROR", e.getMessage());
        }
    }
}