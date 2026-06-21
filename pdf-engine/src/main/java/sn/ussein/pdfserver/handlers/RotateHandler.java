package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pivote des pages spécifiques d'un PDF.
 * Angles acceptés : 90, 180, 270 degrés.
 * Si pages est vide, toutes les pages sont pivotées.
 */
public class RotateHandler {

    private static final Logger log = LoggerFactory.getLogger(RotateHandler.class);
    private static final Set<Integer> VALID_ANGLES = new HashSet<>(
        Arrays.asList(90, 180, 270)
    );

    public PDFResult rotate(byte[] pdf, int[] pages, int angle)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (!VALID_ANGLES.contains(angle)) {
            throw new PDFException("INVALID_ANGLE",
                "Angle invalide : " + angle + ". Valeurs acceptées : 90, 180, 270");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int totalPages = doc.getNumberOfPages();

                // Si aucune page spécifiée → pivoter tout le document
                Set<Integer> pageSet = new HashSet<>();
                if (pages == null || pages.length == 0) {
                    for (int i = 1; i <= totalPages; i++) pageSet.add(i);
                } else {
                    for (int p : pages) {
                        if (p < 1 || p > totalPages) {
                            InvalidPageException ex = new InvalidPageException();
                            ex.requestedPage = p;
                            ex.totalPages    = totalPages;
                            ex.message = "Page " + p + " hors limites";
                            throw ex;
                        }
                        pageSet.add(p);
                    }
                }

                // Appliquer la rotation
                for (int pageNum : pageSet) {
                    PDPage page = doc.getPage(pageNum - 1);
                    int current = page.getRotation();
                    page.setRotation((current + angle) % 360);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                byte[] result = out.toByteArray();
                log.info("Rotation {}° appliquée sur {} pages", angle, pageSet.size());

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "Rotation de " + angle + "° appliquée sur "
                    + pageSet.size() + " page(s)";
                return res;
            }

        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la rotation PDF", e);
            throw new PDFException("ROTATE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}