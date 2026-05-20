package sn.ussein.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.JobRepository;

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
    private final GridFsTemplate gridFs;
    private final MongoTemplate mongo;

    public JobCleanupTask(JobRepository jobs, GridFsTemplate gridFs, MongoTemplate mongo) {
        this.jobs = jobs;
        this.gridFs = gridFs;
        this.mongo = mongo;
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
                if (j.getUserId() != null && j.getResultSizeBytes() > 0) {
                    // $inc atomique pour eviter la race avec d'autres ecritures
                    // concurrentes sur le meme user (recordSuccess, delete).
                    mongo.updateFirst(
                        new Query(Criteria.where("_id").is(j.getUserId())),
                        new Update().inc("storageBytesUsed", -j.getResultSizeBytes()),
                        User.class
                    );
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
