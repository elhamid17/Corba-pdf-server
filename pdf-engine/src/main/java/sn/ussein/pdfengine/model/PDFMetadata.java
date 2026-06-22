package sn.ussein.pdfengine.model;

/**
 * Métadonnées d'un document PDF.
 *
 * <p>POJO de remplacement de la struct IDL {@code PDFMetadata}, sans dépendance
 * CORBA. Les champs publics sont identiques à la struct générée pour préserver
 * la logique des handlers.</p>
 */
public final class PDFMetadata {

    public String title = "";
    public String author = "";
    public String subject = "";
    public String keywords = "";
    public String creator = "";
    public String producer = "";
    public String creationDate = "";
    public int pageCount;

    public PDFMetadata() {
    }

    public PDFMetadata(String title, String author, String subject, String keywords,
                       String creator, String producer, String creationDate, int pageCount) {
        this.title = title;
        this.author = author;
        this.subject = subject;
        this.keywords = keywords;
        this.creator = creator;
        this.producer = producer;
        this.creationDate = creationDate;
        this.pageCount = pageCount;
    }
}
