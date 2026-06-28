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
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.ussein.gateway.controller.support.PdfResponseSupport;
import sn.ussein.gateway.web.ApiPaths;
import sn.ussein.pdfengine.PdfEngine;

import java.io.IOException;

/**
 * Manipulation de la structure des pages : fusion, decoupage, extraction,
 * suppression, reorganisation, rotation, redimensionnement, recadrage,
 * numerotation.
 */
@Tag(name = "PDF — Organisation",
     description = "Structure des pages : fusion, decoupage, extraction, suppression, "
                 + "reorganisation, rotation, redimensionnement, recadrage, numerotation.")
@RestController
@RequestMapping(ApiPaths.PDF)
public class OrganisationController {

    private static final Logger log = LoggerFactory.getLogger(OrganisationController.class);

    private final PdfEngine pdfEngine;
    private final PdfResponseSupport support;

    public OrganisationController(PdfEngine pdfEngine, PdfResponseSupport support) {
        this.pdfEngine = pdfEngine;
        this.support = support;
    }

    @PostMapping("/merge")
    public ResponseEntity<byte[]> merge(@RequestParam("files") MultipartFile[] files,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        if (files == null || files.length < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins 2 fichiers PDF sont requis");
        }
        long start = System.nanoTime();
        try {
            byte[][] pdfs = new byte[files.length][];
            for (int i = 0; i < files.length; i++) {
                support.validatePdfFile(files[i], "files[" + i + "]");
                pdfs[i] = files[i].getBytes();
            }
            return support.pdfResponse(pdfEngine.merge(pdfs), "merged.pdf", outputName,
                "merge", files[0].getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture des fichiers impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /merge", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la fusion", e);
        }
    }

    @PostMapping("/split")
    public ResponseEntity<byte[]> split(@RequestParam("file") MultipartFile file,
                                        @RequestParam("ranges") int[] ranges,
                                        @RequestParam(required = false) String outputName,
                                        HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        support.validateEvenRanges(ranges, "ranges");
        long start = System.nanoTime();
        try {
            byte[][] parts = pdfEngine.split(file.getBytes(), ranges);
            return support.zipResponse(parts, "split.zip", outputName, "part", ".pdf",
                "split", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /split", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du decoupage", e);
        }
    }

    @PostMapping("/extract-pages")
    public ResponseEntity<byte[]> extractPages(@RequestParam("file") MultipartFile file,
                                               @RequestParam("pages") int[] pages,
                                               @RequestParam(required = false) String outputName,
                                               HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        support.validatePositiveList(pages, "pages");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.extractPages(file.getBytes(), pages), "extracted.pdf", outputName,
                "extract-pages", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /extract-pages", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'extraction", e);
        }
    }

    @PostMapping("/delete-pages")
    public ResponseEntity<byte[]> deletePages(@RequestParam("file") MultipartFile file,
                                              @RequestParam("pages") int[] pages,
                                              @RequestParam(required = false) String outputName,
                                              HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        support.validatePositiveList(pages, "pages");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.deletePages(file.getBytes(), pages), "result.pdf", outputName,
                "delete-pages", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /delete-pages", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression", e);
        }
    }

    @PostMapping("/rotate")
    public ResponseEntity<byte[]> rotate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "90") int angle,
            @RequestParam(required = false) int[] pages,
            @RequestParam(required = false) String outputName,
            HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        if (angle != 90 && angle != 180 && angle != 270) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "angle doit valoir 90, 180 ou 270");
        }
        if (pages != null && pages.length > 0) {
            support.validatePositiveList(pages, "pages");
        }
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.rotate(file.getBytes(),
                pages != null ? pages : new int[]{}, angle), "rotated.pdf", outputName,
                "rotate", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /rotate", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la rotation", e);
        }
    }

    @PostMapping("/reverse")
    public ResponseEntity<byte[]> reverse(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false) String outputName,
                                          HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.reversePages(file.getBytes()), "reversed.pdf", outputName,
                "reverse", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /reverse", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de l'inversion", e);
        }
    }

    @PostMapping("/page-numbers")
    public ResponseEntity<byte[]> pageNumbers(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "bottom-center") String position,
                                              @RequestParam(defaultValue = "%d") String format,
                                              @RequestParam(defaultValue = "1") int startNumber,
                                              @RequestParam(defaultValue = "12") int fontSize,
                                              @RequestParam(required = false) String outputName,
                                              HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(
                pdfEngine.addPageNumbers(file.getBytes(), position, format, startNumber, fontSize),
                "numbered.pdf", outputName, "page-numbers", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /page-numbers", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la numerotation", e);
        }
    }

    @PostMapping("/resize")
    public ResponseEntity<byte[]> resize(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "A4") String targetSize,
                                         @RequestParam(required = false) String outputName,
                                         HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.resizePages(file.getBytes(), targetSize),
                "resized.pdf", outputName, "resize", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /resize", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du redimensionnement", e);
        }
    }

    @PostMapping("/crop")
    public ResponseEntity<byte[]> crop(@RequestParam("file") MultipartFile file,
                                       @RequestParam(defaultValue = "0") float marginLeft,
                                       @RequestParam(defaultValue = "0") float marginTop,
                                       @RequestParam(defaultValue = "0") float marginRight,
                                       @RequestParam(defaultValue = "0") float marginBottom,
                                       @RequestParam(required = false) String outputName,
                                       HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.cropPages(file.getBytes(),
                    marginLeft, marginTop, marginRight, marginBottom),
                "cropped.pdf", outputName, "crop", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /crop", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du recadrage", e);
        }
    }

    @PostMapping("/reorder")
    public ResponseEntity<byte[]> reorder(@RequestParam("file") MultipartFile file,
                                          @RequestParam("order") int[] order,
                                          @RequestParam(required = false) String outputName,
                                          HttpServletRequest request) {
        support.validatePdfFile(file, "file");
        support.validatePositiveList(order, "order");
        long start = System.nanoTime();
        try {
            return support.pdfResponse(pdfEngine.reorderPages(file.getBytes(), order),
                "reordered.pdf", outputName, "reorder", file.getOriginalFilename(), request, start);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lecture du fichier impossible", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur /reorder", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la reorganisation", e);
        }
    }
}
