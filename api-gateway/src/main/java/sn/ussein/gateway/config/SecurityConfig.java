package sn.ussein.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import sn.ussein.gateway.security.GuestCookieFilter;
import sn.ussein.gateway.security.JwtAuthenticationFilter;
import sn.ussein.gateway.security.RateLimitFilter;
import sn.ussein.gateway.web.ApiPaths;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * CSP prudente : autorise le rendu de Swagger UI (scripts/styles inline générés
     * par springdoc) sans relâcher la protection. Les réponses de l'API sont du JSON/
     * binaire (CSP sans effet) ; le frontend React est servi par nginx (origine
     * distincte) et n'est donc pas impacté par cet en-tête côté backend.
     * {@code frame-ancestors 'none'} double X-Frame-Options: DENY (anti-clickjacking).
     */
    private static final String CSP_POLICY =
        "default-src 'self'; "
        + "script-src 'self' 'unsafe-inline'; "
        + "style-src 'self' 'unsafe-inline'; "
        + "img-src 'self' data:; "
        + "font-src 'self'; "
        + "connect-src 'self'; "
        + "frame-ancestors 'none'; "
        + "base-uri 'self'; "
        + "form-action 'self'; "
        + "object-src 'none'";

    private final JwtAuthenticationFilter jwtFilter;
    private final GuestCookieFilter guestFilter;
    private final RateLimitFilter rateLimitFilter;
    private final boolean prodProfile;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          GuestCookieFilter guestFilter,
                          RateLimitFilter rateLimitFilter,
                          Environment environment) {
        this.jwtFilter = jwtFilter;
        this.guestFilter = guestFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.prodProfile = environment.acceptsProfiles(Profiles.of("prod"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Headers de securite : durcissement contre clickjacking, MIME-sniff,
            // referrer leak, et utilisation d'API navigateurs sensibles.
            .headers(headers -> {
                headers.frameOptions(f -> f.deny());                    // X-Frame-Options: DENY
                headers.contentTypeOptions(Customizer.withDefaults());  // X-Content-Type-Options: nosniff
                headers.referrerPolicy(r -> r.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                headers.permissionsPolicy(p -> p.policy(
                    "geolocation=(), microphone=(), camera=(), payment=()"));
                headers.contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY));
                // HSTS uniquement en prod (HTTPS) : inutile/contre-productif en dev HTTP local.
                if (prodProfile) {
                    headers.httpStrictTransportSecurity(h -> h
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000));                    // HSTS 1 an
                } else {
                    headers.httpStrictTransportSecurity(h -> h.disable());
                }
            })
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics
                .requestMatchers(ApiPaths.AUTH + "/register", ApiPaths.AUTH + "/login").permitAll()
                .requestMatchers(ApiPaths.PDF + "/ping").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Documentation OpenAPI / Swagger UI : accessible sans authentification
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**",
                                 "/swagger-ui.html", "/swagger-ui/**").permitAll()
                // PDF : accessible aux guests ET aux users (le controleur differenciera)
                .requestMatchers(ApiPaths.PDF + "/**").permitAll()
                // Admin reserve aux ADMIN
                .requestMatchers(ApiPaths.ADMIN + "/**").hasRole("ADMIN")
                // Jobs : accessible aux guests (cookie) ET aux users — le controleur differencie
                .requestMatchers(ApiPaths.JOBS + "/**").permitAll()
                // /api/v1/auth/me et tout le reste : authentification requise
                .anyRequest().authenticated())
            // Ordre important : jwtFilter doit etre enregistre AVANT
            // qu'on puisse positionner guestFilter par rapport a lui.
            // rateLimitFilter doit etre tout en tete pour rejeter avant tout traitement.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(guestFilter, JwtAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, GuestCookieFilter.class);

        return http.build();
    }
}
