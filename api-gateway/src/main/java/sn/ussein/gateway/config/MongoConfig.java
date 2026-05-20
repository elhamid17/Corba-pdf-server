package sn.ussein.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Active l'audit Mongo (@CreatedDate) et le scan des repositories.
 * La connexion elle-meme est configuree via spring.data.mongodb.* dans application.yml.
 */
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "sn.ussein.gateway.repository")
public class MongoConfig {
}
