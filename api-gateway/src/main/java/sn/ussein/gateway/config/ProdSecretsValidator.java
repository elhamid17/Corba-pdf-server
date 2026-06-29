package sn.ussein.gateway.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail-fast de sécurité en profil prod : refuse le démarrage si des secrets
 * sensibles sont absents ou laissés à leur valeur par défaut de développement.
 *
 * <p>Ne s'active QUE sous le profil {@code prod} (SPRING_PROFILES_ACTIVE=prod).
 * Le mode dev/local (docker compose) démarre sans configuration secrète lourde
 * — cf. ADR durcissement sécurité 2.3 dans {@code docs/DECISIONS.md}.</p>
 */
@Component
@Profile("prod")
public class ProdSecretsValidator {

    private static final Logger log = LoggerFactory.getLogger(ProdSecretsValidator.class);

    /** Valeurs par défaut (dev) qui NE DOIVENT JAMAIS servir en prod. */
    static final String DEFAULT_JWT_SECRET =
        "change-me-in-production-please-use-a-long-random-string";
    static final String DEFAULT_ADMIN_PASSWORD = "admin123";
    private static final int MIN_JWT_SECRET_LENGTH = 32;

    private final JwtProperties jwt;
    private final AdminBootstrapProperties admin;

    public ProdSecretsValidator(JwtProperties jwt, AdminBootstrapProperties admin) {
        this.jwt = jwt;
        this.admin = admin;
    }

    @PostConstruct
    void validate() {
        String secret = jwt.getSecret();
        if (secret == null || secret.isBlank()) {
            throw fail("JWT_SECRET est absent. Définissez une chaîne aléatoire >= "
                + MIN_JWT_SECRET_LENGTH + " caractères.");
        }
        if (DEFAULT_JWT_SECRET.equals(secret)) {
            throw fail("JWT_SECRET est laissé à sa valeur par défaut de dev. "
                + "Générez un secret aléatoire (ex. openssl rand -base64 48).");
        }
        if (secret.length() < MIN_JWT_SECRET_LENGTH) {
            throw fail("JWT_SECRET trop court (" + secret.length()
                + " < " + MIN_JWT_SECRET_LENGTH + " caractères).");
        }

        String pwd = admin.getPassword();
        if (pwd == null || pwd.isBlank()) {
            throw fail("ADMIN_PASSWORD est absent. Définissez un mot de passe admin fort.");
        }
        if (DEFAULT_ADMIN_PASSWORD.equals(pwd)) {
            throw fail("ADMIN_PASSWORD est laissé à sa valeur par défaut 'admin123'. Changez-le.");
        }

        log.info("Validation des secrets prod : OK (JWT_SECRET et ADMIN_PASSWORD personnalisés).");
    }

    private IllegalStateException fail(String detail) {
        return new IllegalStateException(
            "Démarrage refusé (profil prod) — " + detail
            + " Voir docs/DECISIONS.md (ADR durcissement sécurité 2.3).");
    }
}
