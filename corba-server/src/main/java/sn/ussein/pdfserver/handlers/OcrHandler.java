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

    // Chemin vers les donnees Tesseract. Sur Alpine (Docker), le package
    // tesseract-ocr-data-* installe dans /usr/share/tessdata/.
    // Fallback /volumes/tessdata pour les setups qui montent un volume custom.
    private static final String TESSDATA_PATH =
        System.getProperty("tessdata.path",
            System.getenv().getOrDefault("TESSDATA_PATH", "/usr/share/tessdata"));

    // Limite le nombre de pages pour eviter l'OOM sur le free tier Render
    // (chaque page rendue a 200 DPI fait ~11 Mo en RGB).
    private static final int MAX_PAGES = 30;

    // 200 DPI : compromis qualite OCR / consommation memoire. A 300 DPI
    // chaque page A4 fait ~26 Mo en RAM, ce qui sature les 512 Mo du free tier.
    // A 200 DPI : ~11 Mo / page, qualite Tesseract toujours tres bonne.
    private static final int RENDER_DPI = 200;

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

                if (totalPages > MAX_PAGES) {
                    throw new PDFException("OCR_TOO_LARGE",
                        "PDF trop volumineux : " + totalPages + " pages (max " + MAX_PAGES + "). "
                        + "Decoupez le document en plusieurs parties avant OCR.");
                }

                StringBuilder fullText = new StringBuilder();

                // Configurer Tesseract
                Tesseract tesseract = new Tesseract();
                tesseract.setDatapath(TESSDATA_PATH);
                tesseract.setLanguage(lang);
                tesseract.setPageSegMode(1);  // auto segmentation
                tesseract.setOcrEngineMode(1); // LSTM neural engine

                log.info("OCR — {} pages | langue : {} | DPI : {}", totalPages, lang, RENDER_DPI);

                for (int i = 0; i < totalPages; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(
                        i, RENDER_DPI, ImageType.RGB
                    );

                    String pageText = tesseract.doOCR(image);
                    fullText.append("=== Page ").append(i + 1)
                            .append(" ===\n")
                            .append(pageText)
                            .append("\n\n");

                    log.info("  OCR page {} — {} caracteres", i + 1, pageText.length());

                    // Libere explicitement la memoire avant la page suivante :
                    // BufferedImage de plusieurs Mo, GC pas toujours assez rapide.
                    image.flush();
                }

                return fullText.toString();
            }

        } catch (PDFException e) {
            throw e;
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
