package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.StyledPdfRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Convertit un classeur Excel (XLSX) en PDF.
 *
 * Approche : une feuille = une serie de pages PDF.
 * Chaque ligne de la feuille est dessinee en grille simple
 * (largeurs de colonne uniformes selon le max constate).
 *
 * Limitations : pas de styles cellules, pas de fusion, pas de formules
 * affichees brutes (POI les evalue automatiquement quand possible).
 */
public class ExcelToPdfHandler {

    private static final Logger log = LoggerFactory.getLogger(ExcelToPdfHandler.class);

    private static final float MARGIN = 30f;
    private static final float ROW_HEIGHT = 16f;
    private static final float HEADER_HEIGHT = 22f;
    private static final float FONT_SIZE = 9f;
    private static final int MAX_COLS = 12;       // limite pratique
    private static final int MAX_CELL_CHARS = 40; // tronque les cellules trop longues

    public PDFResult excelToPdf(byte[] xlsx) throws PDFException {
        if (xlsx == null || xlsx.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le fichier Excel fourni est vide");
        }

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx));
             PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            DataFormatter formatter = new DataFormatter();
            int totalSheets = wb.getNumberOfSheets();

            for (int s = 0; s < totalSheets; s++) {
                Sheet sheet = wb.getSheetAt(s);
                renderSheet(doc, sheet, formatter);
            }

            if (doc.getNumberOfPages() == 0) {
                // Au cas ou le classeur est vide
                doc.addPage(new PDPage(landscape()));
            }

            doc.save(out);
            byte[] result = out.toByteArray();
            log.info("Excel -> PDF : {} feuilles, {} pages, {} octets",
                totalSheets, doc.getNumberOfPages(), result.length);

            PDFResult res = new PDFResult();
            res.success = true;
            res.data = result;
            res.message = totalSheets + " feuille(s) converties en PDF";
            return res;
        } catch (Exception e) {
            log.error("Erreur excelToPdf", e);
            throw new PDFException("EXCEL_TO_PDF_ERROR", e.getMessage());
        }
    }

    /** A4 paysage (PDFBox 2.x n'expose pas .rotate() sur PDRectangle). */
    private static PDRectangle landscape() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }

    private void renderSheet(PDDocument doc, Sheet sheet, DataFormatter fmt) throws Exception {
        int lastRow = sheet.getLastRowNum();
        if (lastRow < 0) return;

        // Determine le nombre de colonnes utilisees
        int maxCol = 0;
        for (int r = sheet.getFirstRowNum(); r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row != null) maxCol = Math.max(maxCol, row.getLastCellNum());
        }
        int cols = Math.min(MAX_COLS, Math.max(1, maxCol));

        // Page en paysage pour avoir plus de largeur
        PDRectangle pageFormat = landscape();
        float pageWidth = pageFormat.getWidth();
        float pageHeight = pageFormat.getHeight();
        float gridWidth = pageWidth - 2 * MARGIN;
        float colWidth = gridWidth / cols;

        PDPage page = new PDPage(pageFormat);
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);

        // En-tete : nom de la feuille
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 14f);
        cs.newLineAtOffset(MARGIN, pageHeight - MARGIN - 4);
        cs.showText(StyledPdfRenderer.sanitize("Feuille : " + sheet.getSheetName()));
        cs.endText();

        float y = pageHeight - MARGIN - HEADER_HEIGHT;

        for (int r = sheet.getFirstRowNum(); r <= lastRow; r++) {
            if (y < MARGIN + ROW_HEIGHT) {
                cs.close();
                page = new PDPage(pageFormat);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = pageHeight - MARGIN;
            }

            Row row = sheet.getRow(r);
            for (int c = 0; c < cols; c++) {
                float x = MARGIN + c * colWidth;
                // Cadre cellule
                cs.setStrokingColor(0.7f);
                cs.setLineWidth(0.4f);
                cs.addRect(x, y - ROW_HEIGHT, colWidth, ROW_HEIGHT);
                cs.stroke();

                if (row != null) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        String text = StyledPdfRenderer.sanitize(fmt.formatCellValue(cell));
                        if (text.length() > MAX_CELL_CHARS) {
                            text = text.substring(0, MAX_CELL_CHARS - 1) + "…";
                        }
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                        cs.newLineAtOffset(x + 3, y - ROW_HEIGHT + 4);
                        cs.showText(text);
                        cs.endText();
                    }
                }
            }
            y -= ROW_HEIGHT;
        }
        cs.close();
    }
}
