package sn.ussein.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.ussein.gateway.web.ApiPaths;
import sn.ussein.pdfengine.PdfEngine;

import java.util.Map;

/**
 * Sonde de disponibilite du moteur PDF.
 */
@Tag(name = "PDF — Ping", description = "Sonde de disponibilite du moteur PDF.")
@RestController
@RequestMapping(ApiPaths.PDF)
public class PingController {

    private final PdfEngine pdfEngine;

    public PingController(PdfEngine pdfEngine) {
        this.pdfEngine = pdfEngine;
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        try {
            String response = pdfEngine.ping();
            return ResponseEntity.ok(Map.of("status", "OK", "server", response));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Moteur PDF indisponible", e);
        }
    }
}
