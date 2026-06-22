package sn.ussein.pdfengine;

import sn.ussein.pdfengine.model.CompressOptions;
import sn.ussein.pdfengine.model.ConvertOptions;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFMetadata;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfengine.model.PasswordException;
import sn.ussein.pdfengine.model.WatermarkOptions;

/**
 * Façade in-process du moteur de traitement PDF.
 *
 * <p>Interface Java idiomatique reprenant <strong>une à une</strong> les 42 opérations
 * métier de l'ancienne interface IDL {@code sn.ussein.pdf.PDFService} (cf.
 * {@code corba-idl/src/main/idl/PDFService.idl}), plus les deux utilitaires
 * {@link #getPageCount(byte[])} et {@link #ping()}.</p>
 *
 * <p>Aucune dépendance CORBA/JacORB : les anciens types générés sont remplacés par
 * des types Java standard et les POJO de {@code sn.ussein.pdfengine.model} :</p>
 * <ul>
 *   <li>{@code PDFData}  → {@code byte[]}</li>
 *   <li>{@code PDFList}  → {@code byte[][]}</li>
 *   <li>{@code PageList} → {@code int[]}</li>
 *   <li>{@code StringList} → {@code String[]}</li>
 *   <li>{@code PDFResult / PDFMetadata / *Options} → POJO de {@code .model}</li>
 *   <li>exceptions IDL → exceptions Java de {@code .model} (héritent de {@link Exception})</li>
 * </ul>
 *
 * <p>L'implémentation de référence est {@link PdfEngineImpl}, qui délègue chaque
 * opération à son handler dédié sans modifier la logique métier existante.</p>
 */
public interface PdfEngine {

    // ═══ 1. Fusion ═══
    PDFResult merge(byte[][] pdfs) throws PDFException;

    // ═══ 2. Découpage ═══
    byte[][] split(byte[] pdf, int[] ranges) throws PDFException, InvalidPageException;

    // ═══ 3. Extraction de pages ═══
    PDFResult extractPages(byte[] pdf, int[] pages) throws PDFException, InvalidPageException;

    // ═══ 4. Suppression de pages ═══
    PDFResult deletePages(byte[] pdf, int[] pages) throws PDFException, InvalidPageException;

    // ═══ 5. Compression ═══
    PDFResult compress(byte[] pdf, CompressOptions options) throws PDFException;

    // ═══ 6. Rotation ═══
    PDFResult rotate(byte[] pdf, int[] pages, int angle) throws PDFException, InvalidPageException;

    // ═══ 7. Filigrane ═══
    PDFResult addWatermark(byte[] pdf, WatermarkOptions options) throws PDFException;

    // ═══ 8. Mot de passe ═══
    PDFResult addPassword(byte[] pdf, String userPassword, String ownerPassword)
            throws PDFException, PasswordException;

    // ═══ 9. Conversion PDF → Images ═══
    byte[][] convertToImages(byte[] pdf, ConvertOptions options) throws PDFException;

    // ═══ 10. Extraction de texte ═══
    String extractText(byte[] pdf) throws PDFException;

    // ═══ 11. OCR ═══
    String performOCR(byte[] pdf, String language) throws PDFException;

    // ═══ 12. Signature numérique ═══
    PDFResult sign(byte[] pdf, byte[] certificate, String password, String reason, String location)
            throws PDFException;

    // ═══ 13. Métadonnées — lecture ═══
    PDFMetadata getMetadata(byte[] pdf) throws PDFException;

    // ═══ 13. Métadonnées — écriture ═══
    PDFResult setMetadata(byte[] pdf, PDFMetadata metadata) throws PDFException;

    // ═══ 14. Création depuis texte ═══
    PDFResult createFromText(String text, String title) throws PDFException;

    // ═══ 15. PDF → Word ═══
    PDFResult pdfToWord(byte[] pdf) throws PDFException;

    // ═══ 16. PDF → Excel ═══
    PDFResult pdfToExcel(byte[] pdf) throws PDFException;

