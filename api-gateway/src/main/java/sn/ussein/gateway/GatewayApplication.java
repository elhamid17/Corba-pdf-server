package sn.ussein.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Point d'entrée de l'API Gateway Spring Boot.
 * Expose le moteur PDF in-process en REST/JSON sur le port 8080.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
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