package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfengine.model.PasswordException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Deverrouille un PDF protege par mot de passe : ouvre avec le password
 * fourni puis sauve sans aucune restriction de securite.
 *
 * Utile quand on a perdu le besoin de la protection (on a le mot de passe)
 * et qu'on veut un PDF librement editable / imprimable.
 */
public class UnlockHandler {

    private static final Logger log = LoggerFactory.getLogger(UnlockHandler.class);

    public PDFResult unlockPdf(byte[] pdf, String password) throws PDFException, PasswordException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        if (password == null) password = "";

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile, password);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                doc.setAllSecurityToBeRemoved(true);
                doc.save(out);

                log.info("Unlock : protection retiree ({} pages)", doc.getNumberOfPages());
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = "Protection retiree du document";
                return res;
            }
        } catch (InvalidPasswordException e) {
            PasswordException pe = new PasswordException();
            pe.message = "Mot de passe incorrect";
            throw pe;
        } catch (Exception e) {
            log.error("Erreur unlockPdf", e);
            throw new PDFException("UNLOCK_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
