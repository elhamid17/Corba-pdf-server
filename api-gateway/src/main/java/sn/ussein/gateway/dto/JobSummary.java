package sn.ussein.gateway.dto;

import sn.ussein.gateway.model.Job;

import java.time.Instant;

public record JobSummary(
    String id,
    String operation,
    String status,
    String inputFilename,
    String outputFilename,
    String outputContentType,
    long resultSizeBytes,
    long durationMs,
    Instant createdAt,
    Instant expiresAt,
    boolean downloadable
) {
    public static JobSummary from(Job j) {
        return new JobSummary(
            j.getId(),
            j.getOperation(),
            j.getStatus().name(),
            j.getInputFilename(),
            j.getOutputFilename(),
            j.getOutputContentType(),
            j.getResultSizeBytes(),
            j.getDurationMs(),
            j.getCreatedAt(),
            j.getExpiresAt(),
            j.getResultGridFsId() != null
        );
    }
}
