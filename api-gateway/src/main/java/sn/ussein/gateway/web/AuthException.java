package sn.ussein.gateway.web;

/**
 * Erreur metier d'authentification (email pris, mauvais mot de passe, etc.).
 * Renvoyee en 400 par GlobalExceptionHandler.
 */
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
