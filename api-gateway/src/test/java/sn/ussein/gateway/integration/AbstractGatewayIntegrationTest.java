package sn.ussein.gateway.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.security.GuestCookieFilter;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.JwtService;
import sn.ussein.gateway.service.CorbaClientService;
import sn.ussein.gateway.service.JobStorageService;

/**
 * Base pour les tests HTTP d'intégration (MockMvc + Mongo Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.jwt.secret=test-secret-key-at-least-32-characters-long",
    "app.jwt.expiration-hours=24",
    "app.quota.guest.max-bytes=1048576",
    "app.quota.guest.max-files=10",
    "app.quota.guest.retention-hours=24",
    "app.quota.user.max-bytes=10485760",
    "app.quota.user.retention-days=30",
    "app.quota.admin.max-bytes=-1",
    "app.admin.email=admin-it@test.local",
    "app.admin.username=admin-it",
    "app.admin.password=admin-it-pass",
    "app.rate-limit.auth-max-requests=10000",
    "app.rate-limit.pdf-max-requests=10000"
})
public abstract class AbstractGatewayIntegrationTest {

    @DynamicPropertySource
    static void registerMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", SharedMongoContainer::connectionString);
        registry.add("spring.data.mongodb.database", () -> "corba_pdf_test_http");
    }

    @MockBean
    protected CorbaClientService corbaClient;

    @Autowired protected MockMvc mockMvc;
    @Autowired protected JobStorageService storage;
    @Autowired protected UserRepository users;
    @Autowired protected JwtService jwtService;
    @Autowired protected MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.getCollection("jobs").deleteMany(new org.bson.Document());
        mongoTemplate.getCollection("users").deleteMany(new org.bson.Document());
        mongoTemplate.dropCollection("fs.files");
        mongoTemplate.dropCollection("fs.chunks");
    }

    protected static jakarta.servlet.http.Cookie guestCookie(String guestId) {
        return new jakarta.servlet.http.Cookie(GuestCookieFilter.COOKIE_NAME, guestId);
    }
}
