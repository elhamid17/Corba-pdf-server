package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Appose une image de signature (PNG transparent recommande, JPG aussi accepte)
 * sur une page donnee, a une position en pourcentage de la page.
 *
 * Origine en bas-gauche (convention PDF), donc xPercent=50, yPercent=10
 * place la signature centree en bas de page.
 */
public class SignatureImageHandler {

    private static final Logger log = LoggerFactory.getLogger(SignatureImageHandler.class);

    public PDFResult addSignatureImage(byte[] pdf, byte[] signatureImage,
                                       int page, float xPercent, float yPercent,
                                       float widthPercent)
            throws PDFException, InvalidPageException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (signatureImage == null || signatureImage.length == 0) {
            throw new PDFException("INVALID_INPUT", "L'image de signature est vide");
        }
        if (widthPercent <= 0 || widthPercent > 100) widthPercent = 30;

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int total = doc.getNumberOfPages();
                if (page < 1 || page > total) {
                    InvalidPageException ex = new InvalidPageException();
                    ex.requestedPage = page;
                    ex.totalPages = total;
                    ex.message = "Page " + page + " hors limites (total : " + total + ")";
                    throw ex;
                }

                PDPage target = doc.getPage(page - 1);
                PDRectangle box = target.getMediaBox();

                PDImageXObject img = PDImageXObject.createFromByteArray(doc, signatureImage, "signature");

                // Largeur en points = widthPercent % de la page, hauteur proportionnelle
                float drawWidth = box.getWidth() * (widthPercent / 100f);
                float ratio = (float) img.getHeight() / (float) img.getWidth();
                float drawHeight = drawWidth * ratio;

                // Position : on prend xPercent/yPercent comme coin bas-gauche de la signature
                float x = box.getWidth()  * (xPercent / 100f);
                float y = box.getHeight() * (yPercent / 100f);

                // Si la signature deborde, on la replie dans la page
                if (x + drawWidth > box.getWidth()) x = box.getWidth() - drawWidth;
                if (y + drawHeight > box.getHeight()) y = box.getHeight() - drawHeight;
                if (x < 0) x = 0;
                if (y < 0) y = 0;

                try (PDPageContentStream cs = new PDPageContentStream(doc, target,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.drawImage(img, x, y, drawWidth, drawHeight);
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Signature image : page {} a ({}%, {}%), largeur {}%",
                    page, xPercent, yPercent, widthPercent);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = "Signature apposee sur la page " + page;
                return res;
            }
        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur addSignatureImage", e);
            throw new PDFException("SIGNATURE_IMAGE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
