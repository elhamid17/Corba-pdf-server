package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.multipdf.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

/**
 * Découpe un PDF en plusieurs segments définis par des intervalles de pages.
 *
 * Exemple : ranges = [1, 3, 4, 6]
 *   → segment 1 : pages 1 à 3
 *   → segment 2 : pages 4 à 6
 */
public class SplitHandler {

    private static final Logger log = LoggerFactory.getLogger(SplitHandler.class);

    public byte[][] split(byte[] pdf, int[] ranges)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (ranges == null || ranges.length < 2 || ranges.length % 2 != 0) {
            throw new PDFException("INVALID_INPUT",
                "Les intervalles doivent être des paires de pages (ex: [1,3,4,6])");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int totalPages = doc.getNumberOfPages();
                log.info("Découpage — total pages : {}, intervalles : {}", totalPages, ranges.length / 2);

                // Valider tous les intervalles avant de commencer
                for (int i = 0; i < ranges.length; i += 2) {
                    int start = ranges[i];
                    int end   = ranges[i + 1];
                    if (start < 1 || end > totalPages || start > end) {
                        InvalidPageException ex = new InvalidPageException();
                        ex.requestedPage = start;
                        ex.totalPages    = totalPages;
                        ex.message = "Intervalle invalide : [" + start + ", " + end + "]";
                        throw ex;
                    }
                }

                // Découper chaque segment
                byte[][] results = new byte[ranges.length / 2][];

                for (int i = 0; i < ranges.length; i += 2) {
                    int start = ranges[i];
                    int end   = ranges[i + 1];

                    Splitter splitter = new Splitter();
                    splitter.setStartPage(start);
                    splitter.setEndPage(end);
                    splitter.setSplitAtPage(end - start + 1);

                    List<PDDocument> parts = splitter.split(doc);

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    parts.get(0).save(out);
                    parts.get(0).close();

                    results[i / 2] = out.toByteArray();
                    log.info("  Segment {} : pages {} à {} → {} octets",
                        (i / 2 + 1), start, end, results[i / 2].length);
                }

                return results;
            }

        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors du découpage PDF", e);
            throw new PDFException("SPLIT_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}