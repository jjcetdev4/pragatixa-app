package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.modules.notification.config.SmsProperties;
import jjcet.PragatiX.modules.notification.exception.NotificationException;
import jjcet.PragatiX.modules.notification.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service("primarySmsService")
public class PrimarySmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(PrimarySmsService.class);

    private final SmsProperties smsProperties;
    private final SmsService airtelSmsService;
    private final SmsService twilioSmsService;

    @org.springframework.beans.factory.annotation.Autowired
    public PrimarySmsService(
            SmsProperties smsProperties,
            @Qualifier("airtelSmsService") SmsService airtelSmsService,
            @Qualifier("twilioSmsService") SmsService twilioSmsService) {
        this.smsProperties = smsProperties;
        this.airtelSmsService = airtelSmsService;
        this.twilioSmsService = twilioSmsService;
    }

    @Override
    public String sendSms(String phone, String message) {
        return sendSms(phone, message, "GENERAL");
    }

    @Override
    public String sendSms(String phone, String message, String purpose) {
        SmsProperties.SmsProvider provider = smsProperties.getProvider();
        boolean fallbackEnabled = smsProperties.isFallbackEnabled();
        String maskedPhone = PhoneNumberUtil.maskPhoneNumber(phone);

        if (provider == SmsProperties.SmsProvider.AIRTEL) {
            try {
                return airtelSmsService.sendSms(phone, message, purpose);
            } catch (Exception e) {
                if (fallbackEnabled) {
                    log.warn("Airtel SMS failed for destination [{}]. Fallback is enabled. Attempting fallback to Twilio...", maskedPhone, e);
                    try {
                        return twilioSmsService.sendSms(phone, message, purpose);
                    } catch (Exception fallbackEx) {
                        log.error("Both Airtel and Twilio SMS failed for destination [{}]", maskedPhone, fallbackEx);
                        throw new NotificationException("All SMS providers failed: " + fallbackEx.getMessage(), fallbackEx);
                    }
                }
                throw e;
            }
        } else {
            try {
                return twilioSmsService.sendSms(phone, message, purpose);
            } catch (Exception e) {
                if (fallbackEnabled) {
                    log.warn("Twilio SMS failed for destination [{}]. Fallback is enabled. Attempting fallback to Airtel...", maskedPhone, e);
                    try {
                        return airtelSmsService.sendSms(phone, message, purpose);
                    } catch (Exception fallbackEx) {
                        log.error("Both Twilio and Airtel SMS failed for destination [{}]", maskedPhone, fallbackEx);
                        throw new NotificationException("All SMS providers failed: " + fallbackEx.getMessage(), fallbackEx);
                    }
                }
                throw e;
            }
        }
    }
}
