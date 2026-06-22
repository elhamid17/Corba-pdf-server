package sn.ussein.gateway.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import sn.ussein.gateway.model.Role;
import sn.ussein.gateway.model.User;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerIntegrationTest extends AbstractGatewayIntegrationTest {

    @Autowired private ObjectMapper objectMapper;

    @Test
    void register_success_returnsTokenAndUser() throws Exception {
        String body = """
            {"email":"newbie@test.local","username":"newbie","password":"password123"}
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("newbie"))
            .andExpect(jsonPath("$.user.email").value("newbie@test.local"));
    }

    @Test
    void register_duplicateEmail_returns400() throws Exception {
        users.save(new User("dup@test.local", "dupuser", "hash"));

        String body = """
            {"email":"dup@test.local","username":"other","password":"password123"}
            """;

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("email")));
    }

    @Test
    void login_success_returnsToken() throws Exception {
        registerUser("login@test.local", "loginuser", "password123");

        String body = """
            {"identifier":"loginuser","password":"password123"}
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("loginuser"));
    }

    @Test
    void login_wrongPassword_returns400() throws Exception {
        registerUser("badlogin@test.local", "badlogin", "password123");

        String body = """
            {"identifier":"badlogin","password":"wrong-password"}
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Identifiants")));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsProfile() throws Exception {
        registerUser("me@test.local", "meuser", "password123");
        String token = loginAndGetToken("meuser", "password123");

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("meuser"))
            .andExpect(jsonPath("$.email").value("me@test.local"))
            .andExpect(jsonPath("$.storageBytesUsed").value(0));
    }

    private void registerUser(String email, String username, String password) throws Exception {
        String body = String.format(
            "{\"email\":\"%s\",\"username\":\"%s\",\"password\":\"%s\"}",
            email, username, password);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk());
    }

    private String loginAndGetToken(String identifier, String password) throws Exception {
        String body = String.format(
            "{\"identifier\":\"%s\",\"password\":\"%s\"}", identifier, password);
        var result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }
}
