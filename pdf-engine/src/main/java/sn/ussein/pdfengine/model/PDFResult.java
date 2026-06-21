package sn.ussein.pdfengine.model;

/**
 * Résultat standard d'une opération PDF.
 *
 * <p>POJO de remplacement de la struct IDL {@code PDFResult}, sans aucune
 * dépendance CORBA/JacORB. Les champs publics ({@code success}, {@code data},
 * {@code message}) sont volontairement identiques à la struct générée afin que
 * la logique métier des handlers migrés reste inchangée.</p>
 */
public final class PDFResult {

    public boolean success;
    public byte[] data;
    public String message = "";

    public PDFResult() {
    }

    public PDFResult(boolean success, byte[] data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }
}
