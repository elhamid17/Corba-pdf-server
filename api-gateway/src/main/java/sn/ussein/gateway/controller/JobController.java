package sn.ussein.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.ussein.gateway.dto.JobSummary;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.repository.JobRepository;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.IdentityResolver;
import sn.ussein.gateway.service.JobStorageService;
import sn.ussein.gateway.web.ApiPaths;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "Jobs",
     description = "Historique des jobs : liste, re-telechargement et suppression.")
@RestController
@RequestMapping(ApiPaths.JOBS)
public class JobController {

    private static final int MAX_PAGE_SIZE = 100;

    private final JobRepository jobs;
    private final JobStorageService storage;
    private final IdentityResolver identity;

    public JobController(JobRepository jobs, JobStorageService storage, IdentityResolver identity) {
        this.jobs = jobs;
        this.storage = storage;
        this.identity = identity;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    HttpServletRequest req) {
        Identity me = identity.current(req);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        if (me.isAuthenticated()) {
            org.springframework.data.domain.Page<JobSummary> p = jobs
                .findByUserIdOrderByCreatedAtDesc(me.userId(), PageRequest.of(safePage, safeSize))
                .map(JobSummary::from);
            return pageResponse(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
        }
        if (me.isGuest()) {
            // Pour les guests on liste sans pagination (peu d'entrees, retention 24h),
            // mais on conserve la meme forme de reponse pour simplifier le frontend.
            List<JobSummary> all = jobs.findByGuestIdOrderByCreatedAtDesc(me.guestId())
                .stream().map(JobSummary::from).toList();
            return pageResponse(all, 0, all.size(), all.size(), 1);
        }
        return pageResponse(List.of(), 0, 0, 0L, 0);
    }

    private static Map<String, Object> pageResponse(List<JobSummary> content,
                                                    int page, int size,
                                                    long totalElements, int totalPages) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("content", content);
        m.put("page", page);
        m.put("size", size);
        m.put("totalElements", totalElements);
        m.put("totalPages", totalPages);
        return m;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable String id, HttpServletRequest req) throws IOException {
        Identity me = identity.current(req);
        Job job = jobs.findById(id).orElse(null);
        if (job == null || !storage.canAccess(job, me)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Fichier introuvable ou expire"));
        }

        GridFsResource resource = storage.loadResult(id, me);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Fichier introuvable ou expire"));
        }

        String contentType = job.getOutputContentType() != null
            ? job.getOutputContentType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + job.getOutputFilename() + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(job.getResultSizeBytes())
            .body(new InputStreamResource(resource.getInputStream()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, HttpServletRequest req) {
        Identity me = identity.current(req);
        boolean ok = storage.delete(id, me);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Job introuvable ou non autorise"));
        }
        return ResponseEntity.noContent().build();
    }
}
