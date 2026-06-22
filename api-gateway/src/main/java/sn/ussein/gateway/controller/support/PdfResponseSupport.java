package sn.ussein.gateway.controller.support;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.IdentityResolver;
import sn.ussein.gateway.service.JobStorageService;
import sn.ussein.pdfengine.model.PDFResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Logique transverse partagee par les controleurs PDF : validation d'entree,
 * construction de la reponse (PDF / binaire / ZIP), enregistrement du Job et
 * stockage du binaire. Extrait de l'ancien {@code PDFController} monolithique
 * (etape 2.1) sans changement de comportement.
 */
@Component
public class PdfResponseSupport {

    private static final Logger log = LoggerFactory.getLogger(PdfResponseSupport.class);

    private final JobStorageService storage;
    private final IdentityResolver identityResolver;

    public PdfResponseSupport(JobStorageService storage, IdentityResolver identityResolver) {
        this.storage = storage;
        this.identityResolver = identityResolver;
    }

    // -------------------- Validation --------------------

    public void validatePdfFile(org.springframework.web.multipart.MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " est requis");
        }
    }

    public void validatePositiveList(int[] values, String field) {
        if (values == null || values.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " est requis");
        }
        for (int v : values) {
            if (v <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit contenir uniquement des valeurs > 0");
            }
        }
    }

    public void validateEvenRanges(int[] ranges, String field) {
        validatePositiveList(ranges, field);
        if (ranges.length % 2 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit contenir un nombre pair de valeurs");
        }
    }

    // -------------------- Construction de reponse --------------------

    /**
     * Construit la reponse, enregistre le Job et stocke le binaire dans GridFS.
     * Le header X-Job-Id permet au frontend de retrouver l'operation dans l'historique.
     */
    public ResponseEntity<byte[]> pdfResponse(PDFResult result, String defaultName, String customName,
                                              String operation, String inputFilename,
                                              HttpServletRequest request, long startNanos) {
        if (result == null || !result.success || result.data == null) {
            // Echec metier (PDF corrompu, parametres invalides cote moteur, etc.) :
            // 422 plutot que 500 — c'est une erreur cliente sur le contenu fourni.
            String message = (result != null && result.message != null) ? result.message : "Operation echouee";
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
        return buildAndRecord(result.data, MediaType.APPLICATION_PDF_VALUE,
            resolveFilename(customName, defaultName),
            operation, inputFilename, request, startNanos);
    }

    public ResponseEntity<byte[]> binaryResponse(PDFResult result, String defaultName, String customName,
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

    public ResponseEntity<byte[]> zipResponse(byte[][] entries, String defaultZipName, String customName,
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

    public ResponseEntity<byte[]> buildAndRecord(byte[] data, String contentType, String filename,
                                                 String operation, String inputFilename,
                                                 HttpServletRequest request, long startNanos) {
        Identity identity = identityResolver.current(request);
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000L;

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(contentType));

        // Politique graceful : si le quota est depasse, on delivre quand meme
        // le fichier (le travail du moteur est deja fait) mais on ne le stocke pas.
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

    /**
     * Combine un nom de fichier utilisateur (optionnel) avec le nom par defaut :
     * - Si custom est vide/null : retourne le defaultName tel quel
     * - Sinon : sanitize custom (caracteres interdits retires) + ajoute l'extension
     *   du defaultName si custom n'en a pas deja une
     * - Longueur max 100 caracteres pour eviter les abus
     */
    public static String resolveFilename(String custom, String defaultName) {
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
}
