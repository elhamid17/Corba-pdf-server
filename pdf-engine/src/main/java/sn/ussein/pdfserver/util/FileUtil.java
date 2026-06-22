package sn.ussein.pdfserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Utilitaire de gestion des fichiers temporaires PDF.
 * Chaque opération crée un fichier tmp → traitement → lecture → suppression.
 */
public class FileUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    /**
     * Écrit un tableau d'octets dans un fichier temporaire et retourne ce fichier.
     */
    public static File bytesToTempFile(byte[] data, String suffix) throws IOException {
        String name = "corba_pdf_" + UUID.randomUUID() + suffix;
        File tmp = new File(TMP_DIR, name);
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(data);
        }
        log.debug("Fichier temporaire créé : {}", tmp.getAbsolutePath());
        return tmp;
    }

    /**
     * Lit un fichier et retourne son contenu en tableau d'octets.
     */
    public static byte[] fileToBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    /**
     * Supprime un fichier temporaire silencieusement.
     */
    public static void deleteSilently(File... files) {
        for (File f : files) {
            if (f != null && f.exists()) {
                boolean deleted = f.delete();
                if (!deleted) {
                    log.warn("Impossible de supprimer le fichier temporaire : {}", f.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Crée un fichier temporaire vide avec le suffixe donné.
     */
    public static File createTempFile(String suffix) throws IOException {
        String name = "corba_pdf_" + UUID.randomUUID() + suffix;
        File tmp = new File(TMP_DIR, name);
        if (!tmp.createNewFile()) {
            throw new IOException("Impossible de créer le fichier temporaire : " + tmp);
        }
        return tmp;
    }
}