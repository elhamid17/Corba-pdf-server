package sn.ussein.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting basique sur /api/auth/* pour resister au brute-force.
 *
 * Algorithme : sliding window simple par IP avec ConcurrentHashMap.
 * Limite par defaut : 10 requetes / minute par IP sur les endpoints d'auth.
 *
 * Note : implementation en-memoire, donc reset au redemarrage et non
 * partagee entre instances. Suffisant pour un deploiement mono-instance
 * (Render free tier). Pour scaler horizontalement il faudrait Redis.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)  // avant GuestCookieFilter
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000L;

    // Map IP -> window
    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // Seul /api/auth/login et /api/auth/register sont rate-limites.
        // /api/auth/me n'a pas besoin (verifie un JWT existant, peu coûteux).
        if (!path.equals("/api/auth/login") && !path.equals("/api/auth/register")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        Window w = buckets.compute(ip, (k, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (w.count.get() > MAX_REQUESTS) {
            long retryAfter = (WINDOW_MS - (now - w.windowStart)) / 1000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"message\":\"Trop de tentatives. Reessayez dans "
                + Math.max(1, retryAfter) + "s.\",\"status\":429}");
            return;
        }

        // Nettoyage opportuniste : 1% de chance par requete de purger les anciens buckets
        if (Math.random() < 0.01) {
            buckets.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS);
        }

        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For peut etre "ip1, ip2, ip3" : on prend la 1ere
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private static class Window {
        final long windowStart;
        final AtomicInteger count;
        Window(long start, AtomicInteger count) {
            this.windowStart = start;
            this.count = count;
        }
    }
}
