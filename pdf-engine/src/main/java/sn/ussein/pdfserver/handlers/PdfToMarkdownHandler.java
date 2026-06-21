package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Convertit un PDF en texte Markdown.
 *
 * Heuristiques :
 *  - Lignes vides : separateur de paragraphes
 *  - Lignes courtes en majuscules : titre (#)
 *  - Lignes commencant par "- ", "* ", "•" : item de liste
 *  - Lignes commencant par "N." ou "N)" : liste numerotee
 *  - Le reste : paragraphe normal
 *
 * Note : la conversion est best-effort, le PDF n'expose pas la semantique
 * du document (titres vs paragraphes) — on devine sur la mise en forme.
 */
public class PdfToMarkdownHandler {

    private static final Logger log = LoggerFactory.getLogger(PdfToMarkdownHandler.class);

    private static final Pattern BULLET = Pattern.compile("^\\s*[-*•◦‣⁃]\\s+(.+)$");
    private static final Pattern NUMBERED = Pattern.compile("^\\s*(\\d+)[.)]\\s+(.+)$");

    public PDFResult pdfToMarkdown(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                int total = doc.getNumberOfPages();
                StringBuilder md = new StringBuilder();

                for (int i = 1; i <= total; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String raw = stripper.getText(doc);

                    if (i > 1) md.append("\n---\n\n");  // separateur de page

                    boolean prevEmpty = true;
                    for (String line : raw.split("\\r?\\n")) {
                        String trimmed = line.trim();

                        if (trimmed.isEmpty()) {
                            if (!prevEmpty) md.append("\n");
                            prevEmpty = true;
                            continue;
                        }

                        // Liste a puces
                        java.util.regex.Matcher mb = BULLET.matcher(trimmed);
                        if (mb.matches()) {
                            md.append("- ").append(mb.group(1)).append("\n");
                            prevEmpty = false;
                            continue;
                        }
                        // Liste numerotee
                        java.util.regex.Matcher mn = NUMBERED.matcher(trimmed);
                        if (mn.matches()) {
                            md.append(mn.group(1)).append(". ").append(mn.group(2)).append("\n");
                            prevEmpty = false;
                            continue;
                        }
                        // Titre : ligne courte tout en majuscules
                        if (trimmed.length() <= 80 && trimmed.length() >= 3
                            && trimmed.equals(trimmed.toUpperCase())
                            && !trimmed.matches(".*\\d.*\\d.*")) {
                            md.append("## ").append(trimmed).append("\n\n");
                            prevEmpty = true;
                            continue;
                        }

                        md.append(trimmed).append("\n");
                        prevEmpty = false;
                    }
                }

                byte[] result = md.toString().getBytes(StandardCharsets.UTF_8);
                log.info("PDF -> Markdown : {} pages, {} octets", total, result.length);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = result;
                res.message = total + " page(s) converties en Markdown";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur pdfToMarkdown", e);
            throw new PDFException("PDF_TO_MARKDOWN_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
