package sn.ussein.gateway.security;

/**
 * Identite de l'appelant pour une requete donnee.
 * Soit un user authentifie (userId non null), soit un guest (guestId non null).
 *
 * isAdmin est un flag pratique pour court-circuiter les checks de quota.
 */
public record Identity(String userId, String guestId, boolean admin) {

    public boolean isAuthenticated() { return userId != null; }
    public boolean isGuest() { return guestId != null && userId == null; }

    public static Identity forUser(String userId, boolean admin) {
        return new Identity(userId, null, admin);
    }

    public static Identity forGuest(String guestId) {
        return new Identity(null, guestId, false);
    }
}
