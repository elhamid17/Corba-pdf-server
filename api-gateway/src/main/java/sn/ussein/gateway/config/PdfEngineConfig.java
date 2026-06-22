package sn.ussein.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sn.ussein.pdfengine.PdfEngine;
import sn.ussein.pdfengine.PdfEngineImpl;

/**
 * Expose le moteur PDF in-process comme bean Spring.
 *
 * <p>Remplace l'ancien client distant : le traitement PDF n'est plus délégué à
 * une seconde JVM via IIOP/IDL mais exécuté dans le même processus que la
 * gateway (cf. ADR-0002, monolithe modulaire).</p>
 */
@Configuration
public class PdfEngineConfig {

    @Bean
    public PdfEngine pdfEngine() {
        return new PdfEngineImpl();
    }
}
