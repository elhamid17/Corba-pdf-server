package sn.ussein.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// `Cookie` reste utilise dans readCookie() pour parser les cookies entrants.
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Garantit qu'un identifiant guest existe pour chaque requete.
 *
 * - Lit le cookie "guest_id" s'il existe ; sinon en genere un (UUID) et le pose sur la reponse
 * - Stocke l'identifiant dans l'attribut requete (lu par IdentityResolver)
 *
 * Le cookie est HttpOnly (non lisible en JS), SameSite=Lax (envoye sur navigation cross-site
 * vers notre domaine, suffisant pour notre API meme origine).
 *
 * Note : ce filtre tourne pour TOUTES les requetes — meme si l'utilisateur est connecte
 * on garde quand meme un cookie guest pour conserver l'historique anonyme pre-connexion.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GuestCookieFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "guest_id";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 90; // 90 jours

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String guestId = readCookie(request);
        if (guestId == null) {
            guestId = UUID.randomUUID().toString();
            // En HTTPS (prod) : SameSite=None + Secure pour fonctionner cross-origin
            // (frontend.onrender.com -> api.onrender.com). Sans ca le navigateur
            // ne renvoie pas le cookie sur les fetch cross-site.
            // En HTTP (dev local) : SameSite=Lax (None+Secure necessite HTTPS).
            boolean secure = isSecureRequest(request);
            String sameSite = secure ? "None" : "Lax";
            // On pose le cookie en construisant le header manuellement plutot que
            // via response.addCookie : ce dernier ne supporte pas proprement
            // SameSite sur toutes les versions de Tomcat.
            StringBuilder sb = new StringBuilder()
                .append(COOKIE_NAME).append('=').append(guestId)
                .append("; Max-Age=").append(COOKIE_MAX_AGE)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; SameSite=").append(sameSite);
            if (secure) sb.append("; Secure");
            response.addHeader("Set-Cookie", sb.toString());
        }
        request.setAttribute(IdentityResolver.GUEST_ID_ATTR, guestId);

        chain.doFilter(request, response);
    }

    /**
     * Detecte si la requete arrive en HTTPS, en tenant compte du header
     * X-Forwarded-Proto pose par nginx/Render (active via
     * server.forward-headers-strategy=framework).
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) return true;
        String proto = request.getHeader("X-Forwarded-Proto");
        return proto != null && proto.equalsIgnoreCase("https");
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (COOKIE_NAME.equals(c.getName())) {
                String v = c.getValue();
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }
}
