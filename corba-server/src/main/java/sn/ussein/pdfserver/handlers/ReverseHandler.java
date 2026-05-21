package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inverse l'ordre des pages d'un PDF :
 * la derniere devient la premiere, etc.
 */
public class ReverseHandler {

    private static final Logger log = LoggerFactory.getLogger(ReverseHandler.class);

    public PDFResult reversePages(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument source = PDDocument.load(tmpFile);
                 PDDocument target = new PDDocument()) {

                int total = source.getNumberOfPages();
                List<PDPage> pages = new ArrayList<>(total);
                for (int i = 0; i < total; i++) pages.add(source.getPage(i));
                Collections.reverse(pages);
                // importPage clone ET ajoute la page au target — pas de addPage en plus
                for (PDPage p : pages) target.importPage(p);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                target.save(out);

                log.info("Inversion : {} pages reorganisees", total);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = total + " pages inversees";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur reversePages", e);
            throw new PDFException("REVERSE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