    // ═══ 17. Word → PDF ═══
    PDFResult wordToPdf(byte[] docx) throws PDFException;

    // ═══ 18. Images → PDF ═══
    PDFResult imagesToPdf(byte[][] images) throws PDFException;

    // ═══ 19. Inversion des pages ═══
    PDFResult reversePages(byte[] pdf) throws PDFException;

    // ═══ 20. Numérotation des pages ═══
    PDFResult addPageNumbers(byte[] pdf, String position, String format, int startNumber, int fontSize)
            throws PDFException;

    // ═══ 21. Redimensionnement ═══
    PDFResult resizePages(byte[] pdf, String targetSize) throws PDFException;

    // ═══ 22. Recadrage ═══
    PDFResult cropPages(byte[] pdf, float marginLeft, float marginTop, float marginRight, float marginBottom)
            throws PDFException;

    // ═══ 23. Page de garde ═══
    PDFResult addCoverPage(byte[] pdf, byte[] coverImage) throws PDFException;

    // ═══ 24. Réorganisation des pages ═══
    PDFResult reorderPages(byte[] pdf, int[] order) throws PDFException, InvalidPageException;

    // ═══ 25. Anonymisation ═══
    PDFResult anonymize(byte[] pdf) throws PDFException;

    // ═══ 26. Signature manuscrite (image) ═══
    PDFResult addSignatureImage(byte[] pdf, byte[] signatureImage, int page,
                                float xPercent, float yPercent, float widthPercent)
            throws PDFException, InvalidPageException;

    // ═══ 27. Caviardage textuel ═══
    PDFResult redactText(byte[] pdf, String[] terms) throws PDFException;

    // ═══ 28. Tampon ═══
    PDFResult addStamp(byte[] pdf, String text, int page, String position, String color, int fontSize)
            throws PDFException, InvalidPageException;

    // ═══ 29. PDF → PowerPoint ═══
    PDFResult pdfToPptx(byte[] pdf) throws PDFException;

    // ═══ 30. PDF → Markdown ═══
    PDFResult pdfToMarkdown(byte[] pdf) throws PDFException;

    // ═══ 31. Markdown → PDF ═══
    PDFResult markdownToPdf(String markdown, String title) throws PDFException;

    // ═══ 32. HTML → PDF ═══
    PDFResult htmlToPdf(String html, String title) throws PDFException;

    // ═══ 33. Excel → PDF ═══
    PDFResult excelToPdf(byte[] xlsx) throws PDFException;

    // ═══ 34. ODT → PDF ═══
    PDFResult odtToPdf(byte[] odt) throws PDFException;

    // ═══ 35. Déverrouillage ═══
    PDFResult unlockPdf(byte[] pdf, String password) throws PDFException, PasswordException;

    // ═══ 36. Vérification de signature ═══
    String verifySignature(byte[] pdf) throws PDFException;

    // ═══ 37. Conversion PDF/A ═══
    PDFResult convertToPdfA(byte[] pdf) throws PDFException;

    // ═══ 38. Comparaison de 2 PDFs ═══
    String comparePdfs(byte[] pdfA, byte[] pdfB) throws PDFException;

    // ═══ 39. Statistiques document ═══
    String getDocumentStats(byte[] pdf) throws PDFException;

    // ═══ 40. Ajout de QR code ═══
    PDFResult addQrCode(byte[] pdf, String text, int page, String position, int sizePx)
            throws PDFException, InvalidPageException;

    // ═══ 41. Ajout de code-barres ═══
    PDFResult addBarcode(byte[] pdf, String code, int page, String position, String type, int sizePx)
            throws PDFException, InvalidPageException;

    // ═══ 42. Générateur de CV ═══
    PDFResult generateCv(String cvJson) throws PDFException;

    // ═══ Utilitaire — nombre de pages ═══
    int getPageCount(byte[] pdf) throws PDFException;

    // ═══ Utilitaire — santé du moteur ═══
    String ping();
}
