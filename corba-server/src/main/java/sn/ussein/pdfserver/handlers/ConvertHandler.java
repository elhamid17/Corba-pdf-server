package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.ConvertOptions;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Convertit chaque page d'un PDF en image (PNG, JPEG ou TIFF).
 * Retourne un tableau d'images — une par page.
 */
public class ConvertHandler {

    private static final Logger log = LoggerFactory.getLogger(ConvertHandler.class);

    public byte[][] convertToImages(byte[] pdf, ConvertOptions options)
            throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        String format = (options.format != null && !options.format.isEmpty())
            ? options.format.toLowerCase() : "png";

        if (!format.equals("png") && !format.equals("jpeg") && !format.equals("tiff")) {
            throw new PDFException("INVALID_FORMAT",
                "Format non supporté : " + format + ". Utilisez PNG, JPEG ou TIFF");
        }

        float dpi = options.dpi > 0 ? options.dpi : 150f;

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                PDFRenderer renderer = new PDFRenderer(doc);
                int totalPages = doc.getNumberOfPages();
                byte[][] images = new byte[totalPages][];

                log.info("Conversion PDF → {} | {} pages | {} DPI",
                    format.toUpperCase(), totalPages, dpi);

                for (int i = 0; i < totalPages; i++) {
                    BufferedImage image = renderer.renderImageWithDPI(
                        i, dpi, ImageType.RGB
                    );
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ImageIO.write(image, format, out);
                    images[i] = out.toByteArray();
                    log.info("  Page {} → {} octets", i + 1, images[i].length);
                }

                return images;
            }

        } catch (Exception e) {
            log.error("Erreur lors de la conversion PDF → images", e);
            throw new PDFException("CONVERT_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
