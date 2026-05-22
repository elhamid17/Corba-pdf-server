package sn.ussein.pdfserver.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.StyledPdfRenderer;
import sn.ussein.pdfserver.util.StyledPdfRenderer.Block;
import sn.ussein.pdfserver.util.StyledPdfRenderer.BlockType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Genere un CV PDF a partir d'un JSON structure.
 *
 * Schema JSON attendu (champs tous optionnels sauf name) :
 * {
 *   "name": "Jean Dupont",
 *   "title": "Developpeur",
 *   "email": "jean@example.com",
 *   "phone": "+221 77 000 00 00",
 *   "address": "Dakar, Senegal",
 *   "summary": "Court resume",
 *   "experiences": [ {"period": "2020-2023", "role": "...", "company": "...", "description": "..."} ],
 *   "education":   [ {"period": "2018-2020", "diploma": "...", "school": "...", "description": "..."} ],
 *   "skills":      ["Java", "React", "..."],
 *   "languages":   ["Francais (natif)", "Anglais (B2)"]
 * }
 *
 * Parseur JSON volontairement minimal (regex) pour ne pas ajouter de
 * dependance Jackson cote corba-server. Le JSON arrive deja valide depuis
 * le frontend.
 */
public class CvHandler {

    private static final Logger log = LoggerFactory.getLogger(CvHandler.class);

    public PDFResult generateCv(String cvJson) throws PDFException {
        if (cvJson == null || cvJson.isBlank()) {
            throw new PDFException("INVALID_INPUT", "Les donnees CV sont vides");
        }

        try {
            String name = extractField(cvJson, "name");
            if (name == null || name.isBlank()) {
                throw new PDFException("INVALID_INPUT", "Le champ 'name' est requis");
            }
            String title = extractField(cvJson, "title");
            String email = extractField(cvJson, "email");
            String phone = extractField(cvJson, "phone");
            String address = extractField(cvJson, "address");
            String summary = extractField(cvJson, "summary");

            List<String> skills = extractStringArray(cvJson, "skills");
            List<String> languages = extractStringArray(cvJson, "languages");
            List<String> experiences = extractObjectsRaw(cvJson, "experiences");
            List<String> education = extractObjectsRaw(cvJson, "education");

            List<Block> blocks = new ArrayList<>();
            // En-tete
            blocks.add(new Block(BlockType.H1, name));
            if (notBlank(title)) blocks.add(new Block(BlockType.PARAGRAPH, title));
            StringBuilder contact = new StringBuilder();
            if (notBlank(email))   contact.append("📧 ").append(email).append("   ");
            if (notBlank(phone))   contact.append("📞 ").append(phone).append("   ");
            if (notBlank(address)) contact.append("📍 ").append(address);
            if (contact.length() > 0) blocks.add(new Block(BlockType.PARAGRAPH, contact.toString()));

            if (notBlank(summary)) {
                blocks.add(new Block(BlockType.H2, "Profil"));
                blocks.add(new Block(BlockType.PARAGRAPH, summary));
            }

            if (!experiences.isEmpty()) {
                blocks.add(new Block(BlockType.H2, "Expérience"));
                for (String exp : experiences) {
                    String period = extractField(exp, "period");
                    String role = extractField(exp, "role");
                    String company = extractField(exp, "company");
                    String description = extractField(exp, "description");

                    StringBuilder header = new StringBuilder();
                    if (notBlank(role))    header.append(role);
                    if (notBlank(company)) header.append(notBlank(role) ? " — " : "").append(company);
                    if (notBlank(period))  header.append(notBlank(header.toString()) ? "  (" : "(").append(period).append(")");
                    if (header.length() > 0) blocks.add(new Block(BlockType.H3, header.toString()));
                    if (notBlank(description)) blocks.add(new Block(BlockType.PARAGRAPH, description));
                }
            }

            if (!education.isEmpty()) {
                blocks.add(new Block(BlockType.H2, "Formation"));
                for (String ed : education) {
                    String period = extractField(ed, "period");
                    String diploma = extractField(ed, "diploma");
                    String school = extractField(ed, "school");
                    String description = extractField(ed, "description");

                    StringBuilder header = new StringBuilder();
                    if (notBlank(diploma)) header.append(diploma);
                    if (notBlank(school))  header.append(notBlank(diploma) ? " — " : "").append(school);
                    if (notBlank(period))  header.append(notBlank(header.toString()) ? "  (" : "(").append(period).append(")");
                    if (header.length() > 0) blocks.add(new Block(BlockType.H3, header.toString()));
                    if (notBlank(description)) blocks.add(new Block(BlockType.PARAGRAPH, description));
                }
            }

            if (!skills.isEmpty()) {
                blocks.add(new Block(BlockType.H2, "Compétences"));
                for (String s : skills) blocks.add(new Block(BlockType.LIST_ITEM, s));
            }
            if (!languages.isEmpty()) {
                blocks.add(new Block(BlockType.H2, "Langues"));
                for (String l : languages) blocks.add(new Block(BlockType.LIST_ITEM, l));
            }

            byte[] pdfBytes = StyledPdfRenderer.render(blocks, null);
            log.info("CV genere pour {} ({} blocs)", name, blocks.size());

            PDFResult res = new PDFResult();
            res.success = true;
            res.data = pdfBytes;
            res.message = "CV PDF genere";
            return res;
        } catch (PDFException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur generateCv", e);
            throw new PDFException("CV_ERROR", e.getMessage());
        }
    }

    /* ───── Mini JSON parser (regex) — ne couvre pas les cas exotiques. ───── */

    private static final Pattern FIELD_PATTERN_TPL =
        Pattern.compile("\"%s\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private String extractField(String json, String key) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return unescape(m.group(1));
        return null;
    }

    private List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        Pattern arrayP = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = arrayP.matcher(json);
        if (!m.find()) return result;
        String inside = m.group(1);
        Matcher str = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(inside);
        while (str.find()) result.add(unescape(str.group(1)));
        return result;
    }

    /** Extrait les objets dans un tableau "key":[ {...}, {...} ] comme chaines brutes. */
    private List<String> extractObjectsRaw(String json, String key) {
        List<String> result = new ArrayList<>();
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return result;
        int bracketStart = json.indexOf('[', idx);
        if (bracketStart < 0) return result;

        // Parcourt en comptant les accolades
        int depth = 0;
        int objStart = -1;
        int i = bracketStart + 1;
        boolean inString = false;
        boolean escape = false;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) escape = false;
                else if (c == '\\') escape = true;
                else if (c == '"') inString = false;
            } else {
                if (c == '"') inString = true;
                else if (c == '{') { if (depth == 0) objStart = i; depth++; }
                else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart >= 0) {
                        result.add(json.substring(objStart, i + 1));
                        objStart = -1;
                    }
                } else if (c == ']' && depth == 0) break;
            }
            i++;
        }
        return result;
    }

    private String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\t", "\t");
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
