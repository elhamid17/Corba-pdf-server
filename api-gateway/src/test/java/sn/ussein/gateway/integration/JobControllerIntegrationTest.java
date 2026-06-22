package sn.ussein.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.security.Identity;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JobControllerIntegrationTest extends AbstractGatewayIntegrationTest {

    @Test
    void listJobs_asGuest_returnsOnlyOwnJobs() throws Exception {
        Identity guestA = Identity.forGuest("guest-a-list");
        Identity guestB = Identity.forGuest("guest-b-list");

        storage.recordSuccess(guestA, "merge", "a.pdf", "a-out.pdf",
            "application/pdf", new byte[]{1}, 1L);
        storage.recordSuccess(guestB, "split", "b.pdf", "b-out.pdf",
            "application/pdf", new byte[]{2}, 1L);

        mockMvc.perform(get("/api/jobs").cookie(guestCookie("guest-a-list")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].operation").value("merge"))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listJobs_asAuthenticatedUser_returnsUserJobsOnly() throws Exception {
        User user = users.save(new User("http-user@test.local", "http-user", "hash"));
        String token = jwtService.issueToken(user);

        storage.recordSuccess(Identity.forUser(user.getId(), false),
            "compress", "in.pdf", "out.pdf", "application/pdf", new byte[]{3}, 2L);
        storage.recordSuccess(Identity.forGuest("some-guest"),
            "merge", "g.pdf", "g-out.pdf", "application/pdf", new byte[]{4}, 1L);

        mockMvc.perform(get("/api/jobs")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].operation").value("compress"))
            .andExpect(jsonPath("$.content[0].downloadable").value(true));
    }

    @Test
    void download_asOwner_returnsStoredBytes() throws Exception {
        Identity guest = Identity.forGuest("guest-dl");
        byte[] pdf = "%PDF-1.4 download test".getBytes();

        var job = storage.recordSuccess(guest, "merge", "in.pdf", "merged.pdf",
            "application/pdf", pdf, 10L);

        mockMvc.perform(get("/api/jobs/{id}/download", job.getId())
                .cookie(guestCookie("guest-dl")))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", containsString("merged.pdf")))
            .andExpect(content().bytes(pdf));
    }

    @Test
    void download_asOtherGuest_returns404() throws Exception {
        Identity owner = Identity.forGuest("guest-owner-dl");
        var job = storage.recordSuccess(owner, "merge", "in.pdf", "out.pdf",
            "application/pdf", new byte[]{5}, 1L);

        mockMvc.perform(get("/api/jobs/{id}/download", job.getId())
                .cookie(guestCookie("guest-intruder-dl")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void delete_asOwner_returns204AndRemovesJob() throws Exception {
        Identity guest = Identity.forGuest("guest-del-http");
        var job = storage.recordSuccess(guest, "merge", "in.pdf", "out.pdf",
            "application/pdf", new byte[]{7}, 1L);

        mockMvc.perform(delete("/api/jobs/{id}", job.getId())
                .cookie(guestCookie("guest-del-http")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/jobs/{id}/download", job.getId())
                .cookie(guestCookie("guest-del-http")))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_asOtherGuest_returns404() throws Exception {
        Identity owner = Identity.forGuest("guest-owner-del");
        var job = storage.recordSuccess(owner, "merge", "in.pdf", "out.pdf",
            "application/pdf", new byte[]{8}, 1L);

        mockMvc.perform(delete("/api/jobs/{id}", job.getId())
                .cookie(guestCookie("guest-intruder-del")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").exists());
    }
}
