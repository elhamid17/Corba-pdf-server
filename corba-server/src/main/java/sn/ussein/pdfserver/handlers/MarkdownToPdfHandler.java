package sn.ussein.pdfserver.handlers;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
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
 * Convertit du Markdown en PDF.
 *
 * Parse via commonmark-java pour obtenir un AST, puis traduit chaque
 * type de noeud en Block pour StyledPdfRenderer.
 */
public class MarkdownToPdfHandler {

    private static final Logger log = LoggerFactory.getLogger(MarkdownToPdfHandler.class);

    public PDFResult markdownToPdf(String markdown, String title) throws PDFException {
        if (markdown == null || markdown.isBlank()) {
            throw new PDFException("INVALID_INPUT", "Le markdown fourni est vide");
        }

        try {
            Parser parser = Parser.builder().build();
            Node ast = parser.parse(markdown);

            List<Block> blocks = new ArrayList<>();
            ast.accept(new MarkdownToBlocksVisitor(blocks));

            byte[] pdfBytes = StyledPdfRenderer.render(blocks,
                (title == null || title.isBlank()) ? null : title);

            log.info("Markdown -> PDF : {} blocs rendus, {} octets",
                blocks.size(), pdfBytes.length);
            PDFResult res = new PDFResult();
            res.success = true;
            res.data = pdfBytes;
            res.message = "Markdown converti en PDF (" + blocks.size() + " blocs)";
            return res;
        } catch (Exception e) {
            log.error("Erreur markdownToPdf", e);
            throw new PDFException("MARKDOWN_TO_PDF_ERROR", e.getMessage());
        }
    }

    /** Walker AST : accumule des Blocks. */
    private static class MarkdownToBlocksVisitor extends AbstractVisitor {
        private final List<Block> blocks;
        private int listLevel = -1;
        MarkdownToBlocksVisitor(List<Block> blocks) { this.blocks = blocks; }

        @Override public void visit(Heading h) {
            String text = inlineText(h);
            BlockType t = switch (h.getLevel()) {
                case 1 -> BlockType.H1;
                case 2 -> BlockType.H2;
                default -> BlockType.H3;
            };
            blocks.add(new Block(t, text));
        }
        @Override public void visit(Paragraph p) {
            blocks.add(new Block(BlockType.PARAGRAPH, inlineText(p)));
        }
        @Override public void visit(BulletList l) {
            listLevel++;
            visitChildren(l);
            listLevel--;
        }
        @Override public void visit(OrderedList l) {
            listLevel++;
            visitChildren(l);
            listLevel--;
        }
        @Override public void visit(ListItem li) {
            // Concatene le texte des paragraphes enfants
            StringBuilder sb = new StringBuilder();
            Node child = li.getFirstChild();
            while (child != null) {
                if (child instanceof Paragraph) sb.append(inlineText(child));
                child = child.getNext();
            }
            blocks.add(new Block(BlockType.LIST_ITEM, sb.toString(), Math.max(0, listLevel)));
        }
        @Override public void visit(BlockQuote b) {
            blocks.add(new Block(BlockType.BLOCKQUOTE, inlineText(b)));
        }
        @Override public void visit(FencedCodeBlock c) {
            blocks.add(new Block(BlockType.CODE, c.getLiteral()));
        }
        @Override public void visit(IndentedCodeBlock c) {
            blocks.add(new Block(BlockType.CODE, c.getLiteral()));
        }
        @Override public void visit(ThematicBreak t) {
            blocks.add(new Block(BlockType.PARAGRAPH, "────────────────"));
        }

        private String inlineText(Node node) {
            StringBuilder sb = new StringBuilder();
            node.accept(new AbstractVisitor() {
                @Override public void visit(Text t) { sb.append(t.getLiteral()); }
                @Override public void visit(Code c) { sb.append(c.getLiteral()); }
                @Override public void visit(HardLineBreak hb) { sb.append(' '); }
                @Override public void visit(SoftLineBreak sb_) { sb.append(' '); }
                @Override public void visit(Link l) { visitChildren(l); }
                @Override public void visit(Image i) { /* skip */ }
            });
            return sb.toString().trim();
        }
    }
}
