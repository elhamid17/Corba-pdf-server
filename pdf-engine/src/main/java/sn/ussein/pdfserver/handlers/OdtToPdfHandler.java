package sn.ussein.pdfserver.handlers;

import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.dom.element.text.TextHElement;
import org.odftoolkit.odfdom.dom.element.text.TextListElement;
import org.odftoolkit.odfdom.dom.element.text.TextListItemElement;
import org.odftoolkit.odfdom.dom.element.text.TextPElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sn.ussein.pdfengine.model.PDFException;
import sn.ussein.pdfengine.model.PDFResult;
import sn.ussein.pdfserver.util.StyledPdfRenderer;
import sn.ussein.pdfserver.util.StyledPdfRenderer.Block;
import sn.ussein.pdfserver.util.StyledPdfRenderer.BlockType;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Convertit un document OpenDocument Text (ODT) en PDF.
 *
 * Parcourt le DOM ODT, mappe les balises text:h en titres, text:p en
 * paragraphes, text:list en listes, et reutilise StyledPdfRenderer pour
 * generer le PDF avec mise en forme basique.
 */
public class OdtToPdfHandler {

    private static final Logger log = LoggerFactory.getLogger(OdtToPdfHandler.class);

    public PDFResult odtToPdf(byte[] odt) throws PDFException {
        if (odt == null || odt.length == 0) {
            throw new PDFException("INVALID_INPUT", "Le document ODT fourni est vide");
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(odt)) {
            OdfTextDocument doc = OdfTextDocument.loadDocument(in);

            List<Block> blocks = new ArrayList<>();
            Node body = doc.getContentRoot();
            walk(body, blocks, 0);

            String title = doc.getOfficeMetadata().getTitle();
            byte[] pdfBytes = StyledPdfRenderer.render(blocks,
                (title == null || title.isBlank()) ? null : title);

            doc.close();
            log.info("ODT -> PDF : {} blocs rendus, {} octets",
                blocks.size(), pdfBytes.length);
            PDFResult res = new PDFResult();
            res.success = true;
            res.data = pdfBytes;
            res.message = "ODT converti en PDF (" + blocks.size() + " blocs)";
            return res;
        } catch (Exception e) {
            log.error("Erreur odtToPdf", e);
            throw new PDFException("ODT_TO_PDF_ERROR", e.getMessage());
        }
    }

    private void walk(Node node, List<Block> out, int listLevel) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child instanceof TextHElement h) {
                int level = parseLevel(h.getTextOutlineLevelAttribute());
                BlockType t = level <= 1 ? BlockType.H1 : (level == 2 ? BlockType.H2 : BlockType.H3);
                out.add(new Block(t, h.getTextContent()));
            } else if (child instanceof TextPElement p) {
                String text = p.getTextContent().trim();
                if (!text.isEmpty()) out.add(new Block(BlockType.PARAGRAPH, text));
            } else if (child instanceof TextListElement) {
                walk(child, out, listLevel + 1);
            } else if (child instanceof TextListItemElement) {
                // Un list-item peut contenir des paragraphes
                NodeList sub = child.getChildNodes();
                StringBuilder buf = new StringBuilder();
                for (int j = 0; j < sub.getLength(); j++) {
                    Node s = sub.item(j);
                    if (s instanceof TextPElement) {
                        if (buf.length() > 0) buf.append(' ');
                        buf.append(s.getTextContent());
                    } else if (s instanceof TextListElement) {
                        // sous-liste
                    }
                }
                if (buf.length() > 0) {
                    out.add(new Block(BlockType.LIST_ITEM, buf.toString(), Math.max(0, listLevel - 1)));
                }
                // Recurse pour gerer les sous-listes
                walk(child, out, listLevel);
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                walk(child, out, listLevel);
            }
        }
    }

    private int parseLevel(Integer attr) {
        return attr == null ? 1 : Math.max(1, attr);
    }
}
