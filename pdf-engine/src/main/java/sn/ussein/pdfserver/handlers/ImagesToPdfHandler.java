package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;

import java.io.ByteArrayOutputStream;

/**
 * Convertit une ou plusieurs images (JPG/PNG) en un PDF.
 * Une image = une page. La taille de la page s'adapte au format A4
 * en préservant le ratio de l'image (lettre-box centré).
 */
public class ImagesToPdfHandler {

    private static final Logger log = LoggerFactory.getLogger(ImagesToPdfHandler.class);

    public PDFResult imagesToPdf(byte[][] images) throws PDFException {

        if (images == null || images.length == 0) {
            throw new PDFException("INVALID_INPUT", "Aucune image fournie");
        }

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            float pageW = PDRectangle.A4.getWidth();
            float pageH = PDRectangle.A4.getHeight();

            for (int i = 0; i < images.length; i++) {
                byte[] imgBytes = images[i];
                if (imgBytes == null || imgBytes.length == 0) {
                    throw new PDFException("INVALID_INPUT",
                        "Image " + (i + 1) + " vide");
                }

                PDImageXObject pdImg = PDImageXObject.createFromByteArray(
                    doc, imgBytes, "image-" + (i + 1));

                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                // Calcul du ratio pour conserver les proportions
                float imgW = pdImg.getWidth();
                float imgH = pdImg.getHeight();
                float ratio = Math.min(pageW / imgW, pageH / imgH);
                float drawW = imgW * ratio;
                float drawH = imgH * ratio;
                float x = (pageW - drawW) / 2;
                float y = (pageH - drawH) / 2;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImg, x, y, drawW, drawH);
                }
            }

            doc.save(out);
            byte[] result = out.toByteArray();

            log.info("Images → PDF — {} pages | {} octets",
                doc.getNumberOfPages(), result.length);

            PDFResult res = new PDFResult();
            res.success = true;
            res.data    = result;
            res.message = "Conversion réussie — " + doc.getNumberOfPages() + " page(s)";
            return res;
        } catch (PDFException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur Images → PDF", e);
            throw new PDFException("IMAGES_TO_PDF_ERROR", e.getMessage());
        }
    }
}
