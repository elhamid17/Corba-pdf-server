package sn.ussein.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import sn.ussein.gateway.dto.AdminJobView;
import sn.ussein.gateway.dto.AdminStats;
import sn.ussein.gateway.dto.AdminUserUpdateRequest;
import sn.ussein.gateway.dto.AdminUserView;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.model.Role;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.JobRepository;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.web.AuthException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Operations admin : liste/edition/suppression d'utilisateurs,
 * vue globale des jobs, statistiques.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository users;
    private final JobRepository jobs;
    private final JobStorageService storage;
    private final MongoTemplate mongo;

    public AdminService(UserRepository users,
                        JobRepository jobs,
                        JobStorageService storage,
                        MongoTemplate mongo) {
        this.users = users;
        this.jobs = jobs;
        this.storage = storage;
        this.mongo = mongo;
    }

    /* ─────────────── Users ─────────────── */

    public Page<AdminUserView> listUsers(String search, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100), sort);

        Page<User> userPage;
        if (search != null && !search.isBlank()) {
            String s = search.trim();
            userPage = users.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(s, s, pageable);
        } else {
            userPage = users.findAll(pageable);
        }

        return userPage.map(u -> AdminUserView.from(u, jobs.countByUserId(u.getId())));
    }

    public AdminUserView updateUser(String userId, AdminUserUpdateRequest req, Identity caller) {
        User u = users.findById(userId)
            .orElseThrow(() -> new AuthException("Utilisateur introuvable."));

        if (req.enabled() != null) {
            // L'admin ne peut pas se desactiver lui-meme
            if (Boolean.FALSE.equals(req.enabled())
                && caller.userId() != null && caller.userId().equals(u.getId())) {
                throw new AuthException("Vous ne pouvez pas desactiver votre propre compte.");
            }
            u.setEnabled(req.enabled());
        }

        if (req.roles() != null) {
            Set<Role> parsed = parseRoles(req.roles());
            // Empeche un admin de se retirer le role ADMIN
            if (caller.userId() != null && caller.userId().equals(u.getId())
                && !parsed.contains(Role.ADMIN)) {
                throw new AuthException("Vous ne pouvez pas retirer votre propre role admin.");
            }
            if (parsed.isEmpty()) parsed.add(Role.USER);
            u.setRoles(parsed);
        }

        users.save(u);
        return AdminUserView.from(u, jobs.countByUserId(u.getId()));
    }

    public void deleteUser(String userId, Identity caller) {
        if (caller.userId() != null && caller.userId().equals(userId)) {
            throw new AuthException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        User u = users.findById(userId)
            .orElseThrow(() -> new AuthException("Utilisateur introuvable."));

        // Purge des jobs de l'utilisateur (GridFS inclus)
        Page<Job> firstPage;
        int purged = 0;
        do {
            firstPage = jobs.findByUserIdOrderByCreatedAtDesc(u.getId(), PageRequest.of(0, 100));
            for (Job j : firstPage.getContent()) {
                storage.deleteGridFs(j.getResultGridFsId());
                jobs.delete(j);
                purged++;
            }
        } while (firstPage.hasNext());

        users.delete(u);
        log.info("Admin {} : utilisateur {} supprime ({} job(s) purges)",
            caller.userId(), u.getUsername(), purged);
    }

    /* ─────────────── Jobs ─────────────── */

    public Page<AdminJobView> listJobs(int page, int size) {
        PageRequest pageable = PageRequest.of(
            Math.max(0, page), Math.min(Math.max(size, 1), 100));
        Page<Job> jobPage = jobs.findAllByOrderByCreatedAtDesc(pageable);

        Set<String> userIds = jobPage.getContent().stream()
            .map(Job::getUserId)
            .filter(id -> id != null)
            .collect(Collectors.toSet());

        Map<String, String> usernames = new HashMap<>();
        if (!userIds.isEmpty()) {
            users.findAllById(userIds).forEach(u -> usernames.put(u.getId(), u.getUsername()));
        }

        return jobPage.map(j -> AdminJobView.from(j, usernames.get(j.getUserId())));
    }

    /* ─────────────── Stats ─────────────── */

    public AdminStats stats() {
        long totalUsers = users.count();
        long enabledUsers = users.countByEnabled(true);
        long admins = users.countByRolesContaining(Role.ADMIN);

        long totalJobs = jobs.count();
        Instant now = Instant.now();
        long jobsLast24h = jobs.countByCreatedAtAfter(now.minus(24, ChronoUnit.HOURS));
        long jobsLast7d = jobs.countByCreatedAtAfter(now.minus(7, ChronoUnit.DAYS));
        long guestJobs = jobs.countByUserIdIsNull();

        long totalStorage = users.findAll().stream()
            .mapToLong(User::getStorageBytesUsed)
            .sum();

        List<AdminStats.OperationCount> top = topOperations(5);

        return new AdminStats(totalUsers, enabledUsers, admins,
            totalJobs, jobsLast24h, jobsLast7d, guestJobs, totalStorage, top);
    }

    private List<AdminStats.OperationCount> topOperations(int limit) {
        Aggregation agg = Aggregation.newAggregation(
            Aggregation.group("operation").count().as("count"),
            Aggregation.sort(Sort.Direction.DESC, "count"),
            Aggregation.limit(limit)
        );
        AggregationResults<OperationCountRow> res = mongo.aggregate(agg, "jobs", OperationCountRow.class);
        List<AdminStats.OperationCount> out = new ArrayList<>();
        for (OperationCountRow row : res.getMappedResults()) {
            if (row._id != null) {
                out.add(new AdminStats.OperationCount(row._id, row.count));
            }
        }
        return out;
    }

    private Set<Role> parseRoles(Set<String> raw) {
        Set<Role> out = EnumSet.noneOf(Role.class);
        for (String r : raw) {
            if (r == null) continue;
            try {
                out.add(Role.valueOf(r.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // ignore les roles inconnus
            }
        }
        return out;
    }

    /** DTO interne pour l'aggregation Mongo. */
    static class OperationCountRow {
        public String _id;
        public long count;
    }
}
