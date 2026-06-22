package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Retire toutes les metadonnees identifiantes d'un PDF :
 * auteur, titre, sujet, mots-cles, createur, producteur, dates.
 *
 * Note : la structure XMP eventuellement embarquee est aussi vidée
 * pour eviter qu'un lecteur la lise a la place des champs Info.
 */
public class AnonymizeHandler {

    private static final Logger log = LoggerFactory.getLogger(AnonymizeHandler.class);

    public PDFResult anonymize(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                // Vide tous les champs Info
                PDDocumentInformation info = new PDDocumentInformation();
                doc.setDocumentInformation(info);

                // Supprime le flux de metadonnees XMP si present
                doc.getDocumentCatalog().setMetadata(null);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                log.info("Anonymisation : metadonnees retirees ({} pages)",
                    doc.getNumberOfPages());
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = "Metadonnees retirees du document";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur anonymize", e);
            throw new PDFException("ANONYMIZE_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }
}
