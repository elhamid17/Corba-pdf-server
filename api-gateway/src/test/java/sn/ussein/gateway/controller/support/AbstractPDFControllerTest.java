package sn.ussein.gateway.controller.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sn.ussein.gateway.controller.AnalysisController;
import sn.ussein.gateway.controller.ConversionController;
import sn.ussein.gateway.controller.GenerationController;
import sn.ussein.gateway.controller.OrganisationController;
import sn.ussein.gateway.controller.PingController;
import sn.ussein.gateway.controller.SecurityController;
import sn.ussein.gateway.model.Job;
import sn.ussein.gateway.security.Identity;
import sn.ussein.gateway.security.IdentityResolver;
import sn.ussein.gateway.service.JobStorageService;
import sn.ussein.gateway.web.GlobalExceptionHandler;
import sn.ussein.pdfengine.PdfEngine;
import sn.ussein.pdfengine.model.PDFResult;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractPDFControllerTest {

    @Mock protected JobStorageService storage;
    @Mock protected IdentityResolver identityResolver;
    @Mock protected PdfEngine pdfService;

    protected MockMvc mockMvc;

    @BeforeEach
    void baseSetUp() {
        // Composant transverse partage par tous les controleurs (extrait de l'ancien
        // PDFController monolithique a l'etape 2.1).
        PdfResponseSupport support = new PdfResponseSupport(storage, identityResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PingController(pdfService),
                new OrganisationController(pdfService, support),
                new ConversionController(pdfService, support),
                new SecurityController(pdfService, support),
                new AnalysisController(pdfService, support),
                new GenerationController(pdfService, support))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        // Stubs partagés : les tests de validation renvoient 400 avant d'atteindre
        // le moteur/stockage et ne les consomment donc pas — d'où lenient().
        lenient().when(identityResolver.current(any())).thenReturn(Identity.forGuest("guest-test"));
        lenient().doNothing().when(storage).checkQuota(any(), anyLong());
        Job job = new Job();
        job.setId("job-1");
        lenient().when(storage.recordSuccess(any(), anyString(), any(), any(), any(), any(), anyLong()))
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
