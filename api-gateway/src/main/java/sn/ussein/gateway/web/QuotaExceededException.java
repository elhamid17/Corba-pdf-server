package sn.ussein.gateway.web;

/**
 * Quota d'espace ou de fichiers depasse.
 * Renvoyee en 413 (Payload Too Large) par GlobalExceptionHandler.
 */
public class QuotaExceededException extends RuntimeException {
    public QuotaExceededException(String message) {
        super(message);
    }
}
