package sn.ussein.pdfserver.handlers;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.StyledPdfRenderer;
import sn.ussein.pdfserver.util.StyledPdfRenderer.Block;
import sn.ussein.pdfserver.util.StyledPdfRenderer.BlockType;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generateur de CV PDF avec 3 templates : Classique, Moderne, Compact.
 *
 * Schema JSON attendu :
 * {
 *   "template": "classique" | "moderne" | "compact",   // par defaut "classique"
 *   "accentColor": "#6366f1",                           // hex, par defaut brand
 *   "photo": "data:image/jpeg;base64,..." | "iVBORw...",// base64 optionnel
 *   "name": "Jean Dupont",
 *   "title": "Développeur",
 *   "email": "jean@example.com",
 *   "phone": "+221 77 000 00 00",
 *   "address": "Dakar, Sénégal",
 *   "summary": "...",
 *   "experiences": [ {"period","role","company","description"} ],
 *   "education":   [ {"period","diploma","school","description"} ],
 *   "skills":      ["Java", "..."],
 *   "languages":   ["Francais (natif)", "..."]
 * }
 */
public class CvHandler {

    private static final Logger log = LoggerFactory.getLogger(CvHandler.class);

    public PDFResult generateCv(String cvJson) throws PDFException {
        if (cvJson == null || cvJson.isBlank()) {
            throw new PDFException("INVALID_INPUT", "Les donnees CV sont vides");
        }

        try {
            CvData cv = parseCvData(cvJson);
            if (cv.name == null || cv.name.isBlank()) {
                throw new PDFException("INVALID_INPUT", "Le champ 'name' est requis");
            }

            byte[] pdfBytes;
            switch (cv.template) {
                case "moderne": pdfBytes = renderModerne(cv); break;
                case "compact": pdfBytes = renderCompact(cv); break;
                default:        pdfBytes = renderClassique(cv); break;
            }

            log.info("CV genere pour {} ({} template, {} octets)", cv.name, cv.template, pdfBytes.length);
            PDFResult res = new PDFResult();
            res.success = true;
            res.data = pdfBytes;
            res.message = "CV PDF genere (" + cv.template + ")";
            return res;
        } catch (PDFException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur generateCv", e);
            throw new PDFException("CV_ERROR", e.getMessage());
        }
    }

    /* ═══════════════════════════════════════════════════════════
     *  Template Classique — sobre, conservateur, mono-colonne
     * ═══════════════════════════════════════════════════════════ */

    private byte[] renderClassique(CvData cv) throws Exception {
        List<Block> blocks = new ArrayList<>();
        blocks.add(new Block(BlockType.H1, cv.name));
        if (nb(cv.title)) blocks.add(new Block(BlockType.PARAGRAPH, cv.title));
        String contact = buildContactLine(cv);
        if (nb(contact)) blocks.add(new Block(BlockType.PARAGRAPH, contact));

        if (nb(cv.summary)) {
            blocks.add(new Block(BlockType.H2, "Profil"));
            blocks.add(new Block(BlockType.PARAGRAPH, cv.summary));
        }

        if (!cv.experiences.isEmpty()) {
            blocks.add(new Block(BlockType.H2, "Expérience"));
            for (ExperienceEntry exp : cv.experiences) {
                StringBuilder header = new StringBuilder();
                if (nb(exp.role))    header.append(exp.role);
                if (nb(exp.company)) header.append(nb(exp.role) ? " — " : "").append(exp.company);
                if (nb(exp.period))  header.append(nb(header.toString()) ? "  (" : "(").append(exp.period).append(")");
                if (header.length() > 0) blocks.add(new Block(BlockType.H3, header.toString()));
                if (nb(exp.description)) blocks.add(new Block(BlockType.PARAGRAPH, exp.description));
            }
        }

        if (!cv.education.isEmpty()) {
            blocks.add(new Block(BlockType.H2, "Formation"));
            for (EducationEntry ed : cv.education) {
                StringBuilder header = new StringBuilder();
                if (nb(ed.diploma)) header.append(ed.diploma);
                if (nb(ed.school))  header.append(nb(ed.diploma) ? " — " : "").append(ed.school);
                if (nb(ed.period))  header.append(nb(header.toString()) ? "  (" : "(").append(ed.period).append(")");
                if (header.length() > 0) blocks.add(new Block(BlockType.H3, header.toString()));
                if (nb(ed.description)) blocks.add(new Block(BlockType.PARAGRAPH, ed.description));
            }
        }

        if (!cv.skills.isEmpty()) {
            blocks.add(new Block(BlockType.H2, "Compétences"));
            for (String s : cv.skills) blocks.add(new Block(BlockType.LIST_ITEM, s));
        }
        if (!cv.languages.isEmpty()) {
            blocks.add(new Block(BlockType.H2, "Langues"));
            for (String l : cv.languages) blocks.add(new Block(BlockType.LIST_ITEM, l));
        }

        return StyledPdfRenderer.render(blocks, null);
    }

