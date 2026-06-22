package sn.ussein.gateway.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import sn.ussein.gateway.model.Role;
import sn.ussein.gateway.model.User;
import sn.ussein.gateway.security.Identity;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdminControllerIntegrationTest extends AbstractGatewayIntegrationTest {

    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void seedUsers() throws Exception {
        // AdminBootstrapper cree admin-it au demarrage ; on le retrouve apres clean users
        // -> on recree manuellement un admin pour ces tests HTTP.
        User admin = new User("admin-http@test.local", "admin-http", "{noop}unused");
        Set<Role> roles = new HashSet<>(Set.of(Role.USER, Role.ADMIN));
        admin.setRoles(roles);
        admin = users.save(admin);
        adminToken = jwtService.issueToken(admin);

        String reg = """
            {"email":"regular@test.local","username":"regular","password":"password123"}
            """;
        var loginRes = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reg))
            .andExpect(status().isOk())
            .andReturn();
        userToken = objectMapper.readTree(loginRes.getResponse().getContentAsString())
            .get("token").asText();

        storage.recordSuccess(Identity.forGuest("guest-admin-it"),
            "merge", "in.pdf", "out.pdf", "application/pdf", new byte[]{1}, 1L);
    }

    @Test
    void stats_asAdmin_returnsAggregates() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").isNumber())
            .andExpect(jsonPath("$.totalJobs").isNumber());
    }

    @Test
    void stats_asRegularUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_asAdmin_returnsPage() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", not(empty())))
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void listJobs_asAdmin_includesStoredJobs() throws Exception {
        mockMvc.perform(get("/api/admin/jobs")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", not(empty())))
            .andExpect(jsonPath("$.content[0].operation").value("merge"));
    }

    @Test
    void adminEndpoints_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
            .andExpect(status().isUnauthorized());
    }
}
