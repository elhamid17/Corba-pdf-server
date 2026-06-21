package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Convertit un PDF en presentation PowerPoint (PPTX).
 * Approche : une slide par page PDF, contenant le texte extrait.
 * Mise en forme avancee (images, layouts) non preservee.
 */
public class PdfToPptxHandler {

    private static final Logger log = LoggerFactory.getLogger(PdfToPptxHandler.class);

    public PDFResult pdfToPptx(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile);
                 XMLSlideShow pptx = new XMLSlideShow();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                int total = doc.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();

                for (int i = 1; i <= total; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String pageText = stripper.getText(doc).trim();

                    XSLFSlide slide = pptx.createSlide();
                    XSLFTextBox tb = slide.createTextBox();
                    // Couvre la slide entiere avec une marge
                    tb.setAnchor(new Rectangle2D.Double(40, 40,
                        pptx.getPageSize().getWidth() - 80,
                        pptx.getPageSize().getHeight() - 80));

                    boolean first = true;
                    for (String line : pageText.split("\\r?\\n")) {
                        XSLFTextParagraph p = first ? tb.getTextParagraphs().get(0) : tb.addNewTextParagraph();
                        first = false;
                        XSLFTextRun run = p.addNewTextRun();
                        run.setText(sanitize(line));
                        run.setFontSize(14d);
                        p.setTextAlign(TextParagraph.TextAlign.LEFT);
                    }
                }

                pptx.write(out);
                byte[] result = out.toByteArray();

                log.info("PDF -> PPTX : {} slides, {} octets", total, result.length);
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = result;
                res.message = total + " slide(s) generee(s)";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur pdfToPptx", e);
            throw new PDFException("PDF_TO_PPTX_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= 0x20 || c == '\t') sb.append(c);
        }
        return sb.toString();
    }
}
