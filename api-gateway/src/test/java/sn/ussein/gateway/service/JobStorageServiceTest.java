package sn.ussein.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import sn.ussein.gateway.config.QuotaProperties;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.repository.JobRepository;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.web.QuotaExceededException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStorageServiceTest {

    @Mock private GridFsTemplate gridFs;
    @Mock private JobRepository jobs;
    @Mock private UserRepository users;
    @Mock private MongoTemplate mongo;

    private JobStorageService storage;
    private QuotaProperties quotas;

    @BeforeEach
    void setUp() {
        quotas = new QuotaProperties();
        quotas.getGuest().setMaxBytes(20 * 1024 * 1024);
        quotas.getGuest().setMaxFiles(3);
        quotas.getUser().setMaxBytes(200 * 1024 * 1024);

        storage = new JobStorageService(gridFs, jobs, users, mongo, quotas);
    }

    @Test
    void checkQuota_admin_isUnlimited() {
        assertDoesNotThrow(() ->
            storage.checkQuota(Identity.forUser("admin-id", true), 999_999_999));
    }

    @Test
    void checkQuota_guest_exceedsMaxFiles() {
        Identity guest = Identity.forGuest("guest-1");
        when(jobs.countByGuestId("guest-1")).thenReturn(3L);

        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(guest, 1024));

        assertTrue(ex.getMessage().contains("3 fichiers"));
    }

    @Test
    void checkQuota_guest_cumulativeBytesExceeded() {
        Identity guest = Identity.forGuest("guest-2");
        when(jobs.countByGuestId("guest-2")).thenReturn(1L);

        Job existing = new Job();
        existing.setResultSizeBytes(15 * 1024 * 1024);
        when(jobs.findByGuestIdOrderByCreatedAtDesc("guest-2")).thenReturn(List.of(existing));

        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(guest, 6 * 1024 * 1024));

        assertTrue(ex.getMessage().contains("cumules"));
    }

    @Test
    void checkQuota_guest_singleFileTooLarge() {
        Identity guest = Identity.forGuest("guest-3");
        when(jobs.countByGuestId("guest-3")).thenReturn(0L);
        when(jobs.findByGuestIdOrderByCreatedAtDesc("guest-3")).thenReturn(List.of());

        assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(guest, 21 * 1024 * 1024));
    }

    @Test
    void checkQuota_user_storageExceeded() {
        Identity user = Identity.forUser("user-1", false);
        User u = new User("u@x.com", "u", "hash");
        u.setId("user-1");
        u.setStorageBytesUsed(199 * 1024 * 1024);
        when(users.findById("user-1")).thenReturn(Optional.of(u));

        assertThrows(QuotaExceededException.class,
            () -> storage.checkQuota(user, 2 * 1024 * 1024));
    }

    @Test
    void canAccess_ownerUser_returnsTrue() {
        Job job = new Job();
        job.setUserId("owner");
        assertTrue(storage.canAccess(job, Identity.forUser("owner", false)));
    }

    @Test
    void canAccess_otherGuest_returnsFalse() {
        Job job = new Job();
        job.setGuestId("guest-a");
        assertFalse(storage.canAccess(job, Identity.forGuest("guest-b")));
    }

    @Test
    void canAccess_admin_returnsTrue() {
        Job job = new Job();
        job.setUserId("someone");
        assertTrue(storage.canAccess(job, Identity.forUser("admin", true)));
    }
}
