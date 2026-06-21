package sn.ussein.pdfserver.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper pour rendre une suite de "blocs" structures (titres, paragraphes,
 * listes, code, blockquote) en PDF avec mise en page basique.
 *
 * Reutilise par MarkdownToPdfHandler, HtmlToPdfHandler, OdtToPdfHandler.
 * Limitations : fonte mono Helvetica/Courier (WinAnsi uniquement),
 * pas d'images, pas de tableaux.
 */
public final class StyledPdfRenderer {

    public enum BlockType { H1, H2, H3, PARAGRAPH, LIST_ITEM, CODE, BLOCKQUOTE }

    public static final class Block {
        public final BlockType type;
        public final String text;
        public final int listLevel; // 0 = top, 1 = nested
        public Block(BlockType type, String text) { this(type, text, 0); }
        public Block(BlockType type, String text, int listLevel) {
            this.type = type;
            this.text = text;
            this.listLevel = listLevel;
        }
    }

    private static final float MARGIN = 50f;
    private static final float CODE_BG_GRAY = 0.95f;

    public static byte[] render(List<Block> blocks, String title) throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            float pageWidth = PDRectangle.A4.getWidth();
            float pageHeight = PDRectangle.A4.getHeight();
            float maxWidth = pageWidth - 2 * MARGIN;

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float y = pageHeight - MARGIN;

            // Titre du document si fourni
            if (title != null && !title.isBlank()) {
                y = drawTextWrapped(cs, doc, page, sanitize(title),
                    PDType1Font.HELVETICA_BOLD, 20f, MARGIN, y, maxWidth, 26f);
                y -= 10f;
            }

            for (Block b : blocks) {
                if (y < MARGIN + 30) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageHeight - MARGIN;
                }
                y = renderBlock(cs, doc, page, b, MARGIN, y, maxWidth, pageHeight);
            }
            cs.close();

            doc.save(out);
            return out.toByteArray();
        }
    }

    private static float renderBlock(PDPageContentStream cs, PDDocument doc, PDPage page,
                                     Block b, float marginX, float y, float maxWidth, float pageHeight) throws Exception {
        switch (b.type) {
            case H1: return drawTextWrapped(cs, doc, page, sanitize(b.text),
                PDType1Font.HELVETICA_BOLD, 18f, marginX, y - 6, maxWidth, 22f) - 6f;
            case H2: return drawTextWrapped(cs, doc, page, sanitize(b.text),
                PDType1Font.HELVETICA_BOLD, 16f, marginX, y - 4, maxWidth, 20f) - 4f;
            case H3: return drawTextWrapped(cs, doc, page, sanitize(b.text),
                PDType1Font.HELVETICA_BOLD, 13f, marginX, y - 3, maxWidth, 17f) - 3f;
            case PARAGRAPH: return drawTextWrapped(cs, doc, page, sanitize(b.text),
                PDType1Font.HELVETICA, 11f, marginX, y, maxWidth, 15f) - 4f;
            case LIST_ITEM: {
                float indent = marginX + b.listLevel * 18f;
                // Puce
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11f);
                cs.newLineAtOffset(indent, y - 11f);
                cs.showText("•");
                cs.endText();
                return drawTextWrapped(cs, doc, page, sanitize(b.text),
                    PDType1Font.HELVETICA, 11f, indent + 14f, y, maxWidth - (indent + 14f - marginX), 15f) - 2f;
            }
            case CODE: {
                // Fond gris clair + Courier
                float lineHeight = 13f;
                String[] lines = b.text.split("\\r?\\n");
                float boxHeight = lineHeight * lines.length + 8f;
                cs.setNonStrokingColor(CODE_BG_GRAY);
                cs.addRect(marginX - 4, y - boxHeight + 4, maxWidth + 8, boxHeight);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
                float yy = y - 11f;
                for (String l : lines) {
                    cs.beginText();
                    cs.setFont(PDType1Font.COURIER, 10f);
                    cs.newLineAtOffset(marginX, yy);
                    cs.showText(sanitize(l));
                    cs.endText();
                    yy -= lineHeight;
                }
                return yy - 4f;
            }
            case BLOCKQUOTE: {
                // Barre verticale a gauche + texte en italique
                cs.setNonStrokingColor(0.7f);
                cs.addRect(marginX, y - 16f, 3f, 12f);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
                return drawTextWrapped(cs, doc, page, sanitize(b.text),
                    PDType1Font.HELVETICA_OBLIQUE, 11f, marginX + 10f, y, maxWidth - 10f, 15f) - 4f;
            }
            default: return y;
        }
    }

    /**
     * Dessine un texte avec wrap automatique et retourne le nouveau y.
     * Note : ne gere pas le saut de page intra-bloc (le bloc complet doit tenir).
     */
    private static float drawTextWrapped(PDPageContentStream cs, PDDocument doc, PDPage page,
                                         String text, PDType1Font font, float fontSize,
                                         float x, float startY, float maxWidth, float leading) throws Exception {
        if (text == null || text.isEmpty()) return startY - leading;
        List<String> lines = wrap(text, font, fontSize, maxWidth);
        float y = startY;
        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(x, y - fontSize);
            cs.showText(line);
            cs.endText();
            y -= leading;
        }
        return y;
    }

    private static List<String> wrap(String text, PDType1Font font, float fontSize, float maxWidth) throws Exception {
        List<String> result = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            String test = current.length() == 0 ? w : current + " " + w;
            float tw = font.getStringWidth(test) / 1000f * fontSize;
            if (tw > maxWidth && current.length() > 0) {
                result.add(current.toString());
                current = new StringBuilder(w);
            } else {
                current = new StringBuilder(test);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
    }

    /** Filtre les caracteres hors WinAnsi (limitation Helvetica/Courier built-in). */
    public static String sanitize(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c < 0x20 || c == 0x7F) continue;
            sb.append(c < 0x100 ? c : '?');
        }
        return sb.toString();
    }

    private StyledPdfRenderer() {}
}
