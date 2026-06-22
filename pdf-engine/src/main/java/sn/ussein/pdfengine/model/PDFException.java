package sn.ussein.pdfengine.model;

/**
 * Exception métier générique du moteur PDF.
 *
 * <p>Remplacement POJO de l'exception IDL {@code PDFException}. Là où la version
 * générée étendait l'exception utilisateur CORBA, celle-ci étend simplement
 * {@link Exception} : aucune dépendance CORBA. Les champs publics {@code code} et
 * {@code message} et les constructeurs reproduisent la struct générée pour ne pas
 * altérer la logique des handlers migrés.</p>
 */
public final class PDFException extends Exception {

    private static final long serialVersionUID = 1L;

    public String code = "";
    public String message = "";

    public PDFException() {
    }

    public PDFException(String _reason, String code, String message) {
        super(_reason);
        this.code = code;
        this.message = message;
    }

    public PDFException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}
