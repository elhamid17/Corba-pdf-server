package sn.ussein.pdfengine.model;

/**
 * Options de compression PDF.
 *
 * <p>POJO de remplacement de la struct IDL {@code CompressOptions}, sans
 * dépendance CORBA. Champs publics identiques à la struct générée.</p>
 */
public final class CompressOptions {

    public boolean compressImages;
    public int imageQuality;
    public boolean removeMetadata;

    public CompressOptions() {
    }

    public CompressOptions(boolean compressImages, int imageQuality, boolean removeMetadata) {
        this.compressImages = compressImages;
        this.imageQuality = imageQuality;
        this.removeMetadata = removeMetadata;
    }
}
