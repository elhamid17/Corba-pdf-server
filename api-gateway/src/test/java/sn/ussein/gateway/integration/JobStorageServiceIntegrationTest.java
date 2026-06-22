package sn.ussein.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.web.QuotaExceededException;

import static org.junit.jupiter.api.Assertions.*;

class JobStorageServiceIntegrationTest extends AbstractMongoIntegrationTest {

    @Test
    void recordSuccess_guest_persistsJobAndGridFsPayload() throws Exception {
        Identity guest = Identity.forGuest("guest-abc");
        byte[] pdf = "%PDF-1.4 integration test".getBytes();

        var job = storage.recordSuccess(
            guest, "merge", "input.pdf", "merged.pdf",
            "application/pdf", pdf, 42L);

        assertNotNull(job.getId());
        assertEquals("guest-abc", job.getGuestId());
        assertNull(job.getUserId());
        assertEquals(pdf.length, job.getResultSizeBytes());
        assertNotNull(job.getResultGridFsId());
        assertNotNull(job.getExpiresAt());

        GridFsResource resource = storage.loadResult(job.getId(), guest);
        assertNotNull(resource);
        assertArrayEquals(pdf, resource.getInputStream().readAllBytes());
    }

    @Test
    void loadResult_otherGuest_isDenied() {
        Identity owner = Identity.forGuest("guest-owner");
        var job = storage.recordSuccess(
            owner, "compress", "in.pdf", "out.pdf",
            "application/pdf", new byte[]{1, 2, 3}, 10L);

        Identity intruder = Identity.forGuest("guest-other");
        assertNull(storage.loadResult(job.getId(), intruder));
    }

    @Test
    void delete_removesJobAndGridFsFile() throws Exception {
        Identity guest = Identity.forGuest("guest-del");
        byte[] pdf = new byte[]{9, 8, 7};
        var job = storage.recordSuccess(
            guest, "split", "in.pdf", "parts.zip",
            "application/zip", pdf, 5L);

        assertTrue(storage.delete(job.getId(), guest));
        assertTrue(jobs.findById(job.getId()).isEmpty());
        assertNull(storage.loadResult(job.getId(), guest));
    }

    @Test
    void checkQuota_guest_cumulativeBytesEnforcedAgainstStoredJobs() {
        Identity guest = Identity.forGuest("guest-quota");
        storage.recordSuccess(
            guest, "merge", "a.pdf", "a-out.pdf",
            "application/pdf", new byte[7000], 1L);
        storage.recordSuccess(
            guest, "merge", "b.pdf", "b-out.pdf",
            "application/pdf", new byte[7000], 1L);

        // 7000 + 7000 + 4000 > 10240 (quota test)
        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(guest, 4000));
        assertTrue(ex.getMessage().contains("cumules"));
    }

    @Test
    void checkQuota_guest_maxFilesEnforced() {
        Identity guest = Identity.forGuest("guest-files");
        for (int i = 0; i < 3; i++) {
            storage.recordSuccess(
                guest, "merge", "in.pdf", "out-" + i + ".pdf",
                "application/pdf", new byte[100], 1L);
        }

        assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(guest, 100));
    }

    @Test
    void recordSuccess_user_incrementsStorageCounter() {
        User user = users.save(new User("u@test.local", "user1", "hash"));
        Identity identity = Identity.forUser(user.getId(), false);

        storage.recordSuccess(
            identity, "merge", "in.pdf", "out.pdf",
            "application/pdf", new byte[8000], 20L);

        User updated = users.findById(user.getId()).orElseThrow();
        assertEquals(8000, updated.getStorageBytesUsed());
    }

    @Test
    void delete_user_decrementsStorageCounter() {
        User user = users.save(new User("d@test.local", "user2", "hash"));
        Identity identity = Identity.forUser(user.getId(), false);

        var job = storage.recordSuccess(
            identity, "merge", "in.pdf", "out.pdf",
            "application/pdf", new byte[6000], 15L);

        storage.delete(job.getId(), identity);

        User updated = users.findById(user.getId()).orElseThrow();
        assertEquals(0, updated.getStorageBytesUsed());
    }

    @Test
    void checkQuota_user_enforcedWithRealStorageCounter() {
        User user = users.save(new User("q@test.local", "user3", "hash"));
        user.setStorageBytesUsed(50_000);
        users.save(user);

        Identity identity = Identity.forUser(user.getId(), false);
        assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(identity, 5000));
    }
}
