package sn.ussein.gateway.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sn.ussein.pdf.PDFServiceHelper;
import sn.ussein.pdf.PDFService;

import java.util.Properties;

/**
 * Service Spring qui gère la connexion CORBA vers le corba-server.
 *
 * Cycle de vie :
 *   @PostConstruct → connecte l'ORB au démarrage
 *   @PreDestroy    → libère l'ORB à l'arrêt
 *
 * Les autres services injectent ce bean pour obtenir
 * le proxy PDFService et appeler les méthodes CORBA.
 */
@Service
public class CorbaClientService {

    private static final Logger log = LoggerFactory.getLogger(CorbaClientService.class);

    @Value("${corba.host:localhost}")
    private String corbaHost;

    @Value("${corba.port:2809}")
    private int corbaPort;

    @Value("${corba.service-name:PDFService}")
    private String serviceName;

    private ORB orb;
    private PDFService pdfService;

    @PostConstruct
    public void connect() {
        log.info("Connexion au serveur CORBA — {}:{}", corbaHost, corbaPort);
        try {
            Properties props = new Properties();
            props.setProperty("org.omg.CORBA.ORBClass",
                "org.jacorb.orb.ORB");
            props.setProperty("org.omg.CORBA.ORBSingletonClass",
                "org.jacorb.orb.ORBSingleton");
            props.setProperty("ORBInitRef.NameService",
                "corbaloc::" + corbaHost + ":" + corbaPort + "/NameService");

            orb = ORB.init(new String[]{}, props);

            org.omg.CORBA.Object nsObj =
                orb.resolve_initial_references("NameService");
            NamingContextExt nc = NamingContextExtHelper.narrow(nsObj);

            org.omg.CORBA.Object obj = nc.resolve_str(serviceName);
            pdfService = PDFServiceHelper.narrow(obj);

            // Vérifier la connexion
            String pong = pdfService.ping();
            log.info("Connexion CORBA établie — réponse : {}", pong);

        } catch (Exception e) {
            log.error("Impossible de se connecter au serveur CORBA", e);
            // On ne plante pas le démarrage — on réessaiera à chaque appel
        }
    }

    /**
     * Retourne le proxy PDFService CORBA.
     * Tente une reconnexion si la connexion est perdue.
     */
    public PDFService getPdfService() throws Exception {
        if (pdfService == null) {
            log.warn("Proxy CORBA null — tentative de reconnexion...");
            connect();
        }
        if (pdfService == null) {
            throw new Exception(
                "Serveur CORBA indisponible — "
                + corbaHost + ":" + corbaPort);
        }
        return pdfService;
    }

    @PreDestroy
    public void disconnect() {
        if (orb != null) {
            orb.shutdown(false);
            log.info("ORB déconnecté proprement");
        }
    }
}