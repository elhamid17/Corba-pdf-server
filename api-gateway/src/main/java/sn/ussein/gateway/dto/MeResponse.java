package sn.ussein.gateway.dto;

import java.time.Instant;
import java.util.Set;

public record MeResponse(
    String id,
    String username,
    String email,
    Set<String> roles,
    long storageBytesUsed,
    Instant createdAt,
    Instant lastLoginAt
) {}
