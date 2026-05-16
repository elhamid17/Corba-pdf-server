package sn.ussein.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS — autorise le frontend React (port 5173 en dev,
 * port 80 en production via Nginx) à appeler l'API.
 */
@Configuration
public class CorsConfig {
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("Content-Disposition")
                    .allowCredentials(false)
                    .maxAge(3600);
            }
        };
    }
}
