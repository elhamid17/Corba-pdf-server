package sn.ussein.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.model.User;

/**
 * Cree les indexes Mongo declares via @Indexed apres le demarrage,
 * de facon asynchrone : ne bloque pas le boot (qui doit rester rapide
 * pour passer le healthcheck Render), et tolere une connexion Atlas
 * temporairement lente.
 *
 * Equivalent a spring.data.mongodb.auto-index-creation=true, mais
 * deplace hors du chemin critique du startup.
 */
@Component
public class IndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

    private final MongoTemplate mongo;
    private final MongoMappingContext mappingContext;

    public IndexInitializer(MongoTemplate mongo,
                            @Autowired(required = false) MongoMappingContext mappingContext) {
        this.mongo = mongo;
        this.mappingContext = mappingContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void createIndexes() {
        if (mappingContext == null) {
            log.warn("MongoMappingContext indisponible, indexes non crees");
            return;
        }
        try {
            ensureIndexes(User.class);
            ensureIndexes(Job.class);
            log.info("Indexes Mongo verifies/crees");
        } catch (Exception e) {
            log.warn("Echec creation indexes Mongo : {}", e.getMessage());
        }
    }

    private void ensureIndexes(Class<?> entity) {
        IndexOperations ops = mongo.indexOps(entity);
        IndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
        resolver.resolveIndexFor(entity).forEach(ops::ensureIndex);
    }
}
