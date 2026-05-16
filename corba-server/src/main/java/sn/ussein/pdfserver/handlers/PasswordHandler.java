package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdf.PasswordException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Protège un PDF par un mot de passe utilisateur et propriétaire.
 *
 * - Mot de passe utilisateur : requis pour ouvrir le PDF
 * - Mot de passe propriétaire : requis pour modifier les permissions
 */
public class PasswordHandler {

    private static final Logger log = LoggerFactory.getLogger(PasswordHandler.class);

    public PDFResult addPassword(byte[] pdf,
                                  String userPassword,
                                  String ownerPassword)
            throws PDFException, PasswordException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (userPassword == null || userPassword.isEmpty()) {
            throw new PDFException("INVALID_INPUT",
                "Le mot de passe utilisateur est requis");
        }
        if (ownerPassword == null || ownerPassword.isEmpty()) {
            ownerPassword = userPassword; // par défaut identique
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {

                // Vérifier si déjà protégé
                if (doc.isEncrypted()) {
                    PasswordException ex = new PasswordException();
                    ex.message = "Ce PDF est déjà protégé par un mot de passe";
                    throw ex;
                }

                // Définir les permissions (lecture seule pour l'utilisateur)
                AccessPermission permissions = new AccessPermission();
                permissions.setCanPrint(true);
                permissions.setCanExtractContent(false);
                permissions.setCanModify(false);
                permissions.setCanFillInForm(false);
                permissions.setCanModifyAnnotations(false);

                // Chiffrement AES 128 bits
                StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    ownerPassword, userPassword, permissions
                );
                policy.setEncryptionKeyLength(128);
                policy.setPreferAES(true);

                doc.protect(policy);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                byte[] result = out.toByteArray();
                log.info("PDF protégé avec succès — chiffrement AES 128 bits");

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "PDF protégé par mot de passe avec succès (AES 128 bits)";
                return res;
            }

        } catch (PasswordException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de la protection du PDF", e);
            throw new PDFException("PASSWORD_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}