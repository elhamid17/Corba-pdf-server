package sn.ussein.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import sn.ussein.gateway.config.JwtProperties;
import sn.ussein.gateway.model.Role;
import sn.ussein.gateway.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(JwtProperties props) {
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        // HS256 demande >= 32 octets ; on rejette tot une cle trop courte
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret doit faire au moins 32 caracteres (HS256). "
                + "Definir JWT_SECRET avec une chaine aleatoire suffisamment longue.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = props.getExpirationHours() * 3600L;
    }

    public String issueToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toList());

        return Jwts.builder()
            .subject(user.getId())
            .claim("username", user.getUsername())
            .claim("email", user.getEmail())
            .claim("roles", roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expirationSeconds, ChronoUnit.SECONDS)))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Parse et verifie le token. Retourne null si invalide ou expire.
     */
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) claims.get("roles");
            Set<Role> roles = roleNames == null
                ? Set.of()
                : roleNames.stream().map(Role::valueOf).collect(Collectors.toSet());

            return new ParsedToken(
                claims.getSubject(),
                (String) claims.get("username"),
                (String) claims.get("email"),
                roles
            );
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public record ParsedToken(String userId, String username, String email, Set<Role> roles) {}
}
