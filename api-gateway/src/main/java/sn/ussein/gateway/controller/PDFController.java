package sn.ussein.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.ussein.gateway.service.CorbaClientService;
import sn.ussein.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/pdf")
public class PDFController {

    private static final Logger log = LoggerFactory.getLogger(PDFController.class);

    private final CorbaClientService corba;

    public PDFController(CorbaClientService corba) {
        this.corba = corba;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        try {
            String response = corba.getPdfService().ping();
            return ResponseEntity.ok(Map.of("status", "OK", "server", response));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serveur CORBA indisponible", e);
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<byte[]> merge(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins 2 fichiers PDF sont requis");
        }
        try {
            byte[][] pdfs = new byte[files.length][];
            for (int i = 0; i < files.length; i++) {
                validatePdfFile(files[i], "files[" + i + "]");
                pdfs[i] = files[i].getBytes();
            }
            return pdfResponse(corba.getPdfService().merge(pdfs), "merged.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (Exception e) {
            log.error("Erreur /merge", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la fusion", e);
        }
    }

    @PostMapping("/split")
    public ResponseEntity<byte[]> split(@RequestParam("file") MultipartFile file, @RequestParam("ranges") int[] ranges) {
        validatePdfFile(file, "file");
        validateEvenRanges(ranges, "ranges");
        try {
            byte[][] parts = corba.getPdfService().split(file.getBytes(), ranges);
            return zipResponse(parts, "split.zip", "part", ".pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /split", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du decoupage", e);
        }
    }

    @PostMapping("/extract-pages")
    public ResponseEntity<byte[]> extractPages(@RequestParam("file") MultipartFile file, @RequestParam("pages") int[] pages) {
        validatePdfFile(file, "file");
        validatePositiveList(pages, "pages");
        try {
            return pdfResponse(corba.getPdfService().extractPages(file.getBytes(), pages), "extracted.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /extract-pages", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'extraction", e);
        }
    }

    @PostMapping("/delete-pages")
    public ResponseEntity<byte[]> deletePages(@RequestParam("file") MultipartFile file, @RequestParam("pages") int[] pages) {
        validatePdfFile(file, "file");
        validatePositiveList(pages, "pages");
        try {
            return pdfResponse(corba.getPdfService().deletePages(file.getBytes(), pages), "result.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /delete-pages", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression", e);
        }
    }

    @PostMapping("/compress")
    public ResponseEntity<byte[]> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean compressImages,
            @RequestParam(defaultValue = "70") int imageQuality,
            @RequestParam(defaultValue = "false") boolean removeMetadata) {
        validatePdfFile(file, "file");
        if (imageQuality < 0 || imageQuality > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageQuality doit etre entre 0 et 100");
        }
        try {
            CompressOptions opts = new CompressOptions();
            opts.compressImages = compressImages;
            opts.imageQuality = imageQuality;
            opts.removeMetadata = removeMetadata;
            return pdfResponse(corba.getPdfService().compress(file.getBytes(), opts), "compressed.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /compress", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la compression", e);
        }
    }

    @PostMapping("/rotate")
    public ResponseEntity<byte[]> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "90") int angle,
            @RequestParam(required = false) int[] pages) {
        validatePdfFile(file, "file");
        if (angle != 90 && angle != 180 && angle != 270) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "angle doit valoir 90, 180 ou 270");
        }
        if (pages != null && pages.length > 0) {
            validatePositiveList(pages, "pages");
        }
        try {
            return pdfResponse(corba.getPdfService().rotate(file.getBytes(), pages != null ? pages : new int[]{}, angle), "rotated.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /rotate", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la rotation", e);
        }
    }

    @PostMapping("/watermark")
    public ResponseEntity<byte[]> watermark(
            @RequestParam("file") MultipartFile file,
            @RequestParam String text,
            @RequestParam(defaultValue = "0.3") float opacity,
            @RequestParam(defaultValue = "48") int fontSize,
            @RequestParam(defaultValue = "true") boolean diagonal) {
        validatePdfFile(file, "file");
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text est requis");
        }
        if (opacity < 0 || opacity > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "opacity doit etre entre 0 et 1");
        }
        if (fontSize <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fontSize doit etre > 0");
        }
        try {
            WatermarkOptions opts = new WatermarkOptions();
            opts.text = text;
            opts.opacity = opacity;
            opts.fontSize = fontSize;
            opts.diagonal = diagonal;
            return pdfResponse(corba.getPdfService().addWatermark(file.getBytes(), opts), "watermarked.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /watermark", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du filigrane", e);
        }
    }

    @PostMapping("/protect")
    public ResponseEntity<byte[]> protect(
            @RequestParam("file") MultipartFile file,
            @RequestParam String userPassword,
            @RequestParam(required = false) String ownerPassword) {
        validatePdfFile(file, "file");
        if (userPassword == null || userPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userPassword est requis");
        }
        try {
            String owner = (ownerPassword == null || ownerPassword.isBlank()) ? userPassword : ownerPassword;
            return pdfResponse(corba.getPdfService().addPassword(file.getBytes(), userPassword, owner), "protected.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /protect", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la protection", e);
        }
    }

    @PostMapping("/convert-to-images")
    public ResponseEntity<byte[]> convertToImages(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PNG") String format,
            @RequestParam(defaultValue = "150") int dpi) {
        validatePdfFile(file, "file");
        if (dpi <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dpi doit etre > 0");
        }
        try {
            ConvertOptions opts = new ConvertOptions();
            opts.format = format;
            opts.dpi = dpi;
            byte[][] images = corba.getPdfService().convertToImages(file.getBytes(), opts);
            String ext = "." + format.toLowerCase();
            return zipResponse(images, "images.zip", "page", ext);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /convert-to-images", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion", e);
        }
    }

    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, String>> extractText(@RequestParam("file") MultipartFile file) {
        validatePdfFile(file, "file");
        try {
            String text = corba.getPdfService().extractText(file.getBytes());
            return ResponseEntity.ok(Map.of("text", text));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /extract-text", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'extraction de texte", e);
        }
    }

    @PostMapping("/ocr")
    public ResponseEntity<Map<String, String>> ocr(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "fra") String language) {
        validatePdfFile(file, "file");
        try {
            String text = corba.getPdfService().performOCR(file.getBytes(), language);
            return ResponseEntity.ok(Map.of("text", text, "language", language));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /ocr", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'OCR", e);
        }
    }

    @PostMapping("/sign")
    public ResponseEntity<byte[]> sign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("certificate") MultipartFile certificate,
            @RequestParam String password,
            @RequestParam(defaultValue = "Signature numérique") String reason,
            @RequestParam(defaultValue = "Sénégal") String location) {
        validatePdfFile(file, "file");
        if (certificate == null || certificate.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "certificate est requis");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password est requis");
        }
        try {
            return pdfResponse(corba.getPdfService().sign(file.getBytes(), certificate.getBytes(), password, reason, location), "signed.pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (Exception e) {
            log.error("Erreur /sign", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la signature", e);
        }
    }

    @PostMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata(@RequestParam("file") MultipartFile file) {
        validatePdfFile(file, "file");
        try {
            PDFMetadata meta = corba.getPdfService().getMetadata(file.getBytes());
            return ResponseEntity.ok(Map.of(
                    "title", meta.title,
                    "author", meta.author,
                    "subject", meta.subject,
                    "keywords", meta.keywords,
                    "creator", meta.creator,
                    "producer", meta.producer,
                    "pageCount", meta.pageCount,
                    "creationDate", meta.creationDate
            ));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /metadata", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la lecture des metadonnees", e);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<byte[]> create(@RequestParam String text, @RequestParam(defaultValue = "Document") String title) {
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text est requis");
        }
        try {
            return pdfResponse(corba.getPdfService().createFromText(text, title), "created.pdf");
        } catch (Exception e) {
            log.error("Erreur /create", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la creation", e);
        }
    }

    @PostMapping("/page-count")
    public ResponseEntity<Map<String, Integer>> pageCount(@RequestParam("file") MultipartFile file) {
        validatePdfFile(file, "file");
        try {
            int count = corba.getPdfService().getPageCount(file.getBytes());
            return ResponseEntity.ok(Map.of("pageCount", count));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /page-count", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du comptage", e);
        }
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(errorBody(status, ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandled(Exception ex) {
        log.error("Erreur non geree", ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(errorBody(status, "Erreur interne"));
    }

    private void validatePdfFile(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " est requis");
        }
    }

    private void validatePositiveList(int[] values, String field) {
        if (values == null || values.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " est requis");
        }
        for (int v : values) {
            if (v <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit contenir uniquement des valeurs > 0");
            }
        }
    }

    private void validateEvenRanges(int[] ranges, String field) {
        validatePositiveList(ranges, field);
        if (ranges.length % 2 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit contenir un nombre pair de valeurs");
        }
    }

    private ResponseEntity<byte[]> pdfResponse(PDFResult result, String filename) {
        if (result == null || !result.success || result.data == null) {
            String message = (result != null && result.message != null) ? result.message : "Operation echouee";
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.data);
    }

    private ResponseEntity<byte[]> zipResponse(byte[][] entries, String zipName, String prefix, String ext) {
        if (entries == null || entries.length == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Aucun resultat genere");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (int i = 0; i < entries.length; i++) {
                byte[] content = entries[i] != null ? entries[i] : new byte[0];
                String name = prefix + "-" + (i + 1) + ext;
                zos.putNextEntry(new ZipEntry(name));
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(baos.toByteArray());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la creation du ZIP", e);
        }
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("message", message == null ? "Erreur" : message);
        body.put("status", status.value());
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
