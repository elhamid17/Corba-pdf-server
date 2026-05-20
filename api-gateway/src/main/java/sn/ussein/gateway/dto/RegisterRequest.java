package sn.ussein.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @NotBlank @Email
    String email,

    @NotBlank @Size(min = 3, max = 32)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
        message = "le nom d'utilisateur ne peut contenir que des lettres, chiffres, . _ -")
    String username,

    @NotBlank @Size(min = 8, max = 128)
    String password
) {}
