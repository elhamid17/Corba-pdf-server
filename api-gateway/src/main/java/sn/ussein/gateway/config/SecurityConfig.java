package sn.ussein.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final GuestCookieFilter guestFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          GuestCookieFilter guestFilter,
                          RateLimitFilter rateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.guestFilter = guestFilter;
        this.rateLimitFilter = rateLimitFilter;
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
            .headers(headers -> headers
                .frameOptions(f -> f.deny())                            // X-Frame-Options: DENY
                .contentTypeOptions(Customizer.withDefaults())          // X-Content-Type-Options: nosniff
                .referrerPolicy(r -> r.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(h -> h
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))                         // HSTS 1 an
                .permissionsPolicy(p -> p.policy(
                    "geolocation=(), microphone=(), camera=(), payment=()"))
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/pdf/ping").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // PDF : accessible aux guests ET aux users (le controleur differenciera)
                .requestMatchers("/api/pdf/**").permitAll()
                // Admin reserve aux ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Jobs : accessible aux guests (cookie) ET aux users — le controleur differencie
                .requestMatchers("/api/jobs/**").permitAll()
                // /api/auth/me et tout le reste : authentification requise
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
