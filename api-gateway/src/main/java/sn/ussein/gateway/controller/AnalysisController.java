package sn.ussein.gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.ussein.gateway.controller.support.PdfResponseSupport;
import sn.ussein.gateway.web.ApiPaths;
import sn.ussein.pdfengine.PdfEngine;
import sn.ussein.pdfengine.model.PDFMetadata;

import java.io.IOException;
import java.util.Map;

/**
 * Operations de lecture / analyse (sans production de fichier persiste) :
 * extraction de texte, OCR, metadonnees, verification de signature, comparaison,
 * statistiques, nombre de pages.
 */
@Tag(name = "PDF — Analyse",
     description = "Lecture / analyse sans production de fichier : extraction de texte, OCR, "
                 + "metadonnees, verification de signature, comparaison, statistiques, nombre de pages.")
@RestController
@RequestMapping(ApiPaths.PDF)
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final PdfEngine pdfEngine;
    private final PdfResponseSupport support;

    public AnalysisController(PdfEngine pdfEngine, PdfResponseSupport support) {
        this.pdfEngine = pdfEngine;
        this.support = support;
    }

    @PostMapping("/extract-text")
    public ResponseEntity<Map<String, String>> extractText(@RequestParam("file") MultipartFile file) {
        support.validatePdfFile(file, "file");
        try {
            String text = pdfEngine.extractText(file.getBytes());
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
        support.validatePdfFile(file, "file");
        try {
            String text = pdfEngine.performOCR(file.getBytes(), language);
            return ResponseEntity.ok(Map.of("text", text, "language", language));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /ocr", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'OCR", e);
        }
    }

    @PostMapping("/metadata")
    public ResponseEntity<Map<String, Object>> getMetadata(@RequestParam("file") MultipartFile file) {
        support.validatePdfFile(file, "file");
        try {
            PDFMetadata meta = pdfEngine.getMetadata(file.getBytes());
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

    @PostMapping("/verify-signature")
    public ResponseEntity<String> verifySignature(@RequestParam("file") MultipartFile file) {
        support.validatePdfFile(file, "file");
        try {
            String json = pdfEngine.verifySignature(file.getBytes());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /verify-signature", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la verification", e);
        }
    }

    @PostMapping("/compare")
    public ResponseEntity<String> compare(@RequestParam("fileA") MultipartFile fileA,
                                          @RequestParam("fileB") MultipartFile fileB) {
        support.validatePdfFile(fileA, "fileA");
        support.validatePdfFile(fileB, "fileB");
        try {
            String json = pdfEngine.comparePdfs(fileA.getBytes(), fileB.getBytes());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (Exception e) {
            log.error("Erreur /compare", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la comparaison", e);
        }
    }

    @PostMapping("/stats")
    public ResponseEntity<String> stats(@RequestParam("file") MultipartFile file) {
        support.validatePdfFile(file, "file");
        try {
            String json = pdfEngine.getDocumentStats(file.getBytes());
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /stats", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du calcul des stats", e);
        }
    }

    @PostMapping("/page-count")
    public ResponseEntity<Map<String, Integer>> pageCount(@RequestParam("file") MultipartFile file) {
        support.validatePdfFile(file, "file");
        try {
            int count = pdfEngine.getPageCount(file.getBytes());
            return ResponseEntity.ok(Map.of("pageCount", count));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (Exception e) {
            log.error("Erreur /page-count", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du comptage", e);
        }
    }
}
