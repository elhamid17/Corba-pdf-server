package sn.ussein.pdfserver.handlers;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PDFResult;
import sn.ussein.pdfserver.util.StyledPdfRenderer;
import sn.ussein.pdfserver.util.StyledPdfRenderer.Block;
import sn.ussein.pdfserver.util.StyledPdfRenderer.BlockType;

import java.util.ArrayList;
import java.util.List;

/**
 * Convertit du HTML en PDF.
 *
 * Parse avec jsoup pour extraire la structure semantique
 * (h1-h6, p, ul/ol, blockquote, pre/code) et la traduit en Blocks.
 * Les styles CSS et JavaScript sont ignores ; seul le texte structure est rendu.
 */
public class HtmlToPdfHandler {

    private static final Logger log = LoggerFactory.getLogger(HtmlToPdfHandler.class);

    public PDFResult htmlToPdf(String html, String title) throws PDFException {
        if (html == null || html.isBlank()) {
            throw new PDFException("INVALID_INPUT", "Le HTML fourni est vide");
        }

        try {
            Document doc = Jsoup.parse(html);
            List<Block> blocks = new ArrayList<>();

            // Si pas de title fourni, on prend le <title> du HTML
            String docTitle = (title == null || title.isBlank()) ? doc.title() : title;
            if (docTitle != null && docTitle.isBlank()) docTitle = null;

            Element body = doc.body();
            walk(body, blocks, 0);

            byte[] pdfBytes = StyledPdfRenderer.render(blocks, docTitle);

            log.info("HTML -> PDF : {} blocs rendus, {} octets",
                blocks.size(), pdfBytes.length);
            PDFResult res = new PDFResult();
            res.success = true;
            res.data = pdfBytes;
            res.message = "HTML converti en PDF (" + blocks.size() + " blocs)";
            return res;
        } catch (Exception e) {
            log.error("Erreur htmlToPdf", e);
            throw new PDFException("HTML_TO_PDF_ERROR", e.getMessage());
        }
    }

    private void walk(Element root, List<Block> out, int listLevel) {
        for (Node n : root.childNodes()) {
            if (!(n instanceof Element)) continue;
            Element el = (Element) n;
            String tag = el.tagName().toLowerCase();
            switch (tag) {
                case "h1": out.add(new Block(BlockType.H1, el.text())); break;
                case "h2": out.add(new Block(BlockType.H2, el.text())); break;
                case "h3": case "h4": case "h5": case "h6":
                    out.add(new Block(BlockType.H3, el.text())); break;
                case "p": case "div":
                    if (!el.text().isEmpty()) {
                        // Si le div n'a que des elements bloc enfants, on recurse plutot
                        if (el.children().isEmpty() || isOnlyInline(el)) {
                            out.add(new Block(BlockType.PARAGRAPH, el.text()));
                        } else {
                            walk(el, out, listLevel);
                        }
                    }
                    break;
                case "ul": case "ol":
                    walk(el, out, listLevel + 1);
                    break;
                case "li":
                    out.add(new Block(BlockType.LIST_ITEM, el.text(), Math.max(0, listLevel - 1)));
                    walk(el, out, listLevel); // sous-listes eventuelles
                    break;
                case "blockquote":
                    out.add(new Block(BlockType.BLOCKQUOTE, el.text())); break;
                case "pre": case "code":
                    out.add(new Block(BlockType.CODE, el.wholeText())); break;
                case "hr":
                    out.add(new Block(BlockType.PARAGRAPH, "────────────────")); break;
                case "br": break;
                case "script": case "style": break;
                default:
                    walk(el, out, listLevel);
            }
        }
    }

    private boolean isOnlyInline(Element el) {
        for (Node n : el.childNodes()) {
            if (n instanceof TextNode) continue;
            if (n instanceof Element child) {
                String t = child.tagName().toLowerCase();
                // Si c'est un tag bloc, on doit recurse
                if (t.matches("h[1-6]|p|div|ul|ol|li|blockquote|pre|code|hr|table")) return false;
            }
        }
        return true;
    }
}
