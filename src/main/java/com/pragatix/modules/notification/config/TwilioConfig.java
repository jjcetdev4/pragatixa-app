package com.pragatix.modules.notification.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    private static final Logger log = LoggerFactory.getLogger(TwilioConfig.class);

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String phoneNumber;

    @PostConstruct
    public void initTwilio() {
        if (accountSid == null || accountSid.isEmpty() || authToken == null || authToken.isEmpty()) {
            log.warn("Twilio credentials not found. Twilio initialization skipped.");
            return;
        }
        try {
            Twilio.init(accountSid, authToken);
        } catch (Exception e) {
            log.error("Failed to initialize Twilio SDK", e);
        }
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
