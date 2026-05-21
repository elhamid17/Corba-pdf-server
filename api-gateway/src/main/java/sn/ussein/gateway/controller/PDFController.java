package sn.ussein.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.IdentityResolver;
import sn.ussein.gateway.service.CorbaClientService;
import sn.ussein.gateway.service.JobStorageService;
import sn.ussein.pdf.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/pdf")
public class PDFController {

    private static final Logger log = LoggerFactory.getLogger(PDFController.class);

    private final CorbaClientService corba;
    private final JobStorageService storage;
    private final IdentityResolver identityResolver;

    public PDFController(CorbaClientService corba,
                         JobStorageService storage,
                         IdentityResolver identityResolver) {
        this.corba = corba;
        this.storage = storage;
        this.identityResolver = identityResolver;
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
    public ResponseEntity<byte[]> merge(@RequestParam("files") MultipartFile[] files,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        if (files == null || files.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins 2 fichiers PDF sont requis");
        }
        long start = System.nanoTime();
        try {
            byte[][] pdfs = new byte[files.length][];
            for (int i = 0; i < files.length; i++) {
                validatePdfFile(files[i], "files[" + i + "]");
                pdfs[i] = files[i].getBytes();
            }
            return pdfResponse(corba.getPdfService().merge(pdfs), "merged.pdf", outputName,
                "merge", files[0].getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /merge", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la fusion", e);
        }
    }

    @PostMapping("/split")
    public ResponseEntity<byte[]> split(@RequestParam("file") MultipartFile file,
                                        @RequestParam("ranges") int[] ranges,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        validatePdfFile(file, "file");
        validateEvenRanges(ranges, "ranges");
        long start = System.nanoTime();
        try {
            byte[][] parts = corba.getPdfService().split(file.getBytes(), ranges);
            return zipResponse(parts, "split.zip", outputName, "part", ".pdf",
                "split", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /split", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du decoupage", e);
        }
    }

    @PostMapping("/extract-pages")
    public ResponseEntity<byte[]> extractPages(@RequestParam("file") MultipartFile file,
                                               @RequestParam("pages") int[] pages,
                                               @RequestParam(required = false) String outputName,
                                               HttpServletRequest request) {
        validatePdfFile(file, "file");
        validatePositiveList(pages, "pages");
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().extractPages(file.getBytes(), pages), "extracted.pdf", outputName,
                "extract-pages", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /extract-pages", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'extraction", e);
        }
    }

    @PostMapping("/delete-pages")
    public ResponseEntity<byte[]> deletePages(@RequestParam("file") MultipartFile file,
                                              @RequestParam("pages") int[] pages,
                                              @RequestParam(required = false) String outputName,
                                              HttpServletRequest request) {
        validatePdfFile(file, "file");
        validatePositiveList(pages, "pages");
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().deletePages(file.getBytes(), pages), "result.pdf", outputName,
                "delete-pages", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
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
            @RequestParam(defaultValue = "false") boolean removeMetadata,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        validatePdfFile(file, "file");
        if (imageQuality < 0 || imageQuality > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageQuality doit etre entre 0 et 100");
        }
        long start = System.nanoTime();
        try {
            CompressOptions opts = new CompressOptions();
            opts.compressImages = compressImages;
            opts.imageQuality = imageQuality;
            opts.removeMetadata = removeMetadata;
            return pdfResponse(corba.getPdfService().compress(file.getBytes(), opts), "compressed.pdf", outputName,
                "compress", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /compress", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la compression", e);
        }
    }

    @PostMapping("/rotate")
    public ResponseEntity<byte[]> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "90") int angle,
            @RequestParam(required = false) int[] pages,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        validatePdfFile(file, "file");
        if (angle != 90 && angle != 180 && angle != 270) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "angle doit valoir 90, 180 ou 270");
        }
        if (pages != null && pages.length > 0) {
            validatePositiveList(pages, "pages");
        }
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().rotate(file.getBytes(),
                pages != null ? pages : new int[]{}, angle), "rotated.pdf", outputName,
                "rotate", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
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
            @RequestParam(defaultValue = "true") boolean diagonal,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
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
        long start = System.nanoTime();
        try {
            WatermarkOptions opts = new WatermarkOptions();
            opts.text = text;
            opts.opacity = opacity;
            opts.fontSize = fontSize;
            opts.diagonal = diagonal;
            return pdfResponse(corba.getPdfService().addWatermark(file.getBytes(), opts), "watermarked.pdf", outputName,
                "watermark", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /watermark", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du filigrane", e);
        }
    }

    @PostMapping("/protect")
    public ResponseEntity<byte[]> protect(
            @RequestParam("file") MultipartFile file,
            @RequestParam String userPassword,
            @RequestParam(required = false) String ownerPassword,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        validatePdfFile(file, "file");
        if (userPassword == null || userPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userPassword est requis");
        }
        long start = System.nanoTime();
        try {
            String owner = (ownerPassword == null || ownerPassword.isBlank()) ? userPassword : ownerPassword;
            return pdfResponse(corba.getPdfService().addPassword(file.getBytes(), userPassword, owner), "protected.pdf", outputName,
                "protect", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /protect", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la protection", e);
        }
    }

    @PostMapping("/convert-to-images")
    public ResponseEntity<byte[]> convertToImages(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PNG") String format,
            @RequestParam(defaultValue = "150") int dpi,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        validatePdfFile(file, "file");
        if (dpi <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dpi doit etre > 0");
        }
        long start = System.nanoTime();
        try {
            ConvertOptions opts = new ConvertOptions();
            opts.format = format;
            opts.dpi = dpi;
            byte[][] images = corba.getPdfService().convertToImages(file.getBytes(), opts);
            String ext = "." + format.toLowerCase();
            return zipResponse(images, "images.zip", outputName, "page", ext,
                "convert-to-images", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
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
            @RequestParam(defaultValue = "Sénégal") String location,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        validatePdfFile(file, "file");
        if (certificate == null || certificate.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "certificate est requis");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password est requis");
        }
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().sign(file.getBytes(), certificate.getBytes(), password, reason, location), "signed.pdf", outputName,
                "sign", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
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
    public ResponseEntity<byte[]> create(@RequestParam String text,
                                         @RequestParam(defaultValue = "Document") String title,
                                         @RequestParam(required = false) String outputName,
                                         HttpServletRequest request) {
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text est requis");
        }
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().createFromText(text, title), "created.pdf", outputName,
                "create", title, request, start);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /create", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la creation", e);
        }
    }

    @PostMapping("/pdf-to-word")
    public ResponseEntity<byte[]> pdfToWord(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            PDFResult r = corba.getPdfService().pdfToWord(file.getBytes());
            return binaryResponse(r, "converted.docx", outputName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "pdf-to-word", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /pdf-to-word", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion PDF -> Word", e);
        }
    }

    @PostMapping("/pdf-to-excel")
    public ResponseEntity<byte[]> pdfToExcel(@RequestParam("file") MultipartFile file,
                                             @RequestParam(required = false) String outputName,
                                             HttpServletRequest request) {
        validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            PDFResult r = corba.getPdfService().pdfToExcel(file.getBytes());
            return binaryResponse(r, "converted.xlsx", outputName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "pdf-to-excel", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /pdf-to-excel", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion PDF -> Excel", e);
        }
    }

    @PostMapping("/word-to-pdf")
    public ResponseEntity<byte[]> wordToPdf(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file est requis");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".docx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier doit avoir l'extension .docx");
        }
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().wordToPdf(file.getBytes()), "converted.pdf", outputName,
                "word-to-pdf", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /word-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion Word -> PDF", e);
        }
    }

    @PostMapping("/images-to-pdf")
    public ResponseEntity<byte[]> imagesToPdf(@RequestParam("files") MultipartFile[] files,
                                              @RequestParam(required = false) String outputName,
                                              HttpServletRequest request) {
        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins une image est requise");
        }
        long start = System.nanoTime();
        try {
            byte[][] images = new byte[files.length][];
            for (int i = 0; i < files.length; i++) {
                MultipartFile f = files[i];
                if (f == null || f.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "files[" + i + "] est vide");
                }
                String ct = f.getContentType();
                if (ct == null || !ct.startsWith("image/")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "files[" + i + "] doit etre une image (JPG/PNG)");
                }
                images[i] = f.getBytes();
            }
            return pdfResponse(corba.getPdfService().imagesToPdf(images), "images.pdf", outputName,
                "images-to-pdf", files[0].getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /images-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion Images -> PDF", e);
        }
    }

    @PostMapping("/reverse")
    public ResponseEntity<byte[]> reverse(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String outputName,
                                          HttpServletRequest request) {
        validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().reversePages(file.getBytes()), "reversed.pdf", outputName,
                "reverse", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /reverse", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'inversion", e);
        }
    }

    @PostMapping("/page-numbers")
    public ResponseEntity<byte[]> pageNumbers(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "bottom-center") String position,
                                              @RequestParam(defaultValue = "%d") String format,
                                              @RequestParam(defaultValue = "1") int startNumber,
                                              @RequestParam(defaultValue = "12") int fontSize,
                                              @RequestParam(required = false) String outputName,
                                              HttpServletRequest request) {
        validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return pdfResponse(
                corba.getPdfService().addPageNumbers(file.getBytes(), position, format, startNumber, fontSize),
                "numbered.pdf", outputName, "page-numbers", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /page-numbers", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la numerotation", e);
        }
    }

    @PostMapping("/resize")
    public ResponseEntity<byte[]> resize(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "A4") String targetSize,
                                         @RequestParam(required = false) String outputName,
                                         HttpServletRequest request) {
        validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().resizePages(file.getBytes(), targetSize),
                "resized.pdf", outputName, "resize", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /resize", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du redimensionnement", e);
        }
    }

    @PostMapping("/crop")
    public ResponseEntity<byte[]> crop(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "0") float marginLeft,
                                       @RequestParam(defaultValue = "0") float marginTop,
                                       @RequestParam(defaultValue = "0") float marginRight,
                                       @RequestParam(defaultValue = "0") float marginBottom,
                                       @RequestParam(required = false) String outputName,
                                       HttpServletRequest request) {
        validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().cropPages(file.getBytes(),
                    marginLeft, marginTop, marginRight, marginBottom),
                "cropped.pdf", outputName, "crop", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /crop", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du recadrage", e);
        }
    }

    @PostMapping("/cover")
    public ResponseEntity<byte[]> cover(@RequestParam("file") MultipartFile file,
                                        @RequestParam("cover") MultipartFile cover,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        validatePdfFile(file, "file");
        if (cover == null || cover.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cover (image) est requis");
        }
        String ct = cover.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cover doit etre une image (JPG/PNG)");
        }
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().addCoverPage(file.getBytes(), cover.getBytes()),
                "with-cover.pdf", outputName, "cover", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /cover", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'ajout de la couverture", e);
        }
    }

    @PostMapping("/reorder")
    public ResponseEntity<byte[]> reorder(@RequestParam("file") MultipartFile file,
                                          @RequestParam("order") int[] order,
                                          @RequestParam(required = false) String outputName,
                                          HttpServletRequest request) {
        validatePdfFile(file, "file");
        validatePositiveList(order, "order");
        long start = System.nanoTime();
        try {
            return pdfResponse(corba.getPdfService().reorderPages(file.getBytes(), order),
                "reordered.pdf", outputName, "reorder", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /reorder", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la reorganisation", e);
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

    // Les handlers d'exceptions sont centralises dans GlobalExceptionHandler
    // (@RestControllerAdvice) pour s'appliquer a tous les controleurs et eviter
    // d'eclipser AuthException / QuotaExceededException.

    private void validatePdfFile(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " est requis");
        }
    }

    /**
     * Combine un nom de fichier utilisateur (optionnel) avec le nom par defaut :
     * - Si custom est vide/null : retourne le defaultName tel quel
     * - Sinon : sanitize custom (caracteres interdits retires) + ajoute l'extension
     *   du defaultName si custom n'en a pas deja une
     * - Longueur max 100 caracteres pour eviter les abus
     */
    private static String resolveFilename(String custom, String defaultName) {
        if (custom == null || custom.isBlank()) return defaultName;

        // Retire chemin (./../), caracteres interdits FS et controle chars
        String sanitized = custom.trim()
            .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "")
            .replaceAll("\\.{2,}", ".");
        if (sanitized.isBlank()) return defaultName;

        // Determine l'extension cible depuis defaultName
        int dotDefault = defaultName.lastIndexOf('.');
        String targetExt = dotDefault > 0 ? defaultName.substring(dotDefault) : "";

        // Si le custom n'a pas la meme extension, on l'ajoute / la remplace
        String customExt = "";
        int dotCustom = sanitized.lastIndexOf('.');
        if (dotCustom > 0) customExt = sanitized.substring(dotCustom);

        String base = customExt.isEmpty() ? sanitized
            : (customExt.equalsIgnoreCase(targetExt) ? sanitized.substring(0, dotCustom) : sanitized);

        // Tronque a 100 chars (extension comprise)
        int maxBase = Math.max(1, 100 - targetExt.length());
        if (base.length() > maxBase) base = base.substring(0, maxBase);

        return base + targetExt;
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

    /**
     * Construit la reponse, enregistre le Job et stocke le binaire dans GridFS.
     * Le header X-Job-Id permet au frontend de retrouver l'operation dans l'historique.
     */
    private ResponseEntity<byte[]> pdfResponse(PDFResult result, String defaultName, String customName,
                                               String operation, String inputFilename,
                                               HttpServletRequest request, long startNanos) {
        if (result == null || !result.success || result.data == null) {
            // Echec metier (PDF corrompu, parametres invalides cote CORBA, etc.) :
            // 422 plutot que 500 — c'est une erreur cliente sur le contenu fourni.
            String message = (result != null && result.message != null) ? result.message : "Operation echouee";
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return buildAndRecord(result.data, MediaType.APPLICATION_PDF_VALUE,
            resolveFilename(customName, defaultName),
            operation, inputFilename, request, startNanos);
    }

    private ResponseEntity<byte[]> binaryResponse(PDFResult result, String defaultName, String customName,
                                                  String contentType,
                                                  String operation, String inputFilename,
                                                  HttpServletRequest request, long startNanos) {
        if (result == null || !result.success || result.data == null) {
            String message = (result != null && result.message != null) ? result.message : "Operation echouee";
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return buildAndRecord(result.data, contentType,
            resolveFilename(customName, defaultName),
            operation, inputFilename, request, startNanos);
    }

    private ResponseEntity<byte[]> zipResponse(byte[][] entries, String defaultZipName, String customName,
                                               String prefix, String ext,
                                               String operation, String inputFilename,
                                               HttpServletRequest request, long startNanos) {
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
            return buildAndRecord(baos.toByteArray(), "application/zip",
                resolveFilename(customName, defaultZipName),
                operation, inputFilename, request, startNanos);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la creation du ZIP", e);
        }
    }

    private ResponseEntity<byte[]> buildAndRecord(byte[] data, String contentType, String filename,
                                                  String operation, String inputFilename,
                                                  HttpServletRequest request, long startNanos) {
        Identity identity = identityResolver.current(request);
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(contentType));

        // Politique graceful : si le quota est depasse, on delivre quand meme
        // le fichier (le travail CORBA est deja fait) mais on ne le stocke pas.
        // Le frontend lit X-Quota-Exceeded pour afficher un avertissement.
        try {
            storage.checkQuota(identity, data.length);
            Job job = storage.recordSuccess(identity, operation, inputFilename, filename,
                contentType, data, durationMs);
            builder.header("X-Job-Id", job.getId());
        } catch (sn.ussein.gateway.web.QuotaExceededException qe) {
            log.warn("Job '{}' non persiste (quota) : {}", operation, qe.getMessage());
            builder.header("X-Quota-Exceeded", qe.getMessage());
        } catch (Exception e) {
            log.warn("Persistance du job '{}' echouee : {}", operation, e.getMessage());
        }

        return builder.body(data);
    }
}
