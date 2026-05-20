package sn.ussein.gateway.dto;

import java.util.List;

public record AdminStats(
    long totalUsers,
    long enabledUsers,
    long admins,
    long totalJobs,
    long jobsLast24h,
    long jobsLast7d,
    long guestJobs,
    long totalStorageBytes,
    List<OperationCount> topOperations
) {
    public record OperationCount(String operation, long count) {}
}
