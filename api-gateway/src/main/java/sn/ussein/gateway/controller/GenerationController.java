package sn.ussein.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.ussein.gateway.controller.support.PdfResponseSupport;
import sn.ussein.pdfengine.PdfEngine;
import sn.ussein.pdfengine.model.WatermarkOptions;

import java.io.IOException;

/**
 * Creation de documents et ajout d'elements graphiques : creation depuis texte,
 * generation de CV, filigrane, couverture, tampon, QR code, code-barres.
 */
@RestController
@RequestMapping("/api/pdf")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);

    private final PdfEngine pdfEngine;
    private final PdfResponseSupport support;

    public GenerationController(PdfEngine pdfEngine, PdfResponseSupport support) {
        this.pdfEngine = pdfEngine;
        this.support = support;
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
            return support.pdfResponse(pdfEngine.createFromText(text, title), "created.pdf", outputName,
                "create", title, request, start);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /create", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la creation", e);
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
        support.validatePdfFile(file, "file");
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
            return support.pdfResponse(pdfEngine.addWatermark(file.getBytes(), opts), "watermarked.pdf", outputName,
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

    @PostMapping("/cover")
    public ResponseEntity<byte[]> cover(@RequestParam("file") MultipartFile file,
                                        @RequestParam("cover") MultipartFile cover,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (cover == null || cover.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cover (image) est requis");
        }
        String ct = cover.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cover doit etre une image (JPG/PNG)");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.addCoverPage(file.getBytes(), cover.getBytes()),
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

    @PostMapping("/stamp")
    public ResponseEntity<byte[]> stamp(@RequestParam("file") MultipartFile file,
                                        @RequestParam String text,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "center") String position,
                                        @RequestParam(defaultValue = "red") String color,
                                        @RequestParam(defaultValue = "36") int fontSize,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text est requis");
        }
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page doit etre >= 1");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.addStamp(file.getBytes(), text, page, position, color, fontSize),
                "stamped.pdf", outputName, "stamp", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /stamp", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'apposition du tampon", e);
        }
    }

    @PostMapping("/add-qr")
    public ResponseEntity<byte[]> addQr(@RequestParam("file") MultipartFile file,
                                        @RequestParam String text,
                                        @RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "bottom-right") String position,
                                        @RequestParam(defaultValue = "120") int sizePx,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text est requis");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.addQrCode(file.getBytes(), text, page, position, sizePx),
                "with-qr.pdf", outputName, "add-qr", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /add-qr", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'ajout du QR code", e);
        }
    }

    @PostMapping("/add-barcode")
    public ResponseEntity<byte[]> addBarcode(@RequestParam("file") MultipartFile file,
                                             @RequestParam String code,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "bottom-right") String position,
                                             @RequestParam(defaultValue = "CODE_128") String type,
                                             @RequestParam(defaultValue = "200") int sizePx,
                                             @RequestParam(required = false) String outputName,
                                             HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code est requis");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.addBarcode(file.getBytes(), code, page, position, type, sizePx),
                "with-barcode.pdf", outputName, "add-barcode", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /add-barcode", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'ajout du code-barres", e);
        }
    }

    @PostMapping("/generate-cv")
    public ResponseEntity<byte[]> generateCv(@RequestBody String cvJson,
                                             @RequestParam(required = false) String outputName,
                                             HttpServletRequest request) {
        if (cvJson == null || cvJson.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Donnees CV vides");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.generateCv(cvJson),
                "cv.pdf", outputName, "generate-cv", "CV", request, start);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /generate-cv", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la generation du CV", e);
        }
    }
}
