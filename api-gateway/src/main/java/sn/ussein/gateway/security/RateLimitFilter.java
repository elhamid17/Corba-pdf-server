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
import sn.ussein.gateway.config.RateLimitProperties;
import sn.ussein.gateway.web.ApiPaths;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting par IP sur les endpoints sensibles (auth + PDF).
 *
 * Algorithme : sliding window simple avec ConcurrentHashMap.
 * Note : implementation en-memoire, reset au redemarrage, non partagee entre instances.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final ConcurrentHashMap<String, Window> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> pdfBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = clientIp(request);
        long now = System.currentTimeMillis();

        Integer limit = resolveLimit(path);
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        ConcurrentHashMap<String, Window> buckets =
            path.startsWith(ApiPaths.PDF) ? pdfBuckets : authBuckets;

        Window w = buckets.compute(ip, (k, existing) -> {
            if (existing == null || now - existing.windowStart > props.getWindowMs()) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (w.count.get() > limit) {
            long retryAfter = (props.getWindowMs() - (now - w.windowStart)) / 1000;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1, retryAfter)));
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Too Many Requests\",\"message\":\"Trop de requetes. Reessayez dans "
                + Math.max(1, retryAfter) + "s.\",\"status\":429}");
            return;
        }

        if (Math.random() < 0.01) {
            purgeExpired(buckets, now);
        }

        chain.doFilter(request, response);
    }

    /** Retourne la limite applicable, ou null si le path n'est pas rate-limite. */
    private Integer resolveLimit(String path) {
        if (path.equals(ApiPaths.AUTH + "/login") || path.equals(ApiPaths.AUTH + "/register")) {
            return props.getAuthMaxRequests();
        }
        // Ping : le healthcheck Docker appelle cet endpoint en boucle.
        if (path.startsWith(ApiPaths.PDF + "/") && !path.equals(ApiPaths.PDF + "/ping")) {
            return props.getPdfMaxRequests();
        }
        return null;
    }

    private void purgeExpired(ConcurrentHashMap<String, Window> buckets, long now) {
        buckets.entrySet().removeIf(e -> now - e.getValue().windowStart > props.getWindowMs());
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
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
