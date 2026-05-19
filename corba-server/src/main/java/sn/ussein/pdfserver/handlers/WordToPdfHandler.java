package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Convertit un document Word (DOCX) en PDF.
 *
 * Lit les paragraphes du DOCX avec Apache POI et les écrit dans un PDF
 * généré par PDFBox. La mise en forme avancée (styles, images, tableaux)
 * n'est pas préservée — seul le texte est conservé.
 */
public class WordToPdfHandler {

    private static final Logger log = LoggerFactory.getLogger(WordToPdfHandler.class);

    private static final float FONT_SIZE = 12f;
    private static final float MARGIN    = 50f;
    private static final float LEADING   = 16f;

    public PDFResult wordToPdf(byte[] docx) throws PDFException {

        if (docx == null || docx.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le document Word fourni est vide");
        }

        try (XWPFDocument word = new XWPFDocument(new ByteArrayInputStream(docx));
             PDDocument pdf = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDType1Font font = PDType1Font.HELVETICA;
            float pageWidth  = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float maxWidth   = pageWidth - 2 * MARGIN;

            List<String> lines = new ArrayList<>();
            for (XWPFParagraph p : word.getParagraphs()) {
                String raw = p.getText();
                if (raw == null || raw.isEmpty()) {
                    lines.add("");
                    continue;
                }
                // Un paragraphe DOCX peut contenir des \n / \r / \t — on les
                // traite comme des sauts de ligne ou des espaces avant tout
                // calcul (PDFBox/Helvetica rejette ces caracteres de controle).
                for (String rawLine : raw.split("\\r?\\n")) {
                    String text = sanitize(rawLine.replace('\t', ' ')).trim();
                    if (text.isEmpty()) {
                        lines.add("");
                        continue;
                    }
                    String[] words = text.split(" +");
                    StringBuilder current = new StringBuilder();
                    for (String w : words) {
                        String test = current.length() == 0 ? w : current + " " + w;
                        float testWidth = font.getStringWidth(test) / 1000 * FONT_SIZE;
                        if (testWidth > maxWidth && current.length() > 0) {
                            lines.add(current.toString());
                            current = new StringBuilder(w);
                        } else {
                            current = new StringBuilder(test);
                        }
                    }
                    if (current.length() > 0) lines.add(current.toString());
                }
            }

            float y = pageHeight - MARGIN;
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(pdf, page);

            for (String line : lines) {
                if (y < MARGIN + LEADING) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    pdf.addPage(page);
                    cs = new PDPageContentStream(pdf, page);
                    y = pageHeight - MARGIN;
                }
                if (!line.isEmpty()) {
                    cs.beginText();
                    cs.setFont(font, FONT_SIZE);
                    cs.newLineAtOffset(MARGIN, y);
                    // Filtrer les caractères non-WinAnsi (PDFBox limitation)
                    cs.showText(sanitize(line));
                    cs.endText();
                }
                y -= LEADING;
            }
            cs.close();

            pdf.save(out);
            byte[] result = out.toByteArray();

            log.info("Word → PDF — {} pages | {} octets",
                pdf.getNumberOfPages(), result.length);

            PDFResult res = new PDFResult();
            res.success = true;
            res.data    = result;
            res.message = "Conversion réussie — " + pdf.getNumberOfPages() + " page(s)";
            return res;
        } catch (Exception e) {
            log.error("Erreur Word → PDF", e);
            throw new PDFException("WORD_TO_PDF_ERROR", e.getMessage());
        }
    }

    private static String sanitize(String s) {
        // Helvetica ne supporte que le jeu WinAnsi — on remplace les caracteres hors plage par ?
        // et on filtre les caracteres de controle (sauf espace) que PDFBox refuse.
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c < 0x20 || c == 0x7F) continue;     // controles ASCII
            sb.append(c < 0x100 ? c : '?');
        }
        return sb.toString();
    }
}
