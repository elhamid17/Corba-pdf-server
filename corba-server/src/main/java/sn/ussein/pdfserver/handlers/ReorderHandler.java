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
 * Reordonne les pages selon une liste d'index (1-based).
 * La liste peut etre plus courte ou plus longue que le doc d'origine :
 * - Pages absentes de la liste sont retirees
 * - Pages dupliquees dans la liste sont dupliquees dans le resultat
 */
public class ReorderHandler {

    private static final Logger log = LoggerFactory.getLogger(ReorderHandler.class);

    public PDFResult reorderPages(byte[] pdf, int[] order)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (order == null || order.length == 0) {
            throw new PDFException("INVALID_INPUT", "L'ordre fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument source = PDDocument.load(tmpFile);
                 PDDocument target = new PDDocument()) {

                int total = source.getNumberOfPages();

                for (int idx : order) {
                    if (idx < 1 || idx > total) {
                        InvalidPageException ex = new InvalidPageException();
                        ex.requestedPage = idx;
                        ex.totalPages = total;
                        ex.message = "Page " + idx + " hors limites (total : " + total + ")";
                        throw ex;
                    }
                    PDPage page = source.getPage(idx - 1);
                    // importPage clone ET ajoute deja la page (eviter le double-add)
                    target.importPage(page);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                target.save(out);

                log.info("Reorganisation : {} pages -> {} pages selon ordre custom",
                    total, order.length);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = order.length + " pages reorganisees";
                return res;
            }
        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur reorderPages", e);
            throw new PDFException("REORDER_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
