package sn.ussein.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sn.ussein.gateway.controller.support.AbstractPDFControllerTest;
import sn.ussein.pdfengine.model.PDFMetadata;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PDFControllerOrganisationTest extends AbstractPDFControllerTest {

    @Test
    void split_withOddRangesCount_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/split")
                .file(file)
                .param("ranges", "1", "2", "3"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void split_success_returnsZip() throws Exception {
        byte[] part = minimalPdfBytes();
        when(pdfService.split(any(), any())).thenReturn(new byte[][]{part, part});

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/split")
                .file(file)
                .param("ranges", "1", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"));
    }

    @Test
    void extractPages_success_returnsPdf() throws Exception {
        when(pdfService.extractPages(any(), any())).thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/extract-pages")
                .file(file)
                .param("pages", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void deletePages_emptyPagesList_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/delete-pages")
                .file(file))
            .andExpect(status().isBadRequest());
    }

    @Test
    void reorder_success_returnsPdf() throws Exception {
        when(pdfService.reorderPages(any(), any())).thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/reorder")
                .file(file)
                .param("order", "1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void reverse_success_returnsPdf() throws Exception {
        when(pdfService.reversePages(any())).thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/reverse").file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }
}
