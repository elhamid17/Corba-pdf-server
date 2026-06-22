package sn.ussein.gateway.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import sn.ussein.gateway.config.MongoConfig;
import sn.ussein.gateway.config.QuotaProperties;
import sn.ussein.gateway.repository.JobRepository;
import sn.ussein.gateway.repository.UserRepository;
import sn.ussein.gateway.service.JobStorageService;

/**
 * Base Testcontainers MongoDB pour les tests d'intégration gateway.
 * Nécessite Docker en cours d'exécution.
 */
@DataMongoTest
@Import({JobStorageService.class, QuotaProperties.class, MongoConfig.class})
@TestPropertySource(properties = {
    "app.quota.guest.max-bytes=10240",
    "app.quota.guest.max-files=3",
    "app.quota.guest.retention-hours=24",
    "app.quota.user.max-bytes=51200",
    "app.quota.user.retention-days=30",
    "app.quota.admin.max-bytes=-1"
})
public abstract class AbstractMongoIntegrationTest {

    @DynamicPropertySource
    static void registerMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", SharedMongoContainer::connectionString);
        registry.add("spring.data.mongodb.database", () -> "corba_pdf_test");
    }

    @Autowired protected MongoTemplate mongoTemplate;
    @Autowired protected JobStorageService storage;
    @Autowired protected JobRepository jobs;
    @Autowired protected UserRepository users;

    @BeforeEach
    void cleanDatabase() {
        jobs.deleteAll();
        users.deleteAll();
        mongoTemplate.dropCollection("fs.files");
        mongoTemplate.dropCollection("fs.chunks");
    }
}
