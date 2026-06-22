package sn.ussein.pdfengine.model;

/**
 * Exception liée à la protection par mot de passe d'un PDF.
 *
 * <p>Remplacement POJO de l'exception IDL {@code PasswordException} (étend
 * désormais {@link Exception} et non l'exception utilisateur CORBA).
 * Champ public {@code message} et constructeurs identiques à la struct générée.</p>
 */
public final class PasswordException extends Exception {

    private static final long serialVersionUID = 1L;

    public String message = "";

    public PasswordException() {
    }

    public PasswordException(String _reason, String message) {
        super(_reason);
        this.message = message;
    }

    public PasswordException(String message) {
        super(message);
        this.message = message;
    }
}
