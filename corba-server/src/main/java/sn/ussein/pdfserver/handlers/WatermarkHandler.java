package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdf.WatermarkOptions;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Ajoute un filigrane texte sur toutes les pages d'un PDF.
 * Supporte l'affichage diagonal et le contrôle de l'opacité.
 */
public class WatermarkHandler {

    private static final Logger log = LoggerFactory.getLogger(WatermarkHandler.class);

    public PDFResult addWatermark(byte[] pdf, WatermarkOptions options)
            throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (options.text == null || options.text.trim().isEmpty()) {
            throw new PDFException("INVALID_INPUT", "Le texte du filigrane est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {

                float opacity   = Math.max(0f, Math.min(1f, options.opacity));
                int   fontSize  = options.fontSize > 0 ? options.fontSize : 48;

                for (PDPage page : doc.getPages()) {
                    PDRectangle mediaBox = page.getMediaBox();
                    float pageWidth  = mediaBox.getWidth();
                    float pageHeight = mediaBox.getHeight();

                    try (PDPageContentStream cs = new PDPageContentStream(
                            doc, page,
                            PDPageContentStream.AppendMode.APPEND,
                            true, true)) {

                        cs.setFont(PDType1Font.HELVETICA_BOLD, fontSize);

                        // Opacité via état graphique
                        org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState gs =
                            new org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState();
                        gs.setNonStrokingAlphaConstant(opacity);
                        gs.setAlphaSourceFlag(true);
                        cs.setGraphicsStateParameters(gs);

                        cs.setNonStrokingColor(0.75f, 0.75f, 0.75f); // gris clair

                        if (options.diagonal) {
                            // Centrer en diagonale
                            cs.saveGraphicsState();
                            cs.transform(Matrix.getRotateInstance(
                                Math.toRadians(45),
                                pageWidth / 2,
                                pageHeight / 2
                            ));
                            float textWidth = PDType1Font.HELVETICA_BOLD
                                .getStringWidth(options.text) / 1000 * fontSize;
                            cs.beginText();
                            cs.newLineAtOffset(-textWidth / 2, 0);
                            cs.showText(options.text);
                            cs.endText();
                            cs.restoreGraphicsState();
                        } else {
                            // Centré horizontal, milieu vertical
                            float textWidth = PDType1Font.HELVETICA_BOLD
                                .getStringWidth(options.text) / 1000 * fontSize;
                            cs.beginText();
                            cs.newLineAtOffset(
                                (pageWidth - textWidth) / 2,
                                pageHeight / 2
                            );
                            cs.showText(options.text);
                            cs.endText();
                        }
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                byte[] result = out.toByteArray();
                log.info("Filigrane '{}' ajouté sur {} pages",
                    options.text, doc.getNumberOfPages());

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "Filigrane ajouté avec succès";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du filigrane", e);
            throw new PDFException("WATERMARK_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}