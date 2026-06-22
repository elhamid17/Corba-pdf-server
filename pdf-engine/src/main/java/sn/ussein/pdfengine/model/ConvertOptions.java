package sn.ussein.pdfengine.model;

/**
 * Options de conversion PDF vers images.
 *
 * <p>POJO de remplacement de la struct IDL {@code ConvertOptions}, sans
 * dépendance CORBA. Champs publics identiques à la struct générée.</p>
 */
public final class ConvertOptions {

    public String format = "";
    public int dpi;

    public ConvertOptions() {
    }

    public ConvertOptions(String format, int dpi) {
        this.format = format;
        this.dpi = dpi;
    }
}
