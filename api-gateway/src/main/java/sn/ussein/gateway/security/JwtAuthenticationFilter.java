package sn.ussein.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lit le header Authorization: Bearer <jwt>, le verifie via JwtService,
 * et pose l'Authentication dans le SecurityContext si valide.
 *
 * Pas de token ou token invalide : on laisse passer en mode anonyme
 * (les endpoints publics et les guests doivent rester accessibles).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(PREFIX.length()).trim();
            JwtService.ParsedToken parsed = jwtService.parse(token);

            if (parsed != null) {
                List<SimpleGrantedAuthority> authorities = parsed.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .collect(Collectors.toList());

                AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                    parsed.userId(), parsed.username(), parsed.email(), parsed.roles()
                );

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
