package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFMetadata;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Lit et écrit les métadonnées d'un PDF (titre, auteur, sujet, etc.)
 * ainsi que le nombre de pages.
 */
public class MetadataHandler {

    private static final Logger log = LoggerFactory.getLogger(MetadataHandler.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public PDFMetadata getMetadata(byte[] pdf) throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                PDDocumentInformation info = doc.getDocumentInformation();

                PDFMetadata meta = new PDFMetadata();
                meta.title        = nullSafe(info.getTitle());
                meta.author       = nullSafe(info.getAuthor());
                meta.subject      = nullSafe(info.getSubject());
                meta.keywords     = nullSafe(info.getKeywords());
                meta.creator      = nullSafe(info.getCreator());
                meta.producer     = nullSafe(info.getProducer());
                meta.pageCount    = doc.getNumberOfPages();
                meta.creationDate = formatDate(info.getCreationDate());

                log.info("Métadonnées lues — {} pages, titre : '{}'",
                    meta.pageCount, meta.title);
                return meta;
            }
        } catch (Exception e) {
            log.error("Erreur lecture métadonnées", e);
            throw new PDFException("METADATA_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    public PDFResult setMetadata(byte[] pdf, PDFMetadata metadata)
            throws PDFException {

        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;

        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                PDDocumentInformation info = doc.getDocumentInformation();

                if (metadata.title    != null) info.setTitle(metadata.title);
                if (metadata.author   != null) info.setAuthor(metadata.author);
                if (metadata.subject  != null) info.setSubject(metadata.subject);
                if (metadata.keywords != null) info.setKeywords(metadata.keywords);
                if (metadata.creator  != null) info.setCreator(metadata.creator);

                doc.setDocumentInformation(info);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                doc.save(out);

                byte[] result = out.toByteArray();
                log.info("Métadonnées mises à jour avec succès");

                PDFResult res = new PDFResult();
                res.success = true;
                res.data    = result;
                res.message = "Métadonnées mises à jour avec succès";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur écriture métadonnées", e);
            throw new PDFException("METADATA_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    public int getPageCount(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }
        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");
            try (PDDocument doc = PDDocument.load(tmpFile)) {
                return doc.getNumberOfPages();
            }
        } catch (Exception e) {
            throw new PDFException("METADATA_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String formatDate(Calendar cal) {
        if (cal == null) return "";
        return new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());
    }
}