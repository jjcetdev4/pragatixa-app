package jjcet.PragatiX.modules.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    public enum SmsProvider {
        AIRTEL
    }

    private SmsProvider provider = SmsProvider.AIRTEL;
    private boolean testEndpointEnabled = true;

    public SmsProvider getProvider() {
        return provider;
    }

    public void setProvider(SmsProvider provider) {
        this.provider = provider;
    }

    public boolean isTestEndpointEnabled() {
        return testEndpointEnabled;
    }

    public void setTestEndpointEnabled(boolean testEndpointEnabled) {
        this.testEndpointEnabled = testEndpointEnabled;
    }
}