    /* ═══════════════════════════════════════════════════════════
     *  Template Moderne — sidebar gauche colorée, photo, accent
     * ═══════════════════════════════════════════════════════════ */

    private byte[] renderModerne(CvData cv) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            float pageW = PDRectangle.A4.getWidth();
            float pageH = PDRectangle.A4.getHeight();
            float sidebarW = pageW * 0.32f;
            float mainX = sidebarW + 24;
            float mainW = pageW - mainX - 36;

            Color accent = parseColor(cv.accentColor);
            Color accentLight = lighten(accent, 0.85f);
            Color sidebarBg = lighten(accent, 0.92f);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Fond sidebar
            setColor(cs, sidebarBg, false);
            cs.addRect(0, 0, sidebarW, pageH);
            cs.fill();

            // Photo (cercle / carre arrondi) en haut sidebar
            float photoSize = 90;
            float photoX = (sidebarW - photoSize) / 2;
            float photoY = pageH - 30 - photoSize;
            if (cv.photoBytes != null && cv.photoBytes.length > 0) {
                try {
                    PDImageXObject img = PDImageXObject.createFromByteArray(doc, cv.photoBytes, "photo");
                    // Cercle de fond pour effet visuel
                    setColor(cs, accent, false);
                    cs.fillRect(photoX - 2, photoY - 2, photoSize + 4, photoSize + 4);
                    cs.drawImage(img, photoX, photoY, photoSize, photoSize);
                } catch (Exception e) {
                    log.warn("Photo invalide ignoree : {}", e.getMessage());
                }
            }

            // Sidebar : contact + skills + langues
            float sy = (cv.photoBytes != null ? photoY - 20 : pageH - 40);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            setColor(cs, accent, true);
            cs.newLineAtOffset(20, sy);
            cs.showText("CONTACT");
            cs.endText();
            sy -= 16;

            cs.setStrokingColor(accent);
            cs.setLineWidth(1.5f);
            cs.moveTo(20, sy); cs.lineTo(sidebarW - 20, sy); cs.stroke();
            sy -= 12;

            setColor(cs, Color.BLACK, true);
            String[] contactLines = {
                nb(cv.email) ? "Email : " + cv.email : null,
                nb(cv.phone) ? "Tel. : " + cv.phone : null,
                nb(cv.address) ? "Adresse : " + cv.address : null,
            };
            for (String l : contactLines) {
                if (l == null) continue;
                sy = drawWrappedTextLeft(cs, l, PDType1Font.HELVETICA, 9, 20, sy, sidebarW - 40);
                sy -= 4;
            }
            sy -= 8;

            if (!cv.skills.isEmpty()) {
                sy = drawSidebarSection(cs, "COMPÉTENCES", cv.skills, sy, sidebarW, accent);
            }
            if (!cv.languages.isEmpty()) {
                sy = drawSidebarSection(cs, "LANGUES", cv.languages, sy, sidebarW, accent);
            }

