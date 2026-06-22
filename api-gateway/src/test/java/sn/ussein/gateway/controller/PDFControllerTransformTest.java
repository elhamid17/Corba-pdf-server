package sn.ussein.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import sn.ussein.gateway.controller.support.AbstractPDFControllerTest;
import sn.ussein.pdf.WatermarkOptions;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PDFControllerTransformTest extends AbstractPDFControllerTest {

    @Test
    void rotate_invalidAngle_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/rotate")
                .file(file)
                .param("angle", "45"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rotate_success_returnsPdf() throws Exception {
        when(pdfService.rotate(any(), any(), eq(90))).thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/rotate")
                .file(file)
                .param("angle", "90"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void compress_success_returnsPdf() throws Exception {
        when(pdfService.compress(any(), any())).thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/compress").file(file))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void watermark_missingText_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/watermark")
                .file(file)
                .param("text", " "))
            .andExpect(status().isBadRequest());
    }

    @Test
    void watermark_success_returnsPdf() throws Exception {
        when(pdfService.addWatermark(any(), any(WatermarkOptions.class)))
            .thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/watermark")
                .file(file)
                .param("text", "CONFIDENTIEL"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void resize_success_returnsPdf() throws Exception {
        when(pdfService.resizePages(any(), eq("A4"))).thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/resize")
                .file(file)
                .param("targetSize", "A4"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void crop_success_returnsPdf() throws Exception {
        when(pdfService.cropPages(any(), anyFloat(), anyFloat(), anyFloat(), anyFloat()))
            .thenReturn(okPdf(minimalPdfBytes()));

        MockMultipartFile file = new MockMultipartFile(
            "file", "doc.pdf", "application/pdf", minimalPdfBytes());

        mockMvc.perform(multipart("/api/pdf/crop")
                .file(file)
                .param("marginLeft", "5")
                .param("marginTop", "5")
                .param("marginRight", "5")
                .param("marginBottom", "5"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/pdf"));
    }
}
