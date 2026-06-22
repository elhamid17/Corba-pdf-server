package sn.ussein.pdfserver.handlers;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.EncodeHintType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

/**
 * Genere un QR code ou un code-barres (CODE_128, EAN_13, etc.) avec ZXing
 * et l'appose sur une page du PDF.
 *
 * Mutualise les deux endpoints : la seule difference est le format ZXing
 * et le ratio largeur/hauteur (QR = carre, code-barres = rectangulaire).
 */
public class BarcodeHandler {

    private static final Logger log = LoggerFactory.getLogger(BarcodeHandler.class);
    private static final float MARGIN = 30f;

    public PDFResult addQrCode(byte[] pdf, String text, int page, String position, int sizePx)
            throws PDFException, InvalidPageException {
        return drawBarcode(pdf, text, page, position, "QR_CODE", sizePx);
    }

    public PDFResult addBarcode(byte[] pdf, String code, int page, String position, String type, int sizePx)
            throws PDFException, InvalidPageException {
        return drawBarcode(pdf, code, page, position, type, sizePx);
    }

    private PDFResult drawBarcode(byte[] pdf, String content, int page, String position,
                                  String typeName, int sizePx)
            throws PDFException, InvalidPageException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (content == null || content.isBlank()) {
            throw new PDFException("INVALID_INPUT", "Le contenu du code-barres est vide");
        }
        if (sizePx <= 0) sizePx = 150;
        if (position == null || position.isBlank()) position = "bottom-right";

        BarcodeFormat format;
        try {
            format = BarcodeFormat.valueOf(typeName == null ? "QR_CODE" : typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PDFException("INVALID_INPUT",
                "Type code-barres inconnu : " + typeName
                + " (attendu : QR_CODE, CODE_128, EAN_13, EAN_8, CODE_39, ITF, UPC_A, PDF_417)");
        }

        boolean isSquare = format == BarcodeFormat.QR_CODE
            || format == BarcodeFormat.AZTEC
            || format == BarcodeFormat.DATA_MATRIX;
        int width = sizePx;
        int height = isSquare ? sizePx : Math.max(40, sizePx / 3);

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            // Genere l'image PNG du code via ZXing
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 1);
            if (format == BarcodeFormat.QR_CODE) {
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            }
            BitMatrix matrix = new MultiFormatWriter().encode(content, format, width, height, hints);
            BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream pngBaos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", pngBaos);
            byte[] pngBytes = pngBaos.toByteArray();

            try (PDDocument doc = PDDocument.load(tmpFile);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

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
                PDImageXObject xo = PDImageXObject.createFromByteArray(doc, pngBytes, "barcode");

                // Conversion px -> points (approximation : 1px = 0.75pt)
                float w = width * 0.75f;
                float h = height * 0.75f;

                float[] xy = computePosition(position, box, w, h);

                try (PDPageContentStream cs = new PDPageContentStream(doc, target,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    cs.drawImage(xo, xy[0], xy[1], w, h);
                }

                doc.save(out);
                log.info("{} '{}' appose page {} en {}", typeName, content, page, position);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = typeName + " appose sur la page " + page;
                return res;
            }
        } catch (InvalidPageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur drawBarcode", e);
            throw new PDFException("BARCODE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private float[] computePosition(String position, PDRectangle box, float w, float h) {
        float W = box.getWidth(), H = box.getHeight();
        float x, y;
        if (position.endsWith("-left")) x = MARGIN;
        else if (position.endsWith("-right")) x = W - w - MARGIN;
        else x = (W - w) / 2f;
        if (position.startsWith("top-")) y = H - h - MARGIN;
        else y = MARGIN;
        return new float[]{x, y};
    }
}
