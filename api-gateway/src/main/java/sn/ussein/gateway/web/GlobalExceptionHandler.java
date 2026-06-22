package sn.ussein.gateway.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import sn.ussein.pdfengine.model.InvalidPageException;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PasswordException;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(AuthException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(QuotaExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(err ->
            fields.put(err.getField(), err.getDefaultMessage()));
        Map<String, Object> b = base(HttpStatus.BAD_REQUEST, "Donnees invalides");
        b.put("fields", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON).body(b);
    }

    /**
     * Mappe une ResponseStatusException (levee par les controleurs PDF, etc.)
     * vers une reponse JSON uniforme.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        return body(status, e.getReason());
    }

    @ExceptionHandler(PDFException.class)
    public ResponseEntity<Map<String, Object>> handlePdfException(PDFException e) {
        String message = e.message != null ? e.message : "Erreur PDF";
        if (e.code != null && !e.code.isBlank()) {
            message = e.code + " : " + message;
        }
        return body(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }

    @ExceptionHandler(InvalidPageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPage(InvalidPageException e) {
        String message = e.message != null ? e.message
            : "Page " + e.requestedPage + " invalide (total : " + e.totalPages + ")";
        return body(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<Map<String, Object>> handlePassword(PasswordException e) {
        String message = e.message != null ? e.message : "Mot de passe incorrect ou PDF protege";
        return body(HttpStatus.BAD_REQUEST, message);
    }

    /** Parametre/partie de requete manquant ou de type invalide → 400 (erreur client). */
    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** Filet de securite pour les exceptions non prevues. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandled(Exception e) {
        log.error("Erreur non geree", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(base(status, message));
    }

    private Map<String, Object> base(HttpStatus status, String message) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("error", status.getReasonPhrase());
        b.put("message", message == null ? "Erreur" : message);
        b.put("status", status.value());
        b.put("timestamp", Instant.now().toString());
        return b;
    }
}