            // Main column : nom + titre + summary + experience + formation
            float my = pageH - 50;
            // Nom en grand
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 26);
            setColor(cs, accent, true);
            cs.newLineAtOffset(mainX, my);
            cs.showText(StyledPdfRenderer.sanitize(cv.name));
            cs.endText();
            my -= 28;

            if (nb(cv.title)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 13);
                setColor(cs, new Color(80, 80, 80), true);
                cs.newLineAtOffset(mainX, my);
                cs.showText(StyledPdfRenderer.sanitize(cv.title));
                cs.endText();
                my -= 24;
            }

            if (nb(cv.summary)) {
                my = drawMainSectionTitle(cs, "Profil", mainX, my, accent);
                my = drawWrappedTextLeft(cs, cv.summary, PDType1Font.HELVETICA, 10.5f, mainX, my, mainW) - 12;
            }
            if (!cv.experiences.isEmpty()) {
                my = drawMainSectionTitle(cs, "Expérience", mainX, my, accent);
                for (ExperienceEntry exp : cv.experiences) {
                    if (my < 80) { cs.close(); page = new PDPage(PDRectangle.A4); doc.addPage(page); cs = new PDPageContentStream(doc, page); my = pageH - 50; }
                    my = drawExperienceItem(cs, exp.role, exp.company, exp.period, exp.description, mainX, my, mainW, accent);
                }
            }
            if (!cv.education.isEmpty()) {
                if (my < 100) { cs.close(); page = new PDPage(PDRectangle.A4); doc.addPage(page); cs = new PDPageContentStream(doc, page); my = pageH - 50; }
                my = drawMainSectionTitle(cs, "Formation", mainX, my, accent);
                for (EducationEntry ed : cv.education) {
                    if (my < 80) { cs.close(); page = new PDPage(PDRectangle.A4); doc.addPage(page); cs = new PDPageContentStream(doc, page); my = pageH - 50; }
                    my = drawExperienceItem(cs, ed.diploma, ed.school, ed.period, ed.description, mainX, my, mainW, accent);
                }
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float drawSidebarSection(PDPageContentStream cs, String title, List<String> items, float startY,
                                     float sidebarW, Color accent) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        setColor(cs, accent, true);
        cs.newLineAtOffset(20, startY);
        cs.showText(StyledPdfRenderer.sanitize(title));
        cs.endText();
        float y = startY - 16;
        cs.setStrokingColor(accent);
        cs.setLineWidth(1.5f);
        cs.moveTo(20, y); cs.lineTo(sidebarW - 20, y); cs.stroke();
        y -= 12;
        setColor(cs, Color.BLACK, true);
        for (String item : items) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 9.5f);
            cs.newLineAtOffset(20, y);
            cs.showText("• " + StyledPdfRenderer.sanitize(item));
            cs.endText();
            y -= 13;
        }
        return y - 8;
    }

    private float drawMainSectionTitle(PDPageContentStream cs, String title, float x, float y, Color accent) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 15);
        setColor(cs, accent, true);
        cs.newLineAtOffset(x, y);
        cs.showText(StyledPdfRenderer.sanitize(title.toUpperCase()));
        cs.endText();
        // Petite ligne décorative dessous
        cs.setStrokingColor(accent);
        cs.setLineWidth(2f);
        cs.moveTo(x, y - 4); cs.lineTo(x + 40, y - 4); cs.stroke();
        return y - 22;
    }

    private float drawExperienceItem(PDPageContentStream cs, String role, String company, String period,
                                     String description, float x, float y, float width, Color accent) throws Exception {
        StringBuilder roleLine = new StringBuilder();
        if (nb(role))    roleLine.append(role);
        if (nb(company)) roleLine.append(nb(role) ? " — " : "").append(company);
        if (roleLine.length() > 0) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
            setColor(cs, Color.BLACK, true);
            cs.newLineAtOffset(x, y);
            cs.showText(StyledPdfRenderer.sanitize(roleLine.toString()));
            cs.endText();
            y -= 13;
        }
        if (nb(period)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 9.5f);
            setColor(cs, accent, true);
            cs.newLineAtOffset(x, y);
            cs.showText(StyledPdfRenderer.sanitize(period));
            cs.endText();
            y -= 12;
        }
        if (nb(description)) {
            setColor(cs, new Color(60, 60, 60), true);
            y = drawWrappedTextLeft(cs, description, PDType1Font.HELVETICA, 10, x, y, width);
        }
        return y - 10;
    }

    /* ═══════════════════════════════════════════════════════════
     *  Template Compact — 2 colonnes, dense, idéal étudiant
     * ═══════════════════════════════════════════════════════════ */

    private byte[] renderCompact(CvData cv) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            float pageW = PDRectangle.A4.getWidth();
            float pageH = PDRectangle.A4.getHeight();
            float margin = 36;
            float gutter = 16;
            float leftW = (pageW - 2 * margin - gutter) * 0.62f;
            float rightW = (pageW - 2 * margin - gutter) * 0.38f;
            float leftX = margin;
            float rightX = margin + leftW + gutter;

            Color accent = parseColor(cv.accentColor);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float headerH = 70;
            float topY = pageH - margin;

            // Header full-width avec nom + titre
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
            setColor(cs, accent, true);
            cs.newLineAtOffset(margin, topY - 10);
            cs.showText(StyledPdfRenderer.sanitize(cv.name));
            cs.endText();

            if (nb(cv.title)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                setColor(cs, new Color(70, 70, 70), true);
                cs.newLineAtOffset(margin, topY - 28);
                cs.showText(StyledPdfRenderer.sanitize(cv.title));
                cs.endText();
            }

            // Ligne contact sous le header
            String contact = buildContactLine(cv);
            if (nb(contact)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8.5f);
                setColor(cs, new Color(90, 90, 90), true);
                cs.newLineAtOffset(margin, topY - 44);
                cs.showText(StyledPdfRenderer.sanitize(contact));
                cs.endText();
            }

            // Ligne d'accent sous le header
            cs.setStrokingColor(accent);
            cs.setLineWidth(2.5f);
            cs.moveTo(margin, topY - headerH);
            cs.lineTo(pageW - margin, topY - headerH);
            cs.stroke();

            float yLeft = topY - headerH - 16;
            float yRight = topY - headerH - 16;

            // Colonne gauche : Profil + Experience + Formation
            if (nb(cv.summary)) {
                yLeft = drawMainSectionTitle(cs, "Profil", leftX, yLeft, accent);
                yLeft = drawWrappedTextLeft(cs, cv.summary, PDType1Font.HELVETICA, 9, leftX, yLeft, leftW) - 8;
            }
            if (!cv.experiences.isEmpty()) {
                yLeft = drawMainSectionTitle(cs, "Expérience", leftX, yLeft, accent);
                for (ExperienceEntry exp : cv.experiences) {
                    if (yLeft < 60) break;
                    yLeft = drawCompactItem(cs, exp.role, exp.company, exp.period, exp.description, leftX, yLeft, leftW, accent);
                }
            }
            if (!cv.education.isEmpty() && yLeft > 80) {
                yLeft = drawMainSectionTitle(cs, "Formation", leftX, yLeft, accent);
                for (EducationEntry ed : cv.education) {
                    if (yLeft < 60) break;
                    yLeft = drawCompactItem(cs, ed.diploma, ed.school, ed.period, ed.description, leftX, yLeft, leftW, accent);
                }
            }

            // Colonne droite : Skills + Langues
            if (!cv.skills.isEmpty()) {
                yRight = drawMainSectionTitle(cs, "Compétences", rightX, yRight, accent);
                for (String s : cv.skills) {
                    if (yRight < 50) break;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9.5f);
                    setColor(cs, Color.BLACK, true);
                    cs.newLineAtOffset(rightX, yRight);
                    cs.showText("• " + StyledPdfRenderer.sanitize(s));
                    cs.endText();
                    yRight -= 13;
                }
                yRight -= 8;
            }
            if (!cv.languages.isEmpty() && yRight > 60) {
                yRight = drawMainSectionTitle(cs, "Langues", rightX, yRight, accent);
                for (String l : cv.languages) {
                    if (yRight < 50) break;
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9.5f);
                    setColor(cs, Color.BLACK, true);
                    cs.newLineAtOffset(rightX, yRight);
                    cs.showText("• " + StyledPdfRenderer.sanitize(l));
                    cs.endText();
                    yRight -= 13;
                }
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float drawCompactItem(PDPageContentStream cs, String role, String company, String period,
                                  String description, float x, float y, float width, Color accent) throws Exception {
        StringBuilder line = new StringBuilder();
        if (nb(role))    line.append(role);
        if (nb(company)) line.append(nb(role) ? " · " : "").append(company);
        if (line.length() > 0) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
            setColor(cs, Color.BLACK, true);
            cs.newLineAtOffset(x, y);
            cs.showText(StyledPdfRenderer.sanitize(line.toString()));
            cs.endText();
            y -= 12;
        }
        if (nb(period)) {
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8.5f);
            setColor(cs, accent, true);
            cs.newLineAtOffset(x, y);
            cs.showText(StyledPdfRenderer.sanitize(period));
            cs.endText();
            y -= 11;
        }
        if (nb(description)) {
            setColor(cs, new Color(60, 60, 60), true);
            y = drawWrappedTextLeft(cs, description, PDType1Font.HELVETICA, 8.5f, x, y, width);
        }
        return y - 6;
    }

    /* ═══════════════════════════════════════════════════════════
     *  Utilitaires : texte wrap, couleurs, parsing
     * ═══════════════════════════════════════════════════════════ */

    private float drawWrappedTextLeft(PDPageContentStream cs, String text, PDType1Font font, float fontSize,
                                      float x, float y, float maxWidth) throws Exception {
        if (!nb(text)) return y;
        String safe = StyledPdfRenderer.sanitize(text);
        float lineH = fontSize * 1.35f;
        List<String> lines = wrapLines(safe, font, fontSize, maxWidth);
        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y - fontSize);
            cs.showText(line);
            cs.endText();
            y -= lineH;
        }
        return y;
    }

    private List<String> wrapLines(String text, PDType1Font font, float fontSize, float maxWidth) throws Exception {
        List<String> result = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            String test = cur.length() == 0 ? w : cur + " " + w;
            float tw = font.getStringWidth(test) / 1000f * fontSize;
            if (tw > maxWidth && cur.length() > 0) {
                result.add(cur.toString());
                cur = new StringBuilder(w);
            } else {
                cur = new StringBuilder(test);
            }
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result;
    }

    private void setColor(PDPageContentStream cs, Color c, boolean nonStroke) throws Exception {
        if (nonStroke) cs.setNonStrokingColor(c);
        else           cs.setNonStrokingColor(c);  // fill = non-stroking
    }

    private Color parseColor(String hex) {
        if (hex == null || hex.isBlank()) return new Color(99, 102, 241);  // brand-500 par defaut
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() == 6) {
            try {
                int r = Integer.parseInt(h.substring(0, 2), 16);
                int g = Integer.parseInt(h.substring(2, 4), 16);
                int b = Integer.parseInt(h.substring(4, 6), 16);
                return new Color(r, g, b);
            } catch (NumberFormatException ignored) {}
        }
        return new Color(99, 102, 241);
    }

    /** Eclaircit une couleur en la melangeant avec du blanc selon le facteur (0=identique, 1=blanc). */
    private Color lighten(Color c, float f) {
        f = Math.max(0, Math.min(1, f));
        int r = (int) (c.getRed()   + (255 - c.getRed())   * f);
        int g = (int) (c.getGreen() + (255 - c.getGreen()) * f);
        int b = (int) (c.getBlue()  + (255 - c.getBlue())  * f);
        return new Color(r, g, b);
    }

    private String buildContactLine(CvData cv) {
        StringBuilder s = new StringBuilder();
        if (nb(cv.email))   s.append(cv.email);
        if (nb(cv.phone))   { if (s.length() > 0) s.append("  ·  "); s.append(cv.phone); }
        if (nb(cv.address)) { if (s.length() > 0) s.append("  ·  "); s.append(cv.address); }
        return s.toString();
    }

    private static boolean nb(String s) { return s != null && !s.isBlank(); }

    /* ═══════════════════════════════════════════════════════════
     *  Parsing JSON (regex basique, suffisant pour les champs simples)
     * ═══════════════════════════════════════════════════════════ */

    private CvData parseCvData(String json) {
        CvData cv = new CvData();
        cv.template    = nz(extractField(json, "template"), "classique").toLowerCase();
        cv.accentColor = extractField(json, "accentColor");
        cv.name    = extractField(json, "name");
        cv.title   = extractField(json, "title");
        cv.email   = extractField(json, "email");
        cv.phone   = extractField(json, "phone");
        cv.address = extractField(json, "address");
        cv.summary = extractField(json, "summary");
        cv.skills    = extractStringArray(json, "skills");
        cv.languages = extractStringArray(json, "languages");

        cv.photoBytes = extractPhotoBytes(json);

        for (String exp : extractObjectsRaw(json, "experiences")) {
            ExperienceEntry e = new ExperienceEntry();
            e.period      = extractField(exp, "period");
            e.role        = extractField(exp, "role");
            e.company     = extractField(exp, "company");
            e.description = extractField(exp, "description");
            cv.experiences.add(e);
        }
        for (String ed : extractObjectsRaw(json, "education")) {
            EducationEntry e = new EducationEntry();
            e.period      = extractField(ed, "period");
            e.diploma     = extractField(ed, "diploma");
            e.school      = extractField(ed, "school");
            e.description = extractField(ed, "description");
            cv.education.add(e);
        }
        return cv;
    }

    /** Decode le champ "photo" depuis JSON. Accepte data URL ou base64 brut. */
    private byte[] extractPhotoBytes(String json) {
        String raw = extractField(json, "photo");
        if (raw == null || raw.isBlank()) return null;
        // Strip data:image/...;base64, prefix
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) raw = raw.substring(comma + 1);
        try { return Base64.getDecoder().decode(raw.replaceAll("\\s", "")); }
        catch (Exception e) {
            log.warn("Photo base64 invalide ignoree : {}", e.getMessage());
            return null;
        }
    }

    private String extractField(String json, String key) {
        if (json == null) return null;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private List<String> extractStringArray(String json, String key) {
        List<String> result = new ArrayList<>();
        if (json == null) return result;
        Pattern arr = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher m = arr.matcher(json);
        if (!m.find()) return result;
        String inside = m.group(1);
        Matcher str = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(inside);
        while (str.find()) result.add(unescape(str.group(1)));
        return result;
    }

    private List<String> extractObjectsRaw(String json, String key) {
        List<String> result = new ArrayList<>();
        if (json == null) return result;
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return result;
        int bracketStart = json.indexOf('[', idx);
        if (bracketStart < 0) return result;

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
    private static String nz(String s, String dflt) { return s == null || s.isBlank() ? dflt : s; }

    /* ═══════════════════════════════════════════════════════════
     *  Modeles de donnees internes
     * ═══════════════════════════════════════════════════════════ */

    private static class CvData {
        String template = "classique";
        String accentColor;
        byte[] photoBytes;
        String name, title, email, phone, address, summary;
        List<String> skills = new ArrayList<>();
        List<String> languages = new ArrayList<>();
        List<ExperienceEntry> experiences = new ArrayList<>();
        List<EducationEntry>  education   = new ArrayList<>();
    }
    private static class ExperienceEntry { String period, role, company, description; }
    private static class EducationEntry  { String period, diploma, school, description; }
}
