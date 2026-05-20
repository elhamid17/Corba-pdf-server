package sn.ussein.gateway.dto;

import java.util.Set;

/**
 * Mise a jour partielle d'un utilisateur depuis la page admin.
 * Tous les champs sont optionnels : null = inchange.
 */
public record AdminUserUpdateRequest(
    Boolean enabled,
    Set<String> roles
) {}
