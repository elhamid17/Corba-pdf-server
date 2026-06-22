package sn.ussein.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Requetes max / minute / IP sur /api/auth/login et /register. */
    private int authMaxRequests = 10;

    /** Requetes max / minute / IP sur /api/pdf/** (hors ping). */
    private int pdfMaxRequests = 30;

    /** Fenetre glissante en millisecondes. */
    private long windowMs = 60_000L;

    public int getAuthMaxRequests() { return authMaxRequests; }
    public void setAuthMaxRequests(int authMaxRequests) { this.authMaxRequests = authMaxRequests; }

    public int getPdfMaxRequests() { return pdfMaxRequests; }
    public void setPdfMaxRequests(int pdfMaxRequests) { this.pdfMaxRequests = pdfMaxRequests; }

    public long getWindowMs() { return windowMs; }
    public void setWindowMs(long windowMs) { this.windowMs = windowMs; }
}
