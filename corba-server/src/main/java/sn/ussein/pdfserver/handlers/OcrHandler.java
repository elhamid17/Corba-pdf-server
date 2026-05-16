package sn.ussein.pdfserver.handlers;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Reconnaissance optique de caractères (OCR) sur un PDF scanné.
 * Utilise Tesseract via Tess4J.
 *
 * Langues supportées : fra (français), eng (anglais), ara (arabe), etc.
 * Les modèles de langue doivent être présents dans /volumes/tessdata/
 */
public class OcrHandler {

    private static final Logger log = LoggerFactory.getLogger(OcrHandler.class);

    // Chemin vers les données Tesseract (monté comme volume Docker)
    private static final String TESSDATA_PATH =
        System.getProperty("tessdata.path", "/volumes/tessdata");

    public String performOCR(byte[] pdf, String language) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        String lang = (language != null && !language.isEmpty()) ? language : "fra";
        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                PDFRenderer renderer = new PDFRenderer(doc);
                int totalPages = doc.getNumberOfPages();
                StringBuilder fullText = new StringBuilder();

                // Configurer Tesseract
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath(TESSDATA_PATH);
                tesseract.setLanguage(lang);
                tesseract.setPageSegMode(1);  // auto segmentation
                tesseract.setOcrEngineMode(1); // LSTM neural engine

                log.info("OCR — {} pages | langue : {}", totalPages, lang);

                for (int i = 0; i < totalPages; i++) {
                    // Rendre chaque page en image haute résolution pour l'OCR
                    BufferedImage image = renderer.renderImageWithDPI(
                        i, 300, ImageType.RGB
                    );

                    String pageText = tesseract.doOCR(image);
                    fullText.append("=== Page ").append(i + 1)
                            .append(" ===\n")
                            .append(pageText)
                            .append("\n\n");

                    log.info("  OCR page {} — {} caractères", i + 1, pageText.length());
                }

                return fullText.toString();
            }

        } catch (TesseractException e) {
            log.error("Erreur Tesseract OCR", e);
            throw new PDFException("OCR_ERROR",
                "Erreur OCR : " + e.getMessage()
                + " — Vérifiez que les modèles de langue sont dans " + TESSDATA_PATH);
        } catch (Exception e) {
            log.error("Erreur lors de l'OCR", e);
            throw new PDFException("OCR_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
