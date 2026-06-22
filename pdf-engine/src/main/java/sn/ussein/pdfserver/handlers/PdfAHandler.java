package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Marque un PDF comme PDF/A-1B via XMP metadata (RDF/XML inline)
 * et pose le marqueur d'identification PDF/A dans le catalog.
 *
 * Note : la conformite stricte PDF/A-1B exige aussi que toutes les fontes
 * soient embarquees, qu'aucun JavaScript ne soit present, qu'un profil
 * ICC OutputIntent soit declare, etc. Cette implementation ajoute le
 * marqueur d'intention — un validateur (veraPDF) reconnaitra le tag mais
 * pourra pointer des non-conformites cote contenu si le PDF source ne
 * respecte pas les regles d'archivage.
 */
public class PdfAHandler {

    private static final Logger log = LoggerFactory.getLogger(PdfAHandler.class);

    private static final String XMP_TEMPLATE =
        "<?xpacket begin=\"﻿\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n" +
        "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"CORBA PDF Server\">\n" +
        " <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
        "  <rdf:Description rdf:about=\"\"\n" +
        "    xmlns:pdfaid=\"http://www.aiim.org/pdfa/ns/id/\"\n" +
        "    xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\"\n" +
        "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
        "    xmlns:pdf=\"http://ns.adobe.com/pdf/1.3/\">\n" +
        "   <pdfaid:part>1</pdfaid:part>\n" +
        "   <pdfaid:conformance>B</pdfaid:conformance>\n" +
        "   <xmp:CreatorTool>%CREATOR%</xmp:CreatorTool>\n" +
        "   <xmp:CreateDate>%NOW%</xmp:CreateDate>\n" +
        "   <xmp:ModifyDate>%NOW%</xmp:ModifyDate>\n" +
        "   <dc:title><rdf:Alt><rdf:li xml:lang=\"x-default\">%TITLE%</rdf:li></rdf:Alt></dc:title>\n" +
        "   <dc:creator><rdf:Seq><rdf:li>%AUTHOR%</rdf:li></rdf:Seq></dc:creator>\n" +
        "   <pdf:Producer>CORBA PDF Server — PDF/A-1B</pdf:Producer>\n" +
        "  </rdf:Description>\n" +
        " </rdf:RDF>\n" +
        "</x:xmpmeta>\n" +
        "<?xpacket end=\"w\"?>\n";

    public PDFResult convertToPdfA(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {

                PDDocumentCatalog catalog = doc.getDocumentCatalog();
                PDDocumentInformation info = doc.getDocumentInformation();

                String title = nullSafe(info.getTitle(), "Document");
                String author = nullSafe(info.getAuthor(), "");
                String creator = nullSafe(info.getCreator(), "CORBA PDF Server");
                String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());

                String xmp = XMP_TEMPLATE
                    .replace("%TITLE%", escapeXml(title))
                    .replace("%AUTHOR%", escapeXml(author))
                    .replace("%CREATOR%", escapeXml(creator))
                    .replace("%NOW%", now);

                PDMetadata metadata = new PDMetadata(doc);
                metadata.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));
                catalog.setMetadata(metadata);

                // Marqueur dans le catalog pour bien indiquer l'intention PDF/A
                catalog.getCOSObject().setName(COSName.getPDFName("PDFA"), "PDFA-1B");

                doc.save(out);

                log.info("PDF/A : marqueur PDFA-1B injecte ({} pages)", doc.getNumberOfPages());
                PDFResult res = new PDFResult();
                res.success = true;
                res.data = out.toByteArray();
                res.message = "Document marque PDF/A-1B (verifier la conformite stricte avec un validateur)";
                return res;
            }
        } catch (Exception e) {
            log.error("Erreur convertToPdfA", e);
            throw new PDFException("PDFA_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private String nullSafe(String s, String def) { return s == null || s.isBlank() ? def : s; }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
