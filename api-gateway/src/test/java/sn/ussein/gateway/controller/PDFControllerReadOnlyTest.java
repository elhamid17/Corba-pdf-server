package sn.ussein.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sn.ussein.gateway.controller.support.AbstractPDFControllerTest;
import sn.ussein.pdfengine.model.PDFMetadata;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PDFControllerReadOnlyTest extends AbstractPDFControllerTest {

    @Test
    void extractText_success_returnsJson() throws Exception {
        when(pdfService.extractText(any())).thenReturn("Hello PDF");

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/extract-text").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text", containsString("Hello")));
    }

    @Test
    void metadata_success_returnsFields() throws Exception {
        PDFMetadata meta = new PDFMetadata();
        meta.title = "Rapport";
        meta.author = "Alice";
        meta.pageCount = 3;
        when(pdfService.getMetadata(any())).thenReturn(meta);

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/metadata").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Rapport"))
            .andExpect(jsonPath("$.author").value("Alice"))
            .andExpect(jsonPath("$.pageCount").value(3));
    }

    @Test
    void pageCount_success_returnsCount() throws Exception {
        when(pdfService.getPageCount(any())).thenReturn(5);

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/page-count").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageCount").value(5));
    }

    @Test
    void stats_success_returnsJsonBody() throws Exception {
        when(pdfService.getDocumentStats(any()))
            .thenReturn("{\"pages\":2,\"words\":10}");

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/stats").file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(content().string(containsString("\"pages\":2")));
    }

    @Test
    void create_missingText_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/pdf/create")
                .param("text", " ")
                .param("title", "Doc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_success_returnsPdf() throws Exception {
        when(pdfService.createFromText(anyString(), anyString()))
            .thenReturn(okPdf(minimalPdfBytes()));

        mockMvc.perform(post("/api/v1/pdf/create")
                .param("text", "Contenu du document")
                .param("title", "Mon doc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }
}
