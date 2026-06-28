package sn.ussein.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger.
 *
 * <p>Expose la documentation interactive via springdoc :</p>
 * <ul>
 *   <li>Swagger UI : {@code /swagger-ui.html} (redirige vers {@code /swagger-ui/index.html})</li>
 *   <li>Schema OpenAPI JSON : {@code /v3/api-docs}</li>
 * </ul>
 *
 * <p>Ces routes sont whitelistees sans authentification dans {@link SecurityConfig}.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Corba PDF Server — API")
                .description("""
                    API REST du serveur PDF open-core (SaaS self-host-first, local-first).
                    Operations PDF (fusion, conversion, securite, analyse, generation),
                    authentification, administration et historique des jobs.
                    Toutes les routes applicatives sont versionnees sous /api/v1.""")
                .version("v1")
                .contact(new Contact()
                    .name("Corba PDF Server")
                    .url("https://github.com/"))
                .license(new License()
                    .name("Open-core")));
    }
}
