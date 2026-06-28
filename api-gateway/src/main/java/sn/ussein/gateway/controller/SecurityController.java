package sn.ussein.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

import java.io.IOException;

/**
 * Securite et confidentialite : protection/deverrouillage par mot de passe,
 * signature numerique, apposition de signature image, caviardage, anonymisation.
 */
@Tag(name = "PDF — Securite",
     description = "Securite et confidentialite : protection/deverrouillage par mot de passe, "
                 + "signature numerique, signature image, caviardage, anonymisation.")
@RestController
@RequestMapping(ApiPaths.PDF)
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    private final PdfEngine pdfEngine;
    private final PdfResponseSupport support;

    public SecurityController(PdfEngine pdfEngine, PdfResponseSupport support) {
        this.pdfEngine = pdfEngine;
        this.support = support;
    }

    @PostMapping("/protect")
    public ResponseEntity<byte[]> protect(
            @RequestParam("file") MultipartFile file,
            @RequestParam String userPassword,
            @RequestParam(required = false) String ownerPassword,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (userPassword == null || userPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userPassword est requis");
        }
        long start = System.nanoTime();
        try {
            String owner = (ownerPassword == null || ownerPassword.isBlank()) ? userPassword : ownerPassword;
            return support.pdfResponse(pdfEngine.addPassword(file.getBytes(), userPassword, owner), "protected.pdf", outputName,
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

    @PostMapping("/unlock")
    public ResponseEntity<byte[]> unlock(@RequestParam("file") MultipartFile file,
                                         @RequestParam String password,
                                         @RequestParam(required = false) String outputName,
                                         HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (password == null) password = "";
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.unlockPdf(file.getBytes(), password),
                "unlocked.pdf", outputName, "unlock", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /unlock", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du deverrouillage", e);
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
        support.validatePdfFile(file, "file");
        if (certificate == null || certificate.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "certificate est requis");
        }
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password est requis");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.sign(file.getBytes(), certificate.getBytes(), password, reason, location), "signed.pdf", outputName,
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

    @PostMapping("/sign-image")
    public ResponseEntity<byte[]> signImage(@RequestParam("file") MultipartFile file,
                                            @RequestParam("signature") MultipartFile signature,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "50") float xPercent,
                                            @RequestParam(defaultValue = "10") float yPercent,
                                            @RequestParam(defaultValue = "30") float widthPercent,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (signature == null || signature.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature (image) est requise");
        }
        String ct = signature.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "signature doit etre une image (PNG/JPG)");
        }
        if (page < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page doit etre >= 1");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.addSignatureImage(file.getBytes(),
                    signature.getBytes(), page, xPercent, yPercent, widthPercent),
                "signed-image.pdf", outputName, "sign-image", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /sign-image", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'apposition de signature", e);
        }
    }

    @PostMapping("/redact")
    public ResponseEntity<byte[]> redact(@RequestParam("file") MultipartFile file,
                                         @RequestParam("terms") String[] terms,
                                         @RequestParam(required = false) String outputName,
                                         HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (terms == null || terms.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un terme a caviarder est requis");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.redactText(file.getBytes(), terms),
                "redacted.pdf", outputName, "redact", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /redact", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du caviardage", e);
        }
    }

    @PostMapping("/anonymize")
    public ResponseEntity<byte[]> anonymize(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.anonymize(file.getBytes()),
                "anonymized.pdf", outputName, "anonymize", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /anonymize", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'anonymisation", e);
        }
    }
}
