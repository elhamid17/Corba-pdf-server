package sn.ussein.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.repository.JobRepository;
import sn.ussein.gateway.repository.UserRepository;

import java.time.Instant;
import java.util.List;

/**
 * Purge les jobs expires : supprime le fichier GridFS puis le doc Job,
 * et decremente le compteur de stockage de l'utilisateur si applicable.
 *
 * Tourne toutes les heures et 30s apres le demarrage.
 */
@Component
public class JobCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(JobCleanupTask.class);

    private final JobRepository jobs;
    private final UserRepository users;
    private final GridFsTemplate gridFs;

    public JobCleanupTask(JobRepository jobs, UserRepository users, GridFsTemplate gridFs) {
        this.jobs = jobs;
        this.users = users;
        this.gridFs = gridFs;
    }

    @Scheduled(initialDelayString = "PT30S", fixedDelayString = "PT1H")
    public void purgeExpired() {
        List<Job> expired = jobs.findAll().stream()
            .filter(j -> j.getExpiresAt() != null && j.getExpiresAt().isBefore(Instant.now()))
            .toList();

        if (expired.isEmpty()) return;

        int deleted = 0;
        for (Job j : expired) {
            try {
                if (j.getResultGridFsId() != null) {
                    gridFs.delete(new Query(Criteria.where("_id").is(j.getResultGridFsId())));
                }
                if (j.getUserId() != null) {
                    users.findById(j.getUserId()).ifPresent(u -> {
                        long newUsage = Math.max(0L, u.getStorageBytesUsed() - j.getResultSizeBytes());
                        u.setStorageBytesUsed(newUsage);
                        users.save(u);
                    });
                }
                jobs.delete(j);
                deleted++;
            } catch (Exception e) {
                log.warn("Echec purge job {} : {}", j.getId(), e.getMessage());
            }
        }
        log.info("JobCleanupTask : {} job(s) purge(s)", deleted);
    }
}
