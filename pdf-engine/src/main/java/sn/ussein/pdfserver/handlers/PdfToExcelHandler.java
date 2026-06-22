package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Convertit un PDF en classeur Excel (XLSX).
 *
 * Stratégie simple : une feuille par page, chaque ligne de texte
 * du PDF devient une ligne dans Excel. Les colonnes sont obtenues
 * par découpage sur le caractère tabulation.
 */
public class PdfToExcelHandler {

    private static final Logger log = LoggerFactory.getLogger(PdfToExcelHandler.class);

    public PDFResult pdfToExcel(byte[] pdf) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile);
                 XSSFWorkbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                int pageCount = doc.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                for (int i = 1; i <= pageCount; i++) {
                    stripper.setStartPage(i);
                    stripper.setEndPage(i);
                    String pageText = stripper.getText(doc);

                    Sheet sheet = workbook.createSheet("Page " + i);
                    String[] lines = pageText.split("\n");
                    int rowIdx = 0;
                    for (String line : lines) {
                        Row row = sheet.createRow(rowIdx++);
                        String[] cells = line.split("\t");
                        for (int c = 0; c < cells.length; c++) {
                            row.createCell(c).setCellValue(cells[c]);
                        }
                    }
                }

                workbook.write(out);
                byte[] result = out.toByteArray();

                log.info("PDF → Excel — {} feuilles | {} octets",
                    pageCount, result.length);

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "Conversion réussie — " + pageCount + " feuille(s)";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur PDF → Excel", e);
            throw new PDFException("PDF_TO_EXCEL_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
