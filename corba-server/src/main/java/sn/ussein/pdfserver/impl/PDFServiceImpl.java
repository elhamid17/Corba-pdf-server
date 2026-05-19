package sn.ussein.pdfserver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.*;
import sn.ussein.pdfserver.handlers.*;

/**
 * Implémentation du skeleton CORBA PDFService.
 *
 * Ce fichier ne contient AUCUNE logique PDF — il délègue
 * chaque opération à son handler dédié.
 * Principe : un service = un handler = un fichier.
 */
public class PDFServiceImpl extends PDFServicePOA {

    private static final Logger log = LoggerFactory.getLogger(PDFServiceImpl.class);

    // ── Handlers ──
    private final MergeHandler       mergeHandler       = new MergeHandler();
    private final SplitHandler       splitHandler       = new SplitHandler();
    private final ExtractHandler     extractHandler     = new ExtractHandler();
    private final DeletePageHandler  deletePageHandler  = new DeletePageHandler();
    private final CompressHandler    compressHandler    = new CompressHandler();
    private final RotateHandler      rotateHandler      = new RotateHandler();
    private final WatermarkHandler   watermarkHandler   = new WatermarkHandler();
    private final PasswordHandler    passwordHandler    = new PasswordHandler();
    private final ConvertHandler     convertHandler     = new ConvertHandler();
    private final TextExtractHandler textExtractHandler = new TextExtractHandler();
    private final OcrHandler         ocrHandler         = new OcrHandler();
    private final SignHandler        signHandler        = new SignHandler();
    private final MetadataHandler    metadataHandler    = new MetadataHandler();
    private final CreateHandler      createHandler      = new CreateHandler();
    private final PdfToWordHandler   pdfToWordHandler   = new PdfToWordHandler();
    private final PdfToExcelHandler  pdfToExcelHandler  = new PdfToExcelHandler();
    private final WordToPdfHandler   wordToPdfHandler   = new WordToPdfHandler();
    private final ImagesToPdfHandler imagesToPdfHandler = new ImagesToPdfHandler();

    // ════════════════════════════════════════
    //  1. Fusion
    // ════════════════════════════════════════
    @Override
    public PDFResult merge(byte[][] pdfs) throws PDFException {
        log.info("→ merge() — {} fichiers", pdfs.length);
        return mergeHandler.merge(pdfs);
    }

    // ════════════════════════════════════════
    //  2. Découpage
    // ════════════════════════════════════════
    @Override
    public byte[][] split(byte[] pdf, int[] ranges)
            throws PDFException, InvalidPageException {
        log.info("→ split() — {} intervalles", ranges.length);
        return splitHandler.split(pdf, ranges);
    }

    // ════════════════════════════════════════
    //  3. Extraction de pages
    // ════════════════════════════════════════
    @Override
    public PDFResult extractPages(byte[] pdf, int[] pages)
            throws PDFException, InvalidPageException {
        log.info("→ extractPages() — pages : {}", pages.length);
        return extractHandler.extractPages(pdf, pages);
    }

    // ════════════════════════════════════════
    //  4. Suppression de pages
    // ════════════════════════════════════════
    @Override
    public PDFResult deletePages(byte[] pdf, int[] pages)
            throws PDFException, InvalidPageException {
        log.info("→ deletePages() — {} pages à supprimer", pages.length);
        return deletePageHandler.deletePages(pdf, pages);
    }

    // ════════════════════════════════════════
    //  5. Compression
    // ════════════════════════════════════════
    @Override
    public PDFResult compress(byte[] pdf, CompressOptions options)
            throws PDFException {
        log.info("→ compress()");
        return compressHandler.compress(pdf, options);
    }

    // ════════════════════════════════════════
    //  6. Rotation
    // ════════════════════════════════════════
    @Override
    public PDFResult rotate(byte[] pdf, int[] pages, int angle)
            throws PDFException, InvalidPageException {
        log.info("→ rotate() — angle : {}°", angle);
        return rotateHandler.rotate(pdf, pages, angle);
    }

    // ════════════════════════════════════════
    //  7. Filigrane
    // ════════════════════════════════════════
    @Override
    public PDFResult addWatermark(byte[] pdf, WatermarkOptions options)
            throws PDFException {
        log.info("→ addWatermark() — texte : '{}'", options.text);
        return watermarkHandler.addWatermark(pdf, options);
    }

