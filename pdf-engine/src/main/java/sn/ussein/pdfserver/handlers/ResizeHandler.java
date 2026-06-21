package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Redimensionne toutes les pages d'un PDF vers un format standard.
 *
 * Strategie : recadrage du mediabox + scaling proportionnel du contenu
 * existant pour qu'il rentre dans le nouveau format sans deformation.
 *
 * Formats supportes : A3, A4, A5, LETTER, LEGAL
 */
public class ResizeHandler {

    private static final Logger log = LoggerFactory.getLogger(ResizeHandler.class);

    private static final Map<String, PDRectangle> SIZES = new HashMap<>();
    static {
        SIZES.put("A3", PDRectangle.A3);
        SIZES.put("A4", PDRectangle.A4);
        SIZES.put("A5", PDRectangle.A5);
        SIZES.put("LETTER", PDRectangle.LETTER);
        SIZES.put("LEGAL", PDRectangle.LEGAL);
    }

    public PDFResult resizePages(byte[] pdf, String targetSize) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        String key = targetSize == null ? "A4" : targetSize.trim().toUpperCase();
        PDRectangle target = SIZES.get(key);
        if (target == null) {
            throw new PDFException("INVALID_SIZE",
                "Format inconnu : " + key + " (attendu : A3, A4, A5, LETTER, LEGAL)");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument source = PDDocument.load(tmpFile);
                 PDDocument out = new PDDocument()) {

                int total = source.getNumberOfPages();

                org.apache.pdfbox.multipdf.LayerUtility lu =
                    new org.apache.pdfbox.multipdf.LayerUtility(out);

                for (int i = 0; i < total; i++) {
                    PDPage src = source.getPage(i);
                    PDRectangle srcBox = src.getMediaBox();

                    PDPage newPage = new PDPage(target);
                    out.addPage(newPage);

                    // Importe la page source comme XObject reutilisable
                    PDFormXObject form = lu.importPageAsForm(source, i);

                    // Calcule l'echelle preservant les proportions
                    float scaleX = target.getWidth()  / srcBox.getWidth();
                    float scaleY = target.getHeight() / srcBox.getHeight();
                    float scale = Math.min(scaleX, scaleY);

                    // Centre la page redimensionnee dans le nouveau format
                    float dx = (target.getWidth()  - srcBox.getWidth()  * scale) / 2f;
                    float dy = (target.getHeight() - srcBox.getHeight() * scale) / 2f;

                    try (PDPageContentStream cs = new PDPageContentStream(out, newPage,
                            PDPageContentStream.AppendMode.OVERWRITE, false, false)) {
                        Matrix m = Matrix.getTranslateInstance(dx, dy);
                        m.concatenate(Matrix.getScaleInstance(scale, scale));
                        cs.saveGraphicsState();
                        cs.transform(m);
                        cs.drawForm(form);
                        cs.restoreGraphicsState();
                    }
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                out.save(baos);

                log.info("Redimensionnement : {} pages -> {}", total, key);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = baos.toByteArray();
                res.message = total + " pages redimensionnees en " + key;
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur resizePages", e);
            throw new PDFException("RESIZE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
