package sn.ussein.gateway.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Le champ "identifier" accepte soit l'email, soit le username.
 */
public record LoginRequest(

    @NotBlank
    String identifier,

    @NotBlank
    String password
) {}
