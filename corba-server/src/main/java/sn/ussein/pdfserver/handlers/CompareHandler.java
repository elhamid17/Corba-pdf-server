package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Compare le texte de deux PDFs (ligne par ligne) et renvoie un JSON
 * avec les statistiques et les diffs.
 *
 * Algorithme : extraction texte par page, comparaison page-a-page.
 * Pour chaque page, on compte les lignes communes et les lignes
 * presentes dans A ou B uniquement.
 */
public class CompareHandler {

    private static final Logger log = LoggerFactory.getLogger(CompareHandler.class);
    private static final int MAX_DIFF_LINES_PER_PAGE = 20;

    public String comparePdfs(byte[] pdfA, byte[] pdfB) throws PDFException {
        if (pdfA == null || pdfA.length == 0 || pdfB == null || pdfB.length == 0) {
            throw new PDFException("INVALID_INPUT", "Les deux PDFs doivent etre fournis");
        }

        File fileA = null, fileB = null;
        try {
            fileA = FileUtil.bytesToTempFile(pdfA, ".pdf");
            fileB = FileUtil.bytesToTempFile(pdfB, ".pdf");

            try (PDDocument docA = PDDocument.load(fileA);
                 PDDocument docB = PDDocument.load(fileB)) {

                int pagesA = docA.getNumberOfPages();
                int pagesB = docB.getNumberOfPages();
                int maxPages = Math.max(pagesA, pagesB);

                PDFTextStripper stripper = new PDFTextStripper();
                StringBuilder json = new StringBuilder();
                json.append("{")
                    .append("\"pagesA\":").append(pagesA).append(",")
                    .append("\"pagesB\":").append(pagesB).append(",")
                    .append("\"pages\":[");

                int totalAdded = 0, totalRemoved = 0, totalCommon = 0;

                for (int p = 1; p <= maxPages; p++) {
                    List<String> linesA = extractLines(docA, stripper, p);
                    List<String> linesB = extractLines(docB, stripper, p);

                    List<String> added = new ArrayList<>(linesB);
                    added.removeAll(linesA);
                    List<String> removed = new ArrayList<>(linesA);
                    removed.removeAll(linesB);

                    int commonOnPage = (linesA.size() + linesB.size() - added.size() - removed.size()) / 2;
                    totalAdded += added.size();
                    totalRemoved += removed.size();
                    totalCommon += commonOnPage;

                    if (p > 1) json.append(',');
                    json.append("{")
                        .append("\"page\":").append(p).append(",")
                        .append("\"common\":").append(commonOnPage).append(",")
                        .append("\"added\":").append(added.size()).append(",")
                        .append("\"removed\":").append(removed.size()).append(",")
                        .append("\"sampleAdded\":").append(jsonArray(added, MAX_DIFF_LINES_PER_PAGE)).append(",")
                        .append("\"sampleRemoved\":").append(jsonArray(removed, MAX_DIFF_LINES_PER_PAGE))
                        .append("}");
                }
                json.append("],")
                    .append("\"totals\":{")
                    .append("\"common\":").append(totalCommon).append(",")
                    .append("\"added\":").append(totalAdded).append(",")
                    .append("\"removed\":").append(totalRemoved)
                    .append("},")
                    .append("\"identical\":").append(totalAdded == 0 && totalRemoved == 0 && pagesA == pagesB)
                    .append("}");

                log.info("Compare : {} commun, +{} -{} ", totalCommon, totalAdded, totalRemoved);
                return json.toString();
            }
        } catch (Exception e) {
            log.error("Erreur comparePdfs", e);
            throw new PDFException("COMPARE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(fileA);
            FileUtil.deleteSilently(fileB);
        }
    }

    private List<String> extractLines(PDDocument doc, PDFTextStripper s, int page) throws Exception {
        if (page > doc.getNumberOfPages()) return new ArrayList<>();
        s.setStartPage(page);
        s.setEndPage(page);
        String text = s.getText(doc);
        List<String> out = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private String jsonArray(List<String> lines, int max) {
        StringBuilder b = new StringBuilder("[");
        int n = Math.min(lines.size(), max);
        for (int i = 0; i < n; i++) {
            if (i > 0) b.append(',');
            b.append('"').append(escape(lines.get(i))).append('"');
        }
        b.append(']');
        return b.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", "").replace("\t", " ");
    }
}
