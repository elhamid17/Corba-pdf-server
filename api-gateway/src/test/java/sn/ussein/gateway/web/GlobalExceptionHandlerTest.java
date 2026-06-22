package sn.ussein.gateway.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import sn.ussein.pdf.InvalidPageException;
import sn.ussein.pdf.PDFException;
import sn.ussein.pdf.PasswordException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handlePdfException_returns422() {
        PDFException ex = new PDFException("INVALID_PDF", "Fichier corrompu");

        ResponseEntity<Map<String, Object>> response = handler.handlePdfException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("INVALID_PDF"));
    }

    @Test
    void handleInvalidPage_returns400() {
        InvalidPageException ex = new InvalidPageException();
        ex.requestedPage = 99;
        ex.totalPages = 5;
        ex.message = "Page hors limites";

        ResponseEntity<Map<String, Object>> response = handler.handleInvalidPage(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().get("status"));
    }

    @Test
    void handlePassword_returns400() {
        PasswordException ex = new PasswordException("Mot de passe incorrect");

        ResponseEntity<Map<String, Object>> response = handler.handlePassword(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("Mot de passe"));
    }

    @Test
    void handleQuota_returns413() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleQuota(new QuotaExceededException("Quota depasse"));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
    }
}
