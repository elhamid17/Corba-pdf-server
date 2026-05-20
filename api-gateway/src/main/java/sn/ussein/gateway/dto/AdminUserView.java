package sn.ussein.gateway.dto;

import sn.ussein.gateway.model.User;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record AdminUserView(
    String id,
    String email,
    String username,
    Set<String> roles,
    boolean enabled,
    long storageBytesUsed,
    long jobsCount,
    Instant createdAt,
    Instant lastLoginAt
) {
    public static AdminUserView from(User u, long jobsCount) {
        return new AdminUserView(
            u.getId(),
            u.getEmail(),
            u.getUsername(),
            u.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
            u.isEnabled(),
            u.getStorageBytesUsed(),
            jobsCount,
            u.getCreatedAt(),
            u.getLastLoginAt()
        );
    }
}
