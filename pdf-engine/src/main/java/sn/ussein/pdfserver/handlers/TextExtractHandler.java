package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.File;

/**
 * Extrait le texte brut de toutes les pages d'un PDF.
 * Utilise PDFTextStripper de PDFBox — fonctionne sur les PDFs
 * avec couche texte native. Pour les PDFs scannés, utiliser OcrHandler.
 */
public class TextExtractHandler {

    private static final Logger log = LoggerFactory.getLogger(TextExtractHandler.class);

    public String extractText(byte[] pdf) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(doc);

                log.info("Extraction de texte réussie — {} caractères extraits",
                    text.length());

                return text;
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction de texte", e);
            throw new PDFException("TEXT_EXTRACT_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}