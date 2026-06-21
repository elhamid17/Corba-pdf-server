package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Recadre toutes les pages en retirant des marges en pourcentage (0 a 50).
 * Modifie le CropBox (vue) plutot que le MediaBox pour preserver le contenu.
 */
public class CropHandler {

    private static final Logger log = LoggerFactory.getLogger(CropHandler.class);

    public PDFResult cropPages(byte[] pdf, float left, float top,
                               float right, float bottom) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (left < 0 || top < 0 || right < 0 || bottom < 0
            || left > 50 || top > 50 || right > 50 || bottom > 50) {
            throw new PDFException("INVALID_INPUT",
                "Les marges doivent etre comprises entre 0 et 50 (%)");
        }
        if (left + right >= 100 || top + bottom >= 100) {
            throw new PDFException("INVALID_INPUT",
                "Les marges cumulees (G+D ou H+B) ne peuvent depasser 100%");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int total = doc.getNumberOfPages();

                for (int i = 0; i < total; i++) {
                    PDPage page = doc.getPage(i);
                    PDRectangle box = page.getMediaBox();
                    float w = box.getWidth();
                    float h = box.getHeight();

                    float newLowerX = box.getLowerLeftX() + w * (left   / 100f);
                    float newLowerY = box.getLowerLeftY() + h * (bottom / 100f);
                    float newWidth  = w * (1 - (left + right)  / 100f);
                    float newHeight = h * (1 - (top + bottom) / 100f);

                    PDRectangle crop = new PDRectangle(newLowerX, newLowerY, newWidth, newHeight);
                    page.setCropBox(crop);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Recadrage : {} pages (marges G:{}% H:{}% D:{}% B:{}%)",
                    total, left, top, right, bottom);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = total + " pages recadrees";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur cropPages", e);
            throw new PDFException("CROP_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
