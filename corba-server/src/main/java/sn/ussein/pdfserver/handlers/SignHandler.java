package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Signe numériquement un PDF avec un certificat PKCS12 (.p12).
 * Utilise BouncyCastle pour la signature CMS/PKCS7.
 */
public class SignHandler {

    private static final Logger log = LoggerFactory.getLogger(SignHandler.class);

    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    public PDFResult sign(byte[] pdf, byte[] certificate,
                           String password, String reason, String location)
            throws PDFException {

        if (pdf == null || pdf.length == 0)
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        if (certificate == null || certificate.length == 0)
            throw new PDFException("INVALID_INPUT", "Le certificat est vide");

        File tmpPdf = null;
        File tmpOut = null;

        try {
            // ── Charger le certificat PKCS12 ──
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(new ByteArrayInputStream(certificate),
                password.toCharArray());

            String alias = keystore.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) keystore.getKey(
                alias, password.toCharArray());
            Certificate[] certChain = keystore.getCertificateChain(alias);
            X509Certificate x509 = (X509Certificate) certChain[0];

            log.info("Certificat chargé — {}", x509.getSubjectDN());

            tmpPdf = FileUtil.bytesToTempFile(pdf, ".pdf");
            tmpOut = FileUtil.createTempFile("_signed.pdf");

            // ── Créer la signature ──
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(x509.getSubjectDN().getName());
            signature.setLocation(location != null ? location : "");
            signature.setReason(reason != null ? reason : "Signature numérique");
            signature.setSignDate(Calendar.getInstance());

            // ── Interface de signature BouncyCastle ──
            final PrivateKey   finalKey   = privateKey;
            final Certificate[] finalChain = certChain;

            SignatureInterface sigInterface = content -> {
                try {
                    List<Certificate> certList = Arrays.asList(finalChain);
                    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

                    ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                        .setProvider("BC").build(finalKey);

                    gen.addSignerInfoGenerator(
                        new JcaSignerInfoGeneratorBuilder(
                            new JcaDigestCalculatorProviderBuilder()
                                .setProvider("BC").build())
                        .build(signer, x509)
                    );
                    gen.addCertificates(new JcaCertStore(certList));

                    // Lire le contenu du stream
                    byte[] buf = content.readAllBytes();
                    CMSProcessableByteArray processable =
                        new CMSProcessableByteArray(buf);
                    CMSSignedData signedData =
                        gen.generate(processable, false);

                    return signedData.getEncoded();
                } catch (Exception e) {
                    throw new IOException("Erreur signature CMS : " + e.getMessage(), e);
                }
            };

            // ── Signer et sauvegarder ──
            try (PDDocument doc = PDDocument.load(tmpPdf);
                 FileOutputStream fos = new FileOutputStream(tmpOut)) {
                doc.addSignature(signature, sigInterface);
                doc.saveIncremental(fos);
            }

            byte[] result = FileUtil.fileToBytes(tmpOut);
            log.info("PDF signé avec succès — {} octets", result.length);

            PDFResult res = new PDFResult();
            res.success = true;
            res.data    = result;
            res.message = "PDF signé numériquement avec succès";
            return res;
        } catch (Exception e) {
            log.error("Erreur lors de la signature PDF", e);
            throw new PDFException("SIGN_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpPdf, tmpOut);
        }
    }
}