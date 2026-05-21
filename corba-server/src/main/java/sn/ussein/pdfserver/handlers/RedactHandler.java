package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Caviardage textuel : cherche chaque terme dans le PDF et recouvre
 * les occurrences d'un rectangle noir.
 *
 * Limitation : le texte sous-jacent reste dans le PDF (un copier-coller
 * le revele toujours). Pour une vraie redaction destructive il faut
 * utiliser PDF Sanitization (complexe). Cette implementation est
 * suffisante pour de la confidentialite visuelle (impression, partage
 * a des lecteurs humains).
 */
public class RedactHandler {

    private static final Logger log = LoggerFactory.getLogger(RedactHandler.class);

    public PDFResult redactText(byte[] pdf, String[] terms) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (terms == null || terms.length == 0) {
            throw new PDFException("INVALID_INPUT", "Aucun terme a caviarder");
        }

        // Nettoyage : retire les vides et les doublons
        List<String> clean = new ArrayList<>();
        for (String t : terms) {
            if (t != null) {
                String s = t.trim();
                if (!s.isEmpty() && !clean.contains(s)) clean.add(s);
            }
        }
        if (clean.isEmpty()) {
            throw new PDFException("INVALID_INPUT", "Tous les termes fournis sont vides");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int total = doc.getNumberOfPages();
                Map<Integer, List<float[]>> rectsByPage = collectRedactionRects(doc, total, clean);

                int totalRedacted = 0;
                for (Map.Entry<Integer, List<float[]>> entry : rectsByPage.entrySet()) {
                    int pageIdx = entry.getKey();
                    PDPage page = doc.getPage(pageIdx);
                    PDRectangle box = page.getMediaBox();

                    try (PDPageContentStream cs = new PDPageContentStream(doc, page,
                            PDPageContentStream.AppendMode.APPEND, true, true)) {
                        cs.setNonStrokingColor(Color.BLACK);
                        for (float[] r : entry.getValue()) {
                            // r = [x, y_top, width, height] en coordonnees PDFBox
                            // PDFBox y est mesure depuis le haut (PDFTextStripper),
                            // on convertit en coordonnees PDF (origine bas-gauche).
                            float pdfY = box.getHeight() - r[1] - r[3];
                            // Petit padding pour bien couvrir les jambages
                            cs.addRect(r[0] - 1, pdfY - 1, r[2] + 2, r[3] + 2);
                            cs.fill();
                            totalRedacted++;
                        }
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Caviardage : {} occurrence(s) recouverte(s) pour {} terme(s)",
                    totalRedacted, clean.size());
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = totalRedacted + " occurrence(s) caviardée(s)";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur redactText", e);
            throw new PDFException("REDACT_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    /**
     * Parcourt le doc avec un PDFTextStripper instrumente, et accumule
     * les rectangles a noircir, indexes par numero de page (0-based).
     */
    private Map<Integer, List<float[]>> collectRedactionRects(
            PDDocument doc, int totalPages, List<String> terms) throws Exception {

        Map<Integer, List<float[]>> result = new HashMap<>();

        for (int p = 1; p <= totalPages; p++) {
            final int pageIdx = p - 1;
            StringBuilder pageText = new StringBuilder();
            List<TextPosition> positions = new ArrayList<>();

            PDFTextStripper stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String text, List<TextPosition> textPositions) {
                    pageText.append(text);
                    positions.addAll(textPositions);
                }
            };
            stripper.setSortByPosition(true);
            stripper.setStartPage(p);
            stripper.setEndPage(p);
            stripper.getText(doc);

            String pageStr = pageText.toString();
            List<float[]> rects = new ArrayList<>();

            for (String term : terms) {
                if (term.isEmpty()) continue;
                int from = 0;
                while (true) {
                    int idx = pageStr.toLowerCase().indexOf(term.toLowerCase(), from);
                    if (idx < 0 || idx + term.length() > positions.size()) break;
                    // Limites de chaque caractere du terme
                    float minX = Float.MAX_VALUE, maxX = -1;
                    float minY = Float.MAX_VALUE, maxH = 0;
                    for (int i = idx; i < idx + term.length(); i++) {
                        TextPosition tp = positions.get(i);
                        if (tp == null) continue;
                        minX = Math.min(minX, tp.getX());
                        maxX = Math.max(maxX, tp.getX() + tp.getWidth());
                        minY = Math.min(minY, tp.getY() - tp.getHeight());
                        maxH = Math.max(maxH, tp.getHeight());
                    }
                    if (maxX > 0) {
                        rects.add(new float[]{ minX, minY, maxX - minX, maxH });
                    }
                    from = idx + term.length();
                }
            }

            if (!rects.isEmpty()) result.put(pageIdx, rects);
        }

        return result;
    }
}
