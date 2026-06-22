package sn.ussein.gateway.controller.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sn.ussein.gateway.controller.PDFController;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.IdentityResolver;
import sn.ussein.gateway.service.JobStorageService;
import sn.ussein.gateway.web.GlobalExceptionHandler;
import sn.ussein.pdfengine.PdfEngine;
import sn.ussein.pdfengine.model.PDFResult;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractPDFControllerTest {

    @Mock protected JobStorageService storage;
    @Mock protected IdentityResolver identityResolver;
    @Mock protected PdfEngine pdfService;

    protected MockMvc mockMvc;

    @BeforeEach
    void baseSetUp() {
        PDFController controller = new PDFController(pdfService, storage, identityResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        when(identityResolver.current(any())).thenReturn(Identity.forGuest("guest-test"));
        doNothing().when(storage).checkQuota(any(), anyLong());
        Job job = new Job();
        job.setId("job-1");
        when(storage.recordSuccess(any(), anyString(), any(), any(), any(), any(), anyLong()))
            .thenReturn(job);
    }

    protected static byte[] minimalPdfBytes() {
        return "%PDF-1.4\n%%EOF".getBytes();
    }

    protected static PDFResult okPdf(byte[] data) {
        PDFResult ok = new PDFResult();
        ok.success = true;
        ok.data = data;
        ok.message = "ok";
        return ok;
    }
}
