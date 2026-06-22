package sn.ussein.pdfengine.model;

/**
 * Options de filigrane.
 *
 * <p>POJO de remplacement de la struct IDL {@code WatermarkOptions}, sans
 * dépendance CORBA. Champs publics identiques à la struct générée.</p>
 */
public final class WatermarkOptions {

    public String text = "";
    public float opacity;
    public int fontSize;
    public boolean diagonal;

    public WatermarkOptions() {
    }

    public WatermarkOptions(String text, float opacity, int fontSize, boolean diagonal) {
        this.text = text;
        this.opacity = opacity;
        this.fontSize = fontSize;
        this.diagonal = diagonal;
    }
}
