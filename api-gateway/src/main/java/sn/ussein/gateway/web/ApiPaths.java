package sn.ussein.gateway.web;

/**
 * Source unique des prefixes d'URL de l'API.
 *
 * <p>Versioning d'API : toutes les routes applicatives sont servies sous {@code /api/v1}
 * (cf. ADR-0007). Ces constantes sont des expressions constantes a la compilation
 * (concatenation de {@code static final String}), donc utilisables directement dans les
 * annotations {@code @RequestMapping}.</p>
 *
 * <p>Ne JAMAIS dupliquer ces chaines ailleurs : controleurs, filtres de securite et
 * configuration doivent referencer ces constantes.</p>
 */
public final class ApiPaths {

    private ApiPaths() {
        // Classe utilitaire : pas d'instanciation.
    }

    /** Racine de l'API (non versionnee). */
    public static final String API = "/api";

    /** Prefixe de version courant. */
    public static final String V1 = API + "/v1";

    /** Operations PDF (fusion, conversion, securite, analyse, generation, ping). */
    public static final String PDF = V1 + "/pdf";

    /** Authentification (register, login, me). */
    public static final String AUTH = V1 + "/auth";

    /** Administration (reserve au role ADMIN). */
    public static final String ADMIN = V1 + "/admin";

    /** Historique des jobs. */
    public static final String JOBS = V1 + "/jobs";
}
