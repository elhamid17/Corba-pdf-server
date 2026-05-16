package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.CompressOptions;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Iterator;

/**
 * Compresse un PDF en réduisant la qualité des images
 * et en supprimant optionnellement les métadonnées inutiles.
 */
public class CompressHandler {

    private static final Logger log = LoggerFactory.getLogger(CompressHandler.class);

    public PDFResult compress(byte[] pdf, CompressOptions options)
            throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {

                // ── Suppression des métadonnées ──
                if (options.removeMetadata) {
                    doc.getDocumentInformation().setCustomMetadataValue("Keywords", null);
                    doc.getDocumentCatalog().setMetadata(null);
                    log.info("Métadonnées supprimées");
                }

                // ── Compression des images ──
                if (options.compressImages) {
                    float quality = Math.max(0f, Math.min(100f, options.imageQuality)) / 100f;
                    log.info("Compression des images — qualité : {}%", options.imageQuality);

                    for (PDPage page : doc.getPages()) {
                        compressPageImages(doc, page, quality);
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                byte[] result = out.toByteArray();
                int gain = (int) (100.0 - (result.length * 100.0 / pdf.length));
                log.info("Compression réussie — {} → {} octets (gain : {}%)",
                    pdf.length, result.length, gain);

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "Compression réussie — gain de " + gain + "%";
                return res;
            }

        } catch (PDFException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la compression PDF", e);
            throw new PDFException("COMPRESS_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private void compressPageImages(PDDocument doc, PDPage page, float quality)
            throws Exception {
        PDResources resources = page.getResources();
        if (resources == null) return;

        for (COSName name : resources.getXObjectNames()) {
            if (!resources.isImageXObject(name)) continue;

            PDImageXObject image = (PDImageXObject) resources.getXObject(name);
            BufferedImage buffered = image.getImage();
            if (buffered == null) continue;

            // Re-encoder en JPEG avec la qualité demandée
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) continue;

            ImageWriter writer = writers.next();
            ByteArrayOutputStream imgOut = new ByteArrayOutputStream();

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(imgOut)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.write(null,
                    new javax.imageio.IIOImage(buffered, null, null), param);
            } finally {
                writer.dispose();
            }

            PDImageXObject compressed = PDImageXObject.createFromByteArray(
                doc, imgOut.toByteArray(), name.getName());
            resources.put(name, compressed);
        }
    }
}