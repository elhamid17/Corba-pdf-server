package sn.ussein.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Quotas par role, charges depuis app.quota.* dans application.yml.
 * Une valeur de -1 sur max-bytes signifie illimite.
 */
@Configuration
@ConfigurationProperties(prefix = "app.quota")
public class QuotaProperties {

    private Guest guest = new Guest();
    private UserQuota user = new UserQuota();
    private Admin admin = new Admin();

    public Guest getGuest() { return guest; }
    public void setGuest(Guest guest) { this.guest = guest; }

    public UserQuota getUser() { return user; }
    public void setUser(UserQuota user) { this.user = user; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public static class Guest {
        private long maxBytes;
        private int retentionHours;
        private int maxFiles;

        public long getMaxBytes() { return maxBytes; }
        public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }

        public int getRetentionHours() { return retentionHours; }
        public void setRetentionHours(int retentionHours) { this.retentionHours = retentionHours; }

        public int getMaxFiles() { return maxFiles; }
        public void setMaxFiles(int maxFiles) { this.maxFiles = maxFiles; }
    }

    public static class UserQuota {
        private long maxBytes;
        private int retentionDays;

        public long getMaxBytes() { return maxBytes; }
        public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    public static class Admin {
        private long maxBytes;

        public long getMaxBytes() { return maxBytes; }
        public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
    }
}
