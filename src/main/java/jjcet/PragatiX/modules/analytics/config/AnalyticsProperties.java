package jjcet.PragatiX.modules.analytics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics")
public class AnalyticsProperties {

    private Risk risk = new Risk();
    private Caching caching = new Caching();

    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }

    public Caching getCaching() { return caching; }
    public void setCaching(Caching caching) { this.caching = caching; }

    public static class Risk {
        private double highAttendanceThreshold = 75.0;
        private double mediumAttendanceThreshold = 85.0;

        public double getHighAttendanceThreshold() { return highAttendanceThreshold; }
        public void setHighAttendanceThreshold(double v) { this.highAttendanceThreshold = v; }

        public double getMediumAttendanceThreshold() { return mediumAttendanceThreshold; }
        public void setMediumAttendanceThreshold(double v) { this.mediumAttendanceThreshold = v; }
    }

    public static class Caching {
        private long ttlSeconds = 60;
        private boolean enabled = false;

        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}