package sn.ussein.gateway.dto;

import sn.ussein.gateway.model.Job;

import java.time.Instant;

/**
 * Vue admin d'un Job : ajoute l'identite du proprietaire (user ou guest)
 * et le nom d'utilisateur si disponible.
 */
public record AdminJobView(
    String id,
    String operation,
    String status,
    String inputFilename,
    String outputFilename,
    long resultSizeBytes,
    long durationMs,
    Instant createdAt,
    Instant expiresAt,
    boolean downloadable,
    String userId,
    String username,
    String guestId
) {
    public static AdminJobView from(Job j, String username) {
        return new AdminJobView(
            j.getId(),
            j.getOperation(),
            j.getStatus().name(),
            j.getInputFilename(),
            j.getOutputFilename(),
            j.getResultSizeBytes(),
            j.getDurationMs(),
            j.getCreatedAt(),
            j.getExpiresAt(),
            j.getResultGridFsId() != null,
            j.getUserId(),
            username,
            j.getGuestId()
        );
    }
}
