package sn.ussein.gateway.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import sn.ussein.gateway.config.QuotaProperties;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.model.JobStatus;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.JobRepository;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.web.QuotaExceededException;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Persiste les operations PDF :
 *   - verifie les quotas avant d'accepter le fichier de sortie
 *   - stocke le binaire dans GridFS
 *   - cree un Job lie a l'identite (user ou guest)
 *
 * Pas de transaction Mongo necessaire : la table jobs et GridFS sont
 * dans la meme base et l'ordre d'insertion (file -> doc) garantit qu'on
 * n'a jamais de doc Job pointant vers un fichier inexistant.
 */
@Service
public class JobStorageService {

    private static final Logger log = LoggerFactory.getLogger(JobStorageService.class);

    private final GridFsTemplate gridFs;
    private final JobRepository jobs;
    private final UserRepository users;
    private final QuotaProperties quotas;

    public JobStorageService(GridFsTemplate gridFs,
                             JobRepository jobs,
                             UserRepository users,
                             QuotaProperties quotas) {
        this.gridFs = gridFs;
        this.jobs = jobs;
        this.users = users;
        this.quotas = quotas;
    }

    /**
     * Verifie les quotas avant d'accepter une nouvelle sortie de taille newOutputBytes.
     * Leve QuotaExceededException si depassement.
     */
    public void checkQuota(Identity identity, long newOutputBytes) {
        if (identity.admin()) {
            return;
        }

        if (identity.isAuthenticated()) {
            long max = quotas.getUser().getMaxBytes();
            if (max < 0) return; // illimite

            User u = users.findById(identity.userId()).orElse(null);
            if (u == null) return;
            if (u.getStorageBytesUsed() + newOutputBytes > max) {
                throw new QuotaExceededException(
                    "Quota de stockage depasse (" + humanBytes(max)
                    + "). Supprimez d'anciens fichiers depuis votre historique.");
            }
            return;
        }

        // Guest
        long maxBytes = quotas.getGuest().getMaxBytes();
        int maxFiles = quotas.getGuest().getMaxFiles();

        long currentFiles = jobs.countByGuestId(identity.guestId());
        if (currentFiles >= maxFiles) {
            throw new QuotaExceededException(
                "Limite de " + maxFiles + " fichiers atteinte pour les invites. "
                + "Creez un compte pour augmenter votre quota.");
        }
        // On approxime l'occupation guest a la taille du nouveau fichier
        // (les anciens sont auto-purges apres retention-hours).
        if (newOutputBytes > maxBytes) {
            throw new QuotaExceededException(
                "Fichier trop volumineux pour un invite (" + humanBytes(maxBytes)
                + " max). Creez un compte pour augmenter votre quota.");
        }
    }

    /**
     * Stocke le resultat dans GridFS et cree un Job en base.
     * Suppose que checkQuota() a deja ete appele.
     */
    public Job recordSuccess(Identity identity,
                             String operation,
                             String inputFilename,
                             String outputFilename,
                             String outputContentType,
                             byte[] data,
                             long durationMs) {

        String gridFsId = storeInGridFs(outputFilename, outputContentType, data);

        Job job = new Job();
        job.setUserId(identity.userId());
        job.setGuestId(identity.guestId());
        job.setOperation(operation);
        job.setStatus(JobStatus.SUCCESS);
        job.setInputFilename(inputFilename);
        job.setOutputFilename(outputFilename);
        job.setOutputContentType(outputContentType);
        job.setResultGridFsId(gridFsId);
        job.setResultSizeBytes(data.length);
        job.setExpiresAt(computeExpiry(identity));
        job.setDurationMs(durationMs);

        job = jobs.save(job);

        if (identity.isAuthenticated()) {
            users.findById(identity.userId()).ifPresent(u -> {
                u.setStorageBytesUsed(u.getStorageBytesUsed() + data.length);
                users.save(u);
            });
        }

        log.info("Job {} ({}) : {} octets stockes pour {}",
            job.getId(), operation, data.length,
            identity.isAuthenticated() ? "user " + identity.userId() : "guest " + identity.guestId());

        return job;
    }

    /**
     * Charge un fichier GridFS si l'identite a le droit d'y acceder.
     * Renvoie null si introuvable ou non autorise.
     */
    public GridFsResource loadResult(String jobId, Identity caller) {
        Job job = jobs.findById(jobId).orElse(null);
        if (job == null || job.getResultGridFsId() == null) {
            return null;
        }
        if (!canAccess(job, caller)) {
            return null;
        }

        GridFSFile file = gridFs.findOne(
            new Query(Criteria.where("_id").is(job.getResultGridFsId())));
        if (file == null) {
            return null;
        }
        return gridFs.getResource(file);
    }

    /**
     * Supprime un job + son fichier GridFS associe.
     * Renvoie true si supprime, false si introuvable ou non autorise.
     */
    public boolean delete(String jobId, Identity caller) {
        Job job = jobs.findById(jobId).orElse(null);
        if (job == null) return false;
        if (!canAccess(job, caller)) return false;

        deleteGridFs(job.getResultGridFsId());
        jobs.delete(job);

        if (job.getUserId() != null) {
            users.findById(job.getUserId()).ifPresent(u -> {
                long newUsage = Math.max(0L, u.getStorageBytesUsed() - job.getResultSizeBytes());
                u.setStorageBytesUsed(newUsage);
                users.save(u);
            });
        }
        return true;
    }

    /**
     * Verifie qu'un job appartient a l'appelant (admin a tous les droits).
     */
    public boolean canAccess(Job job, Identity caller) {
        if (caller.admin()) return true;
        if (caller.isAuthenticated() && caller.userId().equals(job.getUserId())) return true;
        if (caller.isGuest() && caller.guestId() != null && caller.guestId().equals(job.getGuestId())) return true;
        return false;
    }

    void deleteGridFs(String gridFsId) {
        if (gridFsId == null) return;
        try {
            gridFs.delete(new Query(Criteria.where("_id").is(gridFsId)));
        } catch (Exception e) {
            log.warn("Echec suppression GridFS {} : {}", gridFsId, e.getMessage());
        }
    }

    private String storeInGridFs(String filename, String contentType, byte[] data) {
        ObjectId id = gridFs.store(new ByteArrayInputStream(data), filename, contentType);
        return id.toHexString();
    }

    private Instant computeExpiry(Identity identity) {
        if (identity.admin()) {
            return null; // jamais expire
        }
        if (identity.isAuthenticated()) {
            return Instant.now().plus(quotas.getUser().getRetentionDays(), ChronoUnit.DAYS);
        }
        return Instant.now().plus(quotas.getGuest().getRetentionHours(), ChronoUnit.HOURS);
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
