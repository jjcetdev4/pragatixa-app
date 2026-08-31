package jjcet.PragatiX.modules.notification.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service("primarySmsService")
public class PrimarySmsService implements SmsService {

    private final SmsService airtelSmsService;

    @org.springframework.beans.factory.annotation.Autowired
    public PrimarySmsService(@Qualifier("airtelSmsService") SmsService airtelSmsService) {
        this.airtelSmsService = airtelSmsService;
    }

    @Override
    public String sendSms(String phone, String message) {
        return sendSms(phone, message, "GENERAL");
    }

    @Override
    public String sendSms(String phone, String message, String purpose) {
        return airtelSmsService.sendSms(phone, message, purpose);
    }
}
