package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Extrait un sous-ensemble de pages d'un PDF
 * et les rassemble dans un nouveau document.
 */
public class ExtractHandler {

    private static final Logger log = LoggerFactory.getLogger(ExtractHandler.class);

    public PDFResult extractPages(byte[] pdf, int[] pages)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (pages == null || pages.length == 0) {
            throw new PDFException("INVALID_INPUT",
                "La liste des pages à extraire est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument source = PDDocument.load(tmpFile);
                 PDDocument target = new PDDocument()) {

                int totalPages = source.getNumberOfPages();
                log.info("Extraction — {} pages demandées sur {} au total",
                    pages.length, totalPages);

                for (int pageNum : pages) {
                    if (pageNum < 1 || pageNum > totalPages) {
                        InvalidPageException ex = new InvalidPageException();
                        ex.requestedPage = pageNum;
                        ex.totalPages    = totalPages;
                        ex.message = "Page " + pageNum + " hors limites (total : " + totalPages + ")";
                        throw ex;
                    }
                    // PDFBox utilise un index 0-based
                    PDPage page = source.getPage(pageNum - 1);
                    target.addPage(target.importPage(page));
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                target.save(out);

                byte[] result = out.toByteArray();
                log.info("Extraction réussie — {} pages → {} octets",
                    pages.length, result.length);

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = pages.length + " pages extraites avec succès";
                return res;
            }

        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction de pages", e);
            throw new PDFException("EXTRACT_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}