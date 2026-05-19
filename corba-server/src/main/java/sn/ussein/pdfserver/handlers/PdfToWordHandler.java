package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Convertit un PDF en document Word (DOCX).
 *
 * Stratégie : extraction du texte page par page avec PDFBox,
 * puis génération d'un DOCX avec Apache POI (un saut de page entre pages).
 * Les images et la mise en page ne sont pas préservées.
 */
public class PdfToWordHandler {

    private static final Logger log = LoggerFactory.getLogger(PdfToWordHandler.class);

    public PDFResult pdfToWord(byte[] pdf) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile);
                 XWPFDocument docx = new XWPFDocument();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                int pageCount = doc.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                for (int i = 1; i <= pageCount; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String pageText = stripper.getText(doc);

                    for (String line : pageText.split("\n")) {
                        XWPFParagraph p = docx.createParagraph();
                        XWPFRun r = p.createRun();
                        r.setText(line);
                    }

                    if (i < pageCount) {
                        XWPFParagraph br = docx.createParagraph();
                        br.createRun().addBreak(BreakType.PAGE);
                    }
                }

                docx.write(out);
                byte[] result = out.toByteArray();

                log.info("PDF → Word — {} pages converties | {} octets",
                    pageCount, result.length);

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "Conversion réussie — " + pageCount + " page(s)";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur PDF → Word", e);
            throw new PDFException("PDF_TO_WORD_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
