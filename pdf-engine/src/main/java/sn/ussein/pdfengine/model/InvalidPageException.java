package sn.ussein.pdfengine.model;

/**
 * Exception levée lorsqu'une page demandée est hors de l'intervalle valide.
 *
 * <p>Remplacement POJO de l'exception IDL {@code InvalidPageException} (étend
 * désormais {@link Exception} et non l'exception utilisateur CORBA).
 * Champs publics et constructeurs identiques à la struct générée.</p>
 */
public final class InvalidPageException extends Exception {

    private static final long serialVersionUID = 1L;

    public int requestedPage;
    public int totalPages;
    public String message = "";

    public InvalidPageException() {
    }

    public InvalidPageException(String _reason, int requestedPage, int totalPages, String message) {
        super(_reason);
        this.requestedPage = requestedPage;
        this.totalPages = totalPages;
        this.message = message;
    }

    public InvalidPageException(int requestedPage, int totalPages, String message) {
        super(message);
        this.requestedPage = requestedPage;
        this.totalPages = totalPages;
        this.message = message;
    }
}
