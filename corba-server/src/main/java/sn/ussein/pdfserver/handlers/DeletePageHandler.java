package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Supprime des pages spécifiques d'un PDF.
 * Les pages sont supprimées en ordre décroissant pour
 * éviter le décalage des index après chaque suppression.
 */
public class DeletePageHandler {

    private static final Logger log = LoggerFactory.getLogger(DeletePageHandler.class);

    public PDFResult deletePages(byte[] pdf, int[] pages)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (pages == null || pages.length == 0) {
            throw new PDFException("INVALID_INPUT",
                "La liste des pages à supprimer est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int totalPages = doc.getNumberOfPages();

                if (pages.length >= totalPages) {
                    throw new PDFException("INVALID_INPUT",
                        "Impossible de supprimer toutes les pages du PDF");
                }

                // Valider et dédupliquer les numéros de pages
                Set<Integer> pageSet = new HashSet<>();
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

                // Supprimer en ordre décroissant (évite le décalage d'index)
                Integer[] sortedDesc = pageSet.stream()
                    .sorted((a, b) -> b - a)
                    .toArray(Integer[]::new);

                for (int pageNum : sortedDesc) {
                    doc.removePage(pageNum - 1); // index 0-based
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                byte[] result = out.toByteArray();
                log.info("Suppression réussie — {} pages supprimées, {} restantes",
                    pageSet.size(), doc.getNumberOfPages());

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = pageSet.size() + " pages supprimées avec succès";
                return res;
            }

        } catch (PDFException | InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de pages", e);
            throw new PDFException("DELETE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}