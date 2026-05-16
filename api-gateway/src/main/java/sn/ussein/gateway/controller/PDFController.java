package sn.ussein.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sn.ussein.gateway.service.CorbaClientService;
import sn.ussein.pdf.*;

import java.util.Map;

/**
 * Contrôleur REST principal — expose tous les services PDF.
 *
 * Convention des endpoints :
 *   POST /api/pdf/{service}
 *   Les PDFs sont uploadés en multipart/form-data
 *   Les résultats sont retournés en application/pdf ou application/json
 */
@RestController
@RequestMapping("/api/pdf")
public class PDFController {

    private static final Logger log = LoggerFactory.getLogger(PDFController.class);

    private final CorbaClientService corba;

    public PDFController(CorbaClientService corba) {
        this.corba = corba;
    }

    // ══════════════════════════════════════════
    //  Utilitaire — ping
    // ══════════════════════════════════════════
    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        try {
            String response = corba.getPdfService().ping();
            return ResponseEntity.ok(Map.of("status", "OK", "server", response));
        } catch (Exception e) {
            return ResponseEntity.status(503)
                .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════
    //  1. Fusion
    // ══════════════════════════════════════════
    @PostMapping("/merge")
    public ResponseEntity<byte[]> merge(
            @RequestParam("files") MultipartFile[] files) {
        try {
            byte[][] pdfs = new byte[files.length][];
            for (int i = 0; i < files.length; i++) {
                pdfs[i] = files[i].getBytes();
            }
            PDFResult result = corba.getPdfService().merge(pdfs);
            return pdfResponse(result, "merged.pdf");
        } catch (Exception e) {
            log.error("Erreur /merge", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  2. Découpage
    // ══════════════════════════════════════════
    @PostMapping("/split")
    public ResponseEntity<byte[][]> split(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ranges") int[] ranges) {
        try {
            byte[][] parts = corba.getPdfService()
                .split(file.getBytes(), ranges);
            return ResponseEntity.ok(parts);
        } catch (Exception e) {
            log.error("Erreur /split", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ══════════════════════════════════════════
    //  3. Extraction de pages
    // ══════════════════════════════════════════
    @PostMapping("/extract-pages")
    public ResponseEntity<byte[]> extractPages(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pages") int[] pages) {
        try {
            PDFResult result = corba.getPdfService()
                .extractPages(file.getBytes(), pages);
            return pdfResponse(result, "extracted.pdf");
        } catch (Exception e) {
            log.error("Erreur /extract-pages", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  4. Suppression de pages
    // ══════════════════════════════════════════
    @PostMapping("/delete-pages")
    public ResponseEntity<byte[]> deletePages(
            @RequestParam("file") MultipartFile file,
            @RequestParam("pages") int[] pages) {
        try {
            PDFResult result = corba.getPdfService()
                .deletePages(file.getBytes(), pages);
            return pdfResponse(result, "result.pdf");
        } catch (Exception e) {
            log.error("Erreur /delete-pages", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  5. Compression
    // ══════════════════════════════════════════
    @PostMapping("/compress")
    public ResponseEntity<byte[]> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true")  boolean compressImages,
            @RequestParam(defaultValue = "70")    int imageQuality,
            @RequestParam(defaultValue = "false") boolean removeMetadata) {
        try {
            CompressOptions opts = new CompressOptions();
            opts.compressImages  = compressImages;
            opts.imageQuality    = imageQuality;
            opts.removeMetadata  = removeMetadata;

            PDFResult result = corba.getPdfService()
                .compress(file.getBytes(), opts);
            return pdfResponse(result, "compressed.pdf");
        } catch (Exception e) {
            log.error("Erreur /compress", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  6. Rotation
    // ══════════════════════════════════════════
    @PostMapping("/rotate")
    public ResponseEntity<byte[]> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "90") int angle,
            @RequestParam(required = false)    int[] pages) {
        try {
            int[] p = pages != null ? pages : new int[]{};
            PDFResult result = corba.getPdfService()
                .rotate(file.getBytes(), p, angle);
            return pdfResponse(result, "rotated.pdf");
        } catch (Exception e) {
            log.error("Erreur /rotate", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  7. Filigrane
    // ══════════════════════════════════════════
    @PostMapping("/watermark")
    public ResponseEntity<byte[]> watermark(
            @RequestParam("file")                  MultipartFile file,
            @RequestParam                          String text,
            @RequestParam(defaultValue = "0.3")    float opacity,
            @RequestParam(defaultValue = "48")     int fontSize,
            @RequestParam(defaultValue = "true")   boolean diagonal) {
        try {
            WatermarkOptions opts = new WatermarkOptions();
            opts.text     = text;
            opts.opacity  = opacity;
            opts.fontSize = fontSize;
            opts.diagonal = diagonal;

            PDFResult result = corba.getPdfService()
                .addWatermark(file.getBytes(), opts);
            return pdfResponse(result, "watermarked.pdf");
        } catch (Exception e) {
            log.error("Erreur /watermark", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  8. Mot de passe
    // ══════════════════════════════════════════
    @PostMapping("/protect")
    public ResponseEntity<byte[]> protect(
            @RequestParam("file")           MultipartFile file,
            @RequestParam                   String userPassword,
            @RequestParam(required = false) String ownerPassword) {
        try {
            String owner = ownerPassword != null ? ownerPassword : userPassword;
            PDFResult result = corba.getPdfService()
                .addPassword(file.getBytes(), userPassword, owner);
            return pdfResponse(result, "protected.pdf");
        } catch (Exception e) {
            log.error("Erreur /protect", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  9. Conversion PDF → Images
    // ══════════════════════════════════════════
    @PostMapping("/convert-to-images")
    public ResponseEntity<byte[][]> convertToImages(
            @RequestParam("file")               MultipartFile file,
            @RequestParam(defaultValue = "PNG")  String format,
            @RequestParam(defaultValue = "150")  int dpi) {
        try {
            ConvertOptions opts = new ConvertOptions();
            opts.format = format;
            opts.dpi    = dpi;

            byte[][] images = corba.getPdfService()
                .convertToImages(file.getBytes(), opts);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error("Erreur /convert-to-images", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ══════════════════════════════════════════
    //  10. Extraction de texte
    // ══════════════════════════════════════════
    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, String>> extractText(
            @RequestParam("file") MultipartFile file) {
        try {
            String text = corba.getPdfService()
                .extractText(file.getBytes());
            return ResponseEntity.ok(Map.of("text", text));
        } catch (Exception e) {
            log.error("Erreur /extract-text", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════
    //  11. OCR
    // ══════════════════════════════════════════
    @PostMapping("/ocr")
    public ResponseEntity<Map<String, String>> ocr(
            @RequestParam("file")                MultipartFile file,
            @RequestParam(defaultValue = "fra")  String language) {
        try {
            String text = corba.getPdfService()
                .performOCR(file.getBytes(), language);
            return ResponseEntity.ok(Map.of("text", text, "language", language));
        } catch (Exception e) {
            log.error("Erreur /ocr", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════
    //  12. Signature numérique
    // ══════════════════════════════════════════
    @PostMapping("/sign")
    public ResponseEntity<byte[]> sign(
            @RequestParam("file")        MultipartFile file,
            @RequestParam("certificate") MultipartFile certificate,
            @RequestParam               String password,
            @RequestParam(defaultValue = "Signature numérique") String reason,
            @RequestParam(defaultValue = "Sénégal")             String location) {
        try {
            PDFResult result = corba.getPdfService().sign(
                file.getBytes(),
                certificate.getBytes(),
                password, reason, location
            );
            return pdfResponse(result, "signed.pdf");
        } catch (Exception e) {
            log.error("Erreur /sign", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  13. Métadonnées
    // ══════════════════════════════════════════
    @PostMapping("/metadata")
    public ResponseEntity<?> getMetadata(
            @RequestParam("file") MultipartFile file) {
        try {
            PDFMetadata meta = corba.getPdfService()
                .getMetadata(file.getBytes());
            return ResponseEntity.ok(Map.of(
                "title",        meta.title,
                "author",       meta.author,
                "subject",      meta.subject,
                "keywords",     meta.keywords,
                "creator",      meta.creator,
                "producer",     meta.producer,
                "pageCount",    meta.pageCount,
                "creationDate", meta.creationDate
            ));
        } catch (Exception e) {
            log.error("Erreur /metadata", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════
    //  14. Création depuis texte
    // ══════════════════════════════════════════
    @PostMapping("/create")
    public ResponseEntity<byte[]> create(
            @RequestParam           String text,
            @RequestParam(defaultValue = "Document") String title) {
        try {
            PDFResult result = corba.getPdfService()
                .createFromText(text, title);
            return pdfResponse(result, "created.pdf");
        } catch (Exception e) {
            log.error("Erreur /create", e);
            return errorResponse(e);
        }
    }

    // ══════════════════════════════════════════
    //  Nombre de pages
    // ══════════════════════════════════════════
    @PostMapping("/page-count")
    public ResponseEntity<Map<String, Integer>> pageCount(
            @RequestParam("file") MultipartFile file) {
        try {
            int count = corba.getPdfService()
                .getPageCount(file.getBytes());
            return ResponseEntity.ok(Map.of("pageCount", count));
        } catch (Exception e) {
            log.error("Erreur /page-count", e);
            return ResponseEntity.status(500).build();
        }
    }

    // ══════════════════════════════════════════
    //  Helpers privés
    // ══════════════════════════════════════════
    private ResponseEntity<byte[]> pdfResponse(PDFResult result, String filename) {
        if (!result.success) {
            return ResponseEntity.status(500)
                .body(result.message.getBytes());
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(result.data);
    }

    private ResponseEntity<byte[]> errorResponse(Exception e) {
        return ResponseEntity.status(500)
            .body(("Erreur : " + e.getMessage()).getBytes());
    }
}