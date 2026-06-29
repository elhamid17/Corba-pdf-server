package sn.ussein.gateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sn.ussein.gateway.config.RateLimitProperties;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setAuthMaxRequests(2);
        props.setPdfMaxRequests(3);
        props.setWindowMs(60_000L);
        filter = new RateLimitFilter(props);
        chain = mock(FilterChain.class);
    }

    @Test
    void authEndpoint_underLimit_passesThrough() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        req.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);
        filter.doFilter(req, res, chain);

        verify(chain, times(2)).doFilter(req, res);
        assertNotEquals(429, res.getStatus());
    }

    @Test
    void authEndpoint_overLimit_returns429() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        req.setRemoteAddr("10.0.0.2");
        MockHttpServletResponse res = new MockHttpServletResponse();

        for (int i = 0; i < 3; i++) {
            filter.doFilter(req, res, chain);
        }

        assertEquals(429, res.getStatus());
        assertTrue(res.getContentAsString().contains("Too Many Requests"));
        verify(chain, times(2)).doFilter(req, res);
    }

    @Test
    void pdfEndpoint_rateLimited_separatelyFromAuth() throws ServletException, IOException {
        MockHttpServletRequest authReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        authReq.setRemoteAddr("10.0.0.3");
        MockHttpServletRequest pdfReq = new MockHttpServletRequest("POST", "/api/v1/pdf/merge");
        pdfReq.setRemoteAddr("10.0.0.3");
        MockHttpServletResponse res = new MockHttpServletResponse();

        for (int i = 0; i < 3; i++) {
            filter.doFilter(authReq, res, chain);
        }
        assertEquals(429, res.getStatus());

        res = new MockHttpServletResponse();
        filter.doFilter(pdfReq, res, chain);
        verify(chain, atLeastOnce()).doFilter(pdfReq, res);
        assertNotEquals(429, res.getStatus());
    }

    @Test
    void pingEndpoint_isNotRateLimited() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/pdf/ping");
        req.setRemoteAddr("10.0.0.4");
        MockHttpServletResponse res = new MockHttpServletResponse();

        for (int i = 0; i < 50; i++) {
            filter.doFilter(req, res, chain);
        }

        verify(chain, times(50)).doFilter(req, res);
        assertNotEquals(429, res.getStatus());
    }
}
