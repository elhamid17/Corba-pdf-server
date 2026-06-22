package sn.ussein.gateway.integration;

import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Conteneur Mongo partagé entre toutes les classes de test d'intégration.
 * Démarrage manuel : évite les problèmes de cycle de vie JUnit sur classes abstraites.
 */
public final class SharedMongoContainer {

    private static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7"));

    static {
        MONGO.start();
    }

    private SharedMongoContainer() {}

    public static String connectionString() {
        return MONGO.getConnectionString();
    }
}
