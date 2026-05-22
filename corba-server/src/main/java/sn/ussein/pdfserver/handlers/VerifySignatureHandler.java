package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.File;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

/**
 * Detecte et liste les signatures numeriques d'un PDF.
 *
 * Renvoie un JSON :
 *   { "count": N, "signatures": [ {name, reason, location, date, signer, valid} ] }
 *
 * Note : la "validite" verifie uniquement la conformite cryptographique
 * du PKCS#7 embarque et l'integrite du PDF entre [ByteRange]. Elle ne
 * verifie pas la chaine de confiance du certificat (CA).
 */
public class VerifySignatureHandler {

    private static final Logger log = LoggerFactory.getLogger(VerifySignatureHandler.class);

    public String verifySignature(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                List<PDSignature> signatures = doc.getSignatureDictionaries();
                StringBuilder json = new StringBuilder();
                json.append("{\"count\":").append(signatures.size()).append(",\"signatures\":[");

                for (int i = 0; i < signatures.size(); i++) {
                    PDSignature sig = signatures.get(i);
                    if (i > 0) json.append(',');

                    String name = nullToEmpty(sig.getName());
                    String reason = nullToEmpty(sig.getReason());
                    String location = nullToEmpty(sig.getLocation());
                    String date = formatDate(sig.getSignDate());
                    String signer = extractSignerCN(sig, pdf);
                    boolean valid = verifyIntegrity(sig, pdf);

                    json.append("{")
                        .append("\"name\":\"").append(escape(name)).append("\",")
                        .append("\"reason\":\"").append(escape(reason)).append("\",")
                        .append("\"location\":\"").append(escape(location)).append("\",")
                        .append("\"date\":\"").append(escape(date)).append("\",")
                        .append("\"signer\":\"").append(escape(signer)).append("\",")
                        .append("\"valid\":").append(valid)
                        .append("}");
                }
                json.append("]}");
                log.info("VerifySignature : {} signature(s) detectee(s)", signatures.size());
                return json.toString();
            }
        } catch (Exception e) {
            log.error("Erreur verifySignature", e);
            throw new PDFException("VERIFY_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private String extractSignerCN(PDSignature sig, byte[] pdf) {
        try {
            byte[] sigContent = sig.getContents(pdf);
            // Parse PKCS#7
            org.bouncycastle.cms.CMSSignedData cms = new org.bouncycastle.cms.CMSSignedData(
                new org.bouncycastle.cms.CMSProcessableByteArray(sig.getSignedContent(pdf)),
                sigContent);
            for (Object o : cms.getCertificates().getMatches(null)) {
                org.bouncycastle.cert.X509CertificateHolder holder = (org.bouncycastle.cert.X509CertificateHolder) o;
                String dn = holder.getSubject().toString();
                int cn = dn.indexOf("CN=");
                if (cn >= 0) {
                    int end = dn.indexOf(',', cn);
                    return dn.substring(cn + 3, end < 0 ? dn.length() : end);
                }
                return dn;
            }
        } catch (Exception ignored) {}
        return "";
    }

    private boolean verifyIntegrity(PDSignature sig, byte[] pdf) {
        try {
            byte[] signedContent = sig.getSignedContent(pdf);
            byte[] sigContent = sig.getContents(pdf);
            org.bouncycastle.cms.CMSSignedData cms = new org.bouncycastle.cms.CMSSignedData(
                new org.bouncycastle.cms.CMSProcessableByteArray(signedContent), sigContent);

            for (Object signerObj : cms.getSignerInfos().getSigners()) {
                org.bouncycastle.cms.SignerInformation signer = (org.bouncycastle.cms.SignerInformation) signerObj;
                for (Object certObj : cms.getCertificates().getMatches(signer.getSID())) {
                    org.bouncycastle.cert.X509CertificateHolder holder = (org.bouncycastle.cert.X509CertificateHolder) certObj;
                    if (signer.verify(new org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder()
                            .setProvider("BC").build(holder))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Verify integrite : {}", e.getMessage());
        }
        return false;
    }

    private String formatDate(Calendar cal) {
        if (cal == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
    }
    private String nullToEmpty(String s) { return s == null ? "" : s; }
    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
