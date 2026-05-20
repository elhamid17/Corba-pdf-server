package sn.ussein.gateway.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import sn.ussein.gateway.model.Role;

@Service
public class IdentityResolver {

    /** Attribut requete pose par GuestCookieFilter. */
    public static final String GUEST_ID_ATTR = "guestId";

    /**
     * Renvoie l'identite de l'appelant.
     * - Si JWT valide pose un AuthenticatedPrincipal : user
     * - Sinon : guest (le GuestCookieFilter aura pose le cookie / l'attribut)
     */
    public Identity current(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AuthenticatedPrincipal p) {
            return Identity.forUser(p.userId(), p.roles().contains(Role.ADMIN));
        }
        String guestId = (String) request.getAttribute(GUEST_ID_ATTR);
        return Identity.forGuest(guestId);
    }
}
