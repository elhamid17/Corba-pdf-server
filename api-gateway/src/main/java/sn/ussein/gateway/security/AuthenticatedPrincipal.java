package sn.ussein.gateway.security;

import sn.ussein.gateway.model.Role;

import java.util.Set;

/**
 * Principal place dans le SecurityContext apres validation du JWT.
 * Sert d'identite legere : pas de re-lecture Mongo a chaque requete.
 */
public record AuthenticatedPrincipal(
    String userId,
    String username,
    String email,
    Set<Role> roles
) {}