    // ════════════════════════════════════════
    //  8. Mot de passe
    // ════════════════════════════════════════
    @Override
    public PDFResult addPassword(byte[] pdf,
                                  String userPassword,
                                  String ownerPassword)
            throws PDFException, PasswordException {
        log.info("→ addPassword()");
        return passwordHandler.addPassword(pdf, userPassword, ownerPassword);
    }

    // ════════════════════════════════════════
    //  9. Conversion PDF → Images
    // ════════════════════════════════════════
    @Override
    public byte[][] convertToImages(byte[] pdf, ConvertOptions options)
            throws PDFException {
        log.info("→ convertToImages() — format : {}, dpi : {}", options.format, options.dpi);
        return convertHandler.convertToImages(pdf, options);
    }

    // ════════════════════════════════════════
    //  10. Extraction de texte
    // ════════════════════════════════════════
    @Override
    public String extractText(byte[] pdf) throws PDFException {
        log.info("→ extractText()");
        return textExtractHandler.extractText(pdf);
    }

    // ════════════════════════════════════════
    //  11. OCR
    // ════════════════════════════════════════
    @Override
    public String performOCR(byte[] pdf, String language) throws PDFException {
        log.info("→ performOCR() — langue : {}", language);
        return ocrHandler.performOCR(pdf, language);
    }

    // ════════════════════════════════════════
    //  12. Signature numérique
    // ════════════════════════════════════════
    @Override
    public PDFResult sign(byte[] pdf, byte[] certificate,
                           String password, String reason, String location)
            throws PDFException {
        log.info("→ sign() — raison : '{}'", reason);
        return signHandler.sign(pdf, certificate, password, reason, location);
    }

    // ════════════════════════════════════════
    //  13. Métadonnées — lecture
    // ════════════════════════════════════════
    @Override
    public PDFMetadata getMetadata(byte[] pdf) throws PDFException {
        log.info("→ getMetadata()");
        return metadataHandler.getMetadata(pdf);
    }

    // ════════════════════════════════════════
    //  13. Métadonnées — écriture
    // ════════════════════════════════════════
    @Override
    public PDFResult setMetadata(byte[] pdf, PDFMetadata metadata)
            throws PDFException {
        log.info("→ setMetadata()");
        return metadataHandler.setMetadata(pdf, metadata);
    }

    // ════════════════════════════════════════
    //  14. Création depuis texte
    // ════════════════════════════════════════
    @Override
    public PDFResult createFromText(String text, String title)
            throws PDFException {
        log.info("→ createFromText() — titre : '{}'", title);
        return createHandler.createFromText(text, title);
    }

    // ════════════════════════════════════════
    //  15. PDF → Word
    // ════════════════════════════════════════
    @Override
    public PDFResult pdfToWord(byte[] pdf) throws PDFException {
        log.info("→ pdfToWord()");
        return pdfToWordHandler.pdfToWord(pdf);
    }

    // ════════════════════════════════════════
    //  16. PDF → Excel
    // ════════════════════════════════════════
    @Override
    public PDFResult pdfToExcel(byte[] pdf) throws PDFException {
        log.info("→ pdfToExcel()");
        return pdfToExcelHandler.pdfToExcel(pdf);
    }

    // ════════════════════════════════════════
    //  17. Word → PDF
    // ════════════════════════════════════════
    @Override
    public PDFResult wordToPdf(byte[] docx) throws PDFException {
        log.info("→ wordToPdf()");
        return wordToPdfHandler.wordToPdf(docx);
    }

    // ════════════════════════════════════════
    //  18. Images → PDF
    // ════════════════════════════════════════
    @Override
    public PDFResult imagesToPdf(byte[][] images) throws PDFException {
        log.info("→ imagesToPdf() — {} images", images != null ? images.length : 0);
        return imagesToPdfHandler.imagesToPdf(images);
    }

    // ════════════════════════════════════════
    //  Utilitaires
    // ════════════════════════════════════════
    @Override
    public int getPageCount(byte[] pdf) throws PDFException {
        log.info("→ getPageCount()");
        return metadataHandler.getPageCount(pdf);
    }

    @Override
    public String ping() {
        log.info("→ ping()");
        return "CORBA PDF Server — OK — " + java.time.LocalDateTime.now();
    }
}