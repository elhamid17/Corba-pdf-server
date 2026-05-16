package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Fusionne plusieurs PDFs en un seul document dans l'ordre fourni.
 */
public class MergeHandler {

    private static final Logger log = LoggerFactory.getLogger(MergeHandler.class);

    public PDFResult merge(byte[][] pdfs) throws PDFException {
        if (pdfs == null || pdfs.length < 2) {
            throw new PDFException("INVALID_INPUT",
                "La fusion nécessite au moins 2 fichiers PDF");
        }

        File[] tmpFiles = new File[pdfs.length];

        try {
            // Écrire chaque PDF dans un fichier temporaire
            for (int i = 0; i < pdfs.length; i++) {
                tmpFiles[i] = FileUtil.bytesToTempFile(pdfs[i], ".pdf");
            }

            // Fusionner avec PDFMergerUtility
            PDFMergerUtility merger = new PDFMergerUtility();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            merger.setDestinationStream(out);

            for (File f : tmpFiles) {
                merger.addSource(f);
            }

            merger.mergeDocuments(null);

            byte[] result = out.toByteArray();
            log.info("Fusion réussie — {} PDFs → {} octets", pdfs.length, result.length);

            PDFResult res = new PDFResult();
            res.success = true;
            res.data    = result;
            res.message = "Fusion de " + pdfs.length + " fichiers réussie";
            return res;
        } catch (Exception e) {
            log.error("Erreur lors de la fusion PDF", e);
            throw new PDFException("MERGE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFiles);
        }
    }
}