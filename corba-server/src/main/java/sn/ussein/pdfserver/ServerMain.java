package sn.ussein.pdfserver;

import org.jacorb.naming.NameServer;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfserver.impl.PDFServiceImpl;

import java.util.Properties;

/**
 * Point d'entrée du serveur CORBA PDF.
 *
 * Séquence de démarrage :
 *   1. Initialiser l'ORB JacORB
 *   2. Obtenir le POA racine et l'activer
 *   3. Créer et enregistrer l'objet PDFService
 *   4. Publier la référence dans le Naming Service
 *   5. Attendre les appels clients (orb.run())
 */
public class ServerMain {

    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    private static final String SERVICE_NAME = "PDFService";

    public static void main(String[] args) {
        log.info("═══════════════════════════════════════");
        log.info("  CORBA PDF Server — USSEIN L2 AgroTIC");
        log.info("═══════════════════════════════════════");

        try {
            // ── 1. Propriétés ORB ──
            Properties props = new Properties();
            props.setProperty("org.omg.CORBA.ORBClass",        "org.jacorb.orb.ORB");
            props.setProperty("org.omg.CORBA.ORBSingletonClass","org.jacorb.orb.ORBSingleton");
            props.setProperty("OAPort", "2809");

            // ── 2. Initialiser l'ORB ──
            ORB orb = ORB.init(args, props);
            log.info("ORB initialisé avec succès");

            // ── 3. Obtenir et activer le POA racine ──
            POA rootPOA = POAHelper.narrow(
                orb.resolve_initial_references("RootPOA")
            );
            rootPOA.the_POAManager().activate();
            log.info("POA racine activé");

            // ── 4. Créer l'implémentation du service ──
            PDFServiceImpl pdfServiceImpl = new PDFServiceImpl();

            // ── 5. Activer l'objet dans le POA ──
            byte[] oid = rootPOA.activate_object(pdfServiceImpl);
            org.omg.CORBA.Object ref = rootPOA.id_to_reference(oid);
            log.info("Objet PDFService activé dans le POA");

            // ── 6. Enregistrer dans le Naming Service ──
            org.omg.CORBA.Object nsObj =
                orb.resolve_initial_references("NameService");
            NamingContextExt nc = NamingContextExtHelper.narrow(nsObj);

            NameComponent[] name = nc.to_name(SERVICE_NAME);
            nc.rebind(name, ref);
            log.info("PDFService enregistré dans le Naming Service sous '{}'", SERVICE_NAME);

            // ── 7. Prêt ──
            log.info("Serveur CORBA PDF prêt — en attente des clients...");
            log.info("Port IIOP : 2809");

            orb.run();

        } catch (Exception e) {
            log.error("Erreur fatale au démarrage du serveur CORBA", e);
            System.exit(1);
        }
    }
}