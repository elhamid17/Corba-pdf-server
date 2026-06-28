package sn.ussein.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sn.ussein.gateway.controller.support.AbstractPDFControllerTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PDFControllerTest extends AbstractPDFControllerTest {

    @Test
    void ping_whenCorbaAvailable_returnsOk() throws Exception {
        when(pdfService.ping()).thenReturn("CORBA PDF Server OK");

        mockMvc.perform(get("/api/v1/pdf/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.server").value("CORBA PDF Server OK"));
    }

    @Test
    void merge_withLessThanTwoFiles_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "files", "a.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/merge").file(file))
            .andExpect(status().isBadRequest());
    }

    @Test
    void merge_success_returnsPdfAttachment() throws Exception {
        when(pdfService.merge(org.mockito.ArgumentMatchers.any()))
            .thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile f1 = new MockMultipartFile("files", "a.pdf", "application/pdf", minimalPdfBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "b.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/v1/pdf/merge").file(f1).file(f2))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(content().contentType("application/pdf"));
    }
}
