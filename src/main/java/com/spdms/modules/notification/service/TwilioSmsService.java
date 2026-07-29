package com.pragatix.modules.notification.service;

import com.pragatix.modules.notification.config.TwilioConfig;
import com.pragatix.modules.notification.exception.NotificationException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TwilioSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsService.class);
    private final TwilioConfig twilioConfig;

    public TwilioSmsService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
    }

    @Override
    public String sendSms(String phone, String messageContent) {
        try {
            if (twilioConfig.getPhoneNumber() == null || twilioConfig.getPhoneNumber().isEmpty()) {
                throw new NotificationException("Twilio phone number is not configured.");
            }

            // Ensure phone number starts with country code, assuming India +91 if not
            // provided
            String formattedPhone = phone;
            if (!formattedPhone.startsWith("+")) {
                formattedPhone = "+91" + formattedPhone;
            }

            log.info("\n===== SMS CONTENT =====\n{}\n=======================\n", messageContent);

            int charCount = messageContent.length();
            int byteCount = messageContent.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            int estimatedSegments = (int) Math.ceil((double) charCount / 70.0);

            log.info("Character Count: {}", charCount);
            log.info("Byte Count (UTF-8): {}", byteCount);
            log.info("Estimated SMS Segments: {}", estimatedSegments);
            if (estimatedSegments > 1) {
                log.warn("WARNING: Unicode SMS requires {} segments.", estimatedSegments);
            }

            Message message = Message.creator(
                    new PhoneNumber(formattedPhone),
                    new PhoneNumber(twilioConfig.getPhoneNumber()),
                    messageContent).create();

            log.info("SMS SID: {}", message.getSid());
            log.info("SMS STATUS: {}", message.getStatus());
            log.info("SMS ERROR CODE: {}", message.getErrorCode());
            log.info("SMS ERROR MESSAGE: {}", message.getErrorMessage());
            log.info("SMS SEGMENTS: {}", message.getNumSegments());
            log.info("SMS PRICE: {} {}", message.getPrice(), message.getPriceUnit());
            log.info("DATE CREATED: {}", message.getDateCreated());
            log.info("DATE SENT: {}", message.getDateSent());
            log.info("DIRECTION: {}", message.getDirection());
            log.info("FROM: {}", message.getFrom());
            log.info("TO: {}", message.getTo());

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            Message fetchedMessage = Message.fetcher(message.getSid()).fetch();
            log.info("Current Status: {}", fetchedMessage.getStatus());

            if (fetchedMessage.getStatus() == Message.Status.FAILED
                    || fetchedMessage.getStatus() == Message.Status.UNDELIVERED) {
                log.error("Error Code: {}", fetchedMessage.getErrorCode());
                log.error("Error Message: {}", fetchedMessage.getErrorMessage());
            }

            return message.getSid();
        } catch (Exception e) {
            log.error("Failed to send Twilio SMS to {}", phone, e);
            if (e instanceof com.twilio.exception.ApiException) {
                com.twilio.exception.ApiException apiException = (com.twilio.exception.ApiException) e;
                log.error("Twilio Error Code: {}", apiException.getCode());
                log.error("Twilio Error Message: {}", apiException.getMessage());
            }
            throw new NotificationException("Twilio SMS failed: " + e.getMessage(), e);
        }
    }
}
