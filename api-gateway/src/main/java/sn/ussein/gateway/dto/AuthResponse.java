package sn.ussein.gateway.dto;

import java.util.Set;

public record AuthResponse(
    String token,
    long expiresInSeconds,
    UserSummary user
) {
    public record UserSummary(
        String id,
        String username,
        String email,
        Set<String> roles
    ) {}
}
