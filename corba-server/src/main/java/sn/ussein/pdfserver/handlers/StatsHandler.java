package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdfserver.util.FileUtil;

import java.io.File;
import java.util.Calendar;

/**
 * Calcule des statistiques sur un PDF : pages, mots, caracteres, lignes,
 * paragraphes, temps de lecture estime, langue probable, taille fichier.
 *
 * Renvoie un JSON consommable par le frontend.
 */
public class StatsHandler {

    private static final Logger log = LoggerFactory.getLogger(StatsHandler.class);

    private static final int WORDS_PER_MINUTE = 250;

    public String getDocumentStats(byte[] pdf) throws PDFException {
        if (pdf == null || pdf.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le PDF fourni est vide");
        }

        File tmpFile = null;
        try {
            tmpFile = FileUtil.bytesToTempFile(pdf, ".pdf");

            try (PDDocument doc = PDDocument.load(tmpFile)) {
                int pages = doc.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(doc);

                int chars = text.length();
                int charsNoSpace = text.replaceAll("\\s", "").length();
                int words = countWords(text);
                int lines = (int) text.lines().filter(l -> !l.isBlank()).count();
                int paragraphs = countParagraphs(text);
                int readingMinutes = Math.max(1, words / WORDS_PER_MINUTE);

                String language = detectLanguage(text);

                PDDocumentInformation info = doc.getDocumentInformation();
                String title = info.getTitle() == null ? "" : info.getTitle();
                String author = info.getAuthor() == null ? "" : info.getAuthor();
                String creator = info.getCreator() == null ? "" : info.getCreator();
                Calendar created = info.getCreationDate();

                StringBuilder json = new StringBuilder();
                json.append("{")
                    .append("\"pages\":").append(pages).append(",")
                    .append("\"characters\":").append(chars).append(",")
                    .append("\"charactersNoSpace\":").append(charsNoSpace).append(",")
                    .append("\"words\":").append(words).append(",")
                    .append("\"lines\":").append(lines).append(",")
                    .append("\"paragraphs\":").append(paragraphs).append(",")
                    .append("\"readingMinutes\":").append(readingMinutes).append(",")
                    .append("\"sizeBytes\":").append(pdf.length).append(",")
                    .append("\"language\":\"").append(language).append("\",")
                    .append("\"title\":\"").append(escape(title)).append("\",")
                    .append("\"author\":\"").append(escape(author)).append("\",")
                    .append("\"creator\":\"").append(escape(creator)).append("\",")
                    .append("\"creationDate\":\"")
                    .append(created == null ? "" : escape(new java.text.SimpleDateFormat("yyyy-MM-dd").format(created.getTime())))
                    .append("\"")
                    .append("}");
                log.info("Stats : {} mots / {} caracteres / {} pages", words, chars, pages);
                return json.toString();
            }
        } catch (Exception e) {
            log.error("Erreur getDocumentStats", e);
            throw new PDFException("STATS_ERROR", e.getMessage());
        } finally {
            FileUtil.deleteSilently(tmpFile);
        }
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    private int countParagraphs(String text) {
        // Un paragraphe = bloc separe par au moins une ligne vide
        if (text == null || text.isBlank()) return 0;
        return text.split("\\r?\\n\\s*\\r?\\n").length;
    }

    /** Detection naive de la langue : compare la frequence de mots-cles tres communs. */
    private String detectLanguage(String text) {
        if (text == null || text.length() < 50) return "indeterminee";
        String t = " " + text.toLowerCase() + " ";
        int fr = count(t, " le ") + count(t, " la ") + count(t, " les ") + count(t, " un ") + count(t, " et ") + count(t, " est ") + count(t, " des ") + count(t, " que ");
        int en = count(t, " the ") + count(t, " and ") + count(t, " is ") + count(t, " of ") + count(t, " to ") + count(t, " in ") + count(t, " for ") + count(t, " a ");
        int es = count(t, " el ") + count(t, " la ") + count(t, " que ") + count(t, " los ") + count(t, " y ") + count(t, " es ");
        int de = count(t, " der ") + count(t, " die ") + count(t, " und ") + count(t, " ist ") + count(t, " mit ");

        int max = Math.max(Math.max(fr, en), Math.max(es, de));
        if (max < 3) return "indeterminee";
        if (max == fr) return "francais";
        if (max == en) return "anglais";
        if (max == es) return "espagnol";
        if (max == de) return "allemand";
        return "indeterminee";
    }

    private int count(String text, String token) {
        int n = 0, i = 0;
        while ((i = text.indexOf(token, i)) >= 0) { n++; i += token.length(); }
        return n;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
