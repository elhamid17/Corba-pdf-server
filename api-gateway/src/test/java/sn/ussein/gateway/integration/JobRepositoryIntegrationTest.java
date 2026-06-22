package sn.ussein.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.model.JobStatus;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.security.Identity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JobRepositoryIntegrationTest extends AbstractMongoIntegrationTest {

    @Test
    void findByUserIdOrderByCreatedAtDesc_returnsNewestFirst() {
        User user = users.save(new User("repo@test.local", "repo-user", "hash"));
        Identity identity = Identity.forUser(user.getId(), false);

        storage.recordSuccess(identity, "merge", "a.pdf", "a.pdf",
            "application/pdf", new byte[10], 1L);
        storage.recordSuccess(identity, "split", "b.pdf", "b.pdf",
            "application/pdf", new byte[10], 1L);

        var page = jobs.findByUserIdOrderByCreatedAtDesc(
            user.getId(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        // @CreatedDate : le dernier job inséré doit apparaître en premier
        assertTrue(page.getContent().get(0).getCreatedAt()
            .compareTo(page.getContent().get(1).getCreatedAt()) >= 0);
        assertEquals("split", page.getContent().get(0).getOperation());
    }

    @Test
    void findByGuestIdOrderByCreatedAtDesc_listsGuestJobsOnly() {
        Identity guestA = Identity.forGuest("guest-a");
        Identity guestB = Identity.forGuest("guest-b");

        storage.recordSuccess(guestA, "ocr", "scan.pdf", "ocr.txt",
            "text/plain", new byte[5], 1L);
        storage.recordSuccess(guestB, "merge", "x.pdf", "y.pdf",
            "application/pdf", new byte[5], 1L);

        assertEquals(1, jobs.findByGuestIdOrderByCreatedAtDesc("guest-a").size());
        assertEquals("ocr", jobs.findByGuestIdOrderByCreatedAtDesc("guest-a").get(0).getOperation());
        assertEquals(1, jobs.countByGuestId("guest-b"));
    }

    @Test
    void countByCreatedAtAfter_countsRecentJobs() {
        Job oldJob = new Job();
        oldJob.setGuestId("g");
        oldJob.setOperation("merge");
        oldJob.setStatus(JobStatus.SUCCESS);
        oldJob = jobs.save(oldJob);
        // @CreatedDate écrase la date à l'insert : on force un timestamp ancien après coup
        mongoTemplate.updateFirst(
            Query.query(Criteria.where("_id").is(oldJob.getId())),
            Update.update("createdAt", Instant.now().minusSeconds(3600)),
            Job.class);

        Identity guest = Identity.forGuest("g");
        storage.recordSuccess(guest, "compress", "in.pdf", "out.pdf",
            "application/pdf", new byte[3], 1L);

        long recent = jobs.countByCreatedAtAfter(Instant.now().minusSeconds(60));
        assertEquals(1, recent);
    }
}
