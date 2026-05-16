package sn.ussein.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API Gateway Spring Boot.
 * Expose les services CORBA PDF en REST/JSON sur le port 8080.
 */
@SpringBootApplication
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        log.info("═══════════════════════════════════════════");
        log.info("  API Gateway — CORBA PDF Server");
        log.info("  USSEIN L2 AgroTIC");
        log.info("═══════════════════════════════════════════");
        SpringApplication.run(GatewayApplication.class, args);
    }
}