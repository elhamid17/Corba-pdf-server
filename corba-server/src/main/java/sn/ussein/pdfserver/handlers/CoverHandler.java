package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Ajoute une page de garde au debut d'un PDF.
 * La page de garde est une image (PNG/JPEG) qui est affichee plein cadre
 * sur une page au meme format que la 1ere page du document.
 */
public class CoverHandler {

    private static final Logger log = LoggerFactory.getLogger(CoverHandler.class);

    public PDFResult addCoverPage(byte[] pdf, byte[] coverImage) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (coverImage == null || coverImage.length == 0) {
            throw new PDFException("INVALID_INPUT", "L'image de couverture est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                // Format inspire de la 1ere page du document, sinon A4
                PDRectangle format = doc.getNumberOfPages() > 0
                    ? doc.getPage(0).getMediaBox()
                    : PDRectangle.A4;

                PDPage cover = new PDPage(format);
                doc.getPages().insertBefore(cover,
                    doc.getNumberOfPages() > 0 ? doc.getPage(0) : null);

                PDImageXObject img = PDImageXObject.createFromByteArray(doc, coverImage, "cover");

                // Calcule l'echelle pour faire rentrer l'image en preservant les proportions
                float scale = Math.min(format.getWidth() / img.getWidth(),
                                       format.getHeight() / img.getHeight());
                float w = img.getWidth() * scale;
                float h = img.getHeight() * scale;
                float x = (format.getWidth() - w) / 2f;
                float y = (format.getHeight() - h) / 2f;

                try (PDPageContentStream cs = new PDPageContentStream(doc, cover)) {
                    cs.drawImage(img, x, y, w, h);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Page de garde ajoutee ({} octets image)", coverImage.length);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = "Page de garde ajoutee en debut de document";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur addCoverPage", e);
            throw new PDFException("COVER_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
