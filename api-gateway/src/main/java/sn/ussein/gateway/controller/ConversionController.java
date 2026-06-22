package sn.ussein.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import sn.ussein.gateway.controller.support.PdfResponseSupport;
import sn.ussein.pdfengine.PdfEngine;
import sn.ussein.pdfengine.model.CompressOptions;
import sn.ussein.pdfengine.model.ConvertOptions;
import sn.ussein.pdfengine.model.PDFResult;

import java.io.IOException;

/**
 * Conversions entre formats (PDF <-> Word/Excel/PowerPoint/Markdown/HTML/ODT/images),
 * compression et conversion PDF/A.
 */
@RestController
@RequestMapping("/api/pdf")
public class ConversionController {

    private static final Logger log = LoggerFactory.getLogger(ConversionController.class);

    private final PdfEngine pdfEngine;
    private final PdfResponseSupport support;

    public ConversionController(PdfEngine pdfEngine, PdfResponseSupport support) {
        this.pdfEngine = pdfEngine;
        this.support = support;
    }

    @PostMapping("/compress")
    public ResponseEntity<byte[]> compress(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean compressImages,
            @RequestParam(defaultValue = "70") int imageQuality,
            @RequestParam(defaultValue = "false") boolean removeMetadata,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (imageQuality < 0 || imageQuality > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageQuality doit etre entre 0 et 100");
        }
        long start = System.nanoTime();
        try {
            CompressOptions opts = new CompressOptions();
            opts.compressImages = compressImages;
            opts.imageQuality = imageQuality;
            opts.removeMetadata = removeMetadata;
            return support.pdfResponse(pdfEngine.compress(file.getBytes(), opts), "compressed.pdf", outputName,
                "compress", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /compress", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la compression", e);
        }
    }

    @PostMapping("/convert-to-images")
    public ResponseEntity<byte[]> convertToImages(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PNG") String format,
            @RequestParam(defaultValue = "150") int dpi,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (dpi <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dpi doit etre > 0");
        }
        long start = System.nanoTime();
        try {
            ConvertOptions opts = new ConvertOptions();
            opts.format = format;
            opts.dpi = dpi;
            byte[][] images = pdfEngine.convertToImages(file.getBytes(), opts);
            String ext = "." + format.toLowerCase();
            return support.zipResponse(images, "images.zip", outputName, "page", ext,
                "convert-to-images", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /convert-to-images", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion", e);
        }
    }

    @PostMapping("/pdf-to-word")
    public ResponseEntity<byte[]> pdfToWord(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            PDFResult r = pdfEngine.pdfToWord(file.getBytes());
            return support.binaryResponse(r, "converted.docx", outputName,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "pdf-to-word", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /pdf-to-word", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion PDF -> Word", e);
        }
    }

    @PostMapping("/pdf-to-excel")
    public ResponseEntity<byte[]> pdfToExcel(@RequestParam("file") MultipartFile file,
                                             @RequestParam(required = false) String outputName,
                                             HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            PDFResult r = pdfEngine.pdfToExcel(file.getBytes());
            return support.binaryResponse(r, "converted.xlsx", outputName,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "pdf-to-excel", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /pdf-to-excel", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion PDF -> Excel", e);
        }
    }

    @PostMapping("/word-to-pdf")
    public ResponseEntity<byte[]> wordToPdf(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file est requis");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".docx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier doit avoir l'extension .docx");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.wordToPdf(file.getBytes()), "converted.pdf", outputName,
                "word-to-pdf", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /word-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion Word -> PDF", e);
        }
    }

    @PostMapping("/images-to-pdf")
    public ResponseEntity<byte[]> imagesToPdf(@RequestParam("files") MultipartFile[] files,
                                              @RequestParam(required = false) String outputName,
                                              HttpServletRequest request) {
        if (files == null || files.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins une image est requise");
        }
        long start = System.nanoTime();
        try {
            byte[][] images = new byte[files.length][];
            for (int i = 0; i < files.length; i++) {
                MultipartFile f = files[i];
                if (f == null || f.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "files[" + i + "] est vide");
                }
                String ct = f.getContentType();
                if (ct == null || !ct.startsWith("image/")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "files[" + i + "] doit etre une image (JPG/PNG)");
                }
                images[i] = f.getBytes();
            }
            return support.pdfResponse(pdfEngine.imagesToPdf(images), "images.pdf", outputName,
                "images-to-pdf", files[0].getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /images-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion Images -> PDF", e);
        }
    }

    @PostMapping("/pdf-to-pptx")
    public ResponseEntity<byte[]> pdfToPptx(@RequestParam("file") MultipartFile file,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            PDFResult r = pdfEngine.pdfToPptx(file.getBytes());
            return support.binaryResponse(r, "converted.pptx", outputName,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "pdf-to-pptx", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /pdf-to-pptx", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion PDF -> PowerPoint", e);
        }
    }

    @PostMapping("/pdf-to-markdown")
    public ResponseEntity<byte[]> pdfToMarkdown(@RequestParam("file") MultipartFile file,
                                                @RequestParam(required = false) String outputName,
                                                HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            PDFResult r = pdfEngine.pdfToMarkdown(file.getBytes());
            return support.binaryResponse(r, "converted.md", outputName, "text/markdown",
                "pdf-to-markdown", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /pdf-to-markdown", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion PDF -> Markdown", e);
        }
    }

    @PostMapping("/markdown-to-pdf")
    public ResponseEntity<byte[]> markdownToPdf(@RequestParam String markdown,
                                                @RequestParam(defaultValue = "Document") String title,
                                                @RequestParam(required = false) String outputName,
                                                HttpServletRequest request) {
        if (markdown == null || markdown.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "markdown est requis");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.markdownToPdf(markdown, title),
                "from-markdown.pdf", outputName, "markdown-to-pdf", title, request, start);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /markdown-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion Markdown -> PDF", e);
        }
    }

    @PostMapping("/html-to-pdf")
    public ResponseEntity<byte[]> htmlToPdf(@RequestParam String html,
                                            @RequestParam(defaultValue = "Document") String title,
                                            @RequestParam(required = false) String outputName,
                                            HttpServletRequest request) {
        if (html == null || html.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "html est requis");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.htmlToPdf(html, title),
                "from-html.pdf", outputName, "html-to-pdf", title, request, start);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /html-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion HTML -> PDF", e);
        }
    }

    @PostMapping("/excel-to-pdf")
    public ResponseEntity<byte[]> excelToPdf(@RequestParam("file") MultipartFile file,
                                             @RequestParam(required = false) String outputName,
                                             HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file est requis");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".xlsx")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier doit etre un .xlsx");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.excelToPdf(file.getBytes()),
                "from-excel.pdf", outputName, "excel-to-pdf", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /excel-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion Excel -> PDF", e);
        }
    }

    @PostMapping("/odt-to-pdf")
    public ResponseEntity<byte[]> odtToPdf(@RequestParam("file") MultipartFile file,
                                           @RequestParam(required = false) String outputName,
                                           HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file est requis");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".odt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier doit etre un .odt");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.odtToPdf(file.getBytes()),
                "from-odt.pdf", outputName, "odt-to-pdf", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /odt-to-pdf", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion ODT -> PDF", e);
        }
    }

    @PostMapping("/to-pdfa")
    public ResponseEntity<byte[]> toPdfA(@RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) String outputName,
                                         HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.convertToPdfA(file.getBytes()),
                "archive.pdf", outputName, "to-pdfa", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /to-pdfa", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur conversion PDF/A", e);
        }
    }
}
