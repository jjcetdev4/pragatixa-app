package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.modules.notification.config.SmsProperties;
import jjcet.PragatiX.modules.notification.exception.NotificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrimarySmsServiceTest {

    private SmsProperties smsProperties;
    private SmsService airtelSmsService;
    private SmsService twilioSmsService;
    private PrimarySmsService primarySmsService;

    @BeforeEach
    void setUp() {
        smsProperties = new SmsProperties();
        airtelSmsService = mock(SmsService.class);
        twilioSmsService = mock(SmsService.class);
        primarySmsService = new PrimarySmsService(smsProperties, airtelSmsService, twilioSmsService);
    }

    @Test
    void testSendSms_DefaultAirtelProvider() {
        smsProperties.setProvider(SmsProperties.SmsProvider.AIRTEL);
        when(airtelSmsService.sendSms("9591234567", "Hello", "GENERAL")).thenReturn("REQ_AIRTEL_123");

        String result = primarySmsService.sendSms("9591234567", "Hello");

        assertEquals("REQ_AIRTEL_123", result);
        verify(airtelSmsService).sendSms("9591234567", "Hello", "GENERAL");
        verifyNoInteractions(twilioSmsService);
    }

    @Test
    void testSendSms_TwilioProvider() {
        smsProperties.setProvider(SmsProperties.SmsProvider.TWILIO);
        when(twilioSmsService.sendSms("9591234567", "Hello", "GENERAL")).thenReturn("SM_TWILIO_123");

        String result = primarySmsService.sendSms("9591234567", "Hello");

        assertEquals("SM_TWILIO_123", result);
        verify(twilioSmsService).sendSms("9591234567", "Hello", "GENERAL");
        verifyNoInteractions(airtelSmsService);
    }

    @Test
    void testSendSms_AirtelFails_FallbackDisabled() {
        smsProperties.setProvider(SmsProperties.SmsProvider.AIRTEL);
        smsProperties.setFallbackEnabled(false);

        when(airtelSmsService.sendSms(anyString(), anyString(), anyString()))
                .thenThrow(new NotificationException("Airtel gateway down"));

        assertThrows(NotificationException.class, () ->
                primarySmsService.sendSms("9591234567", "Hello", "OTP"));

        verify(airtelSmsService).sendSms("9591234567", "Hello", "OTP");
        verifyNoInteractions(twilioSmsService);
    }

    @Test
    void testSendSms_AirtelFails_FallbackEnabled_TwilioSucceeds() {
        smsProperties.setProvider(SmsProperties.SmsProvider.AIRTEL);
        smsProperties.setFallbackEnabled(true);

        when(airtelSmsService.sendSms("9591234567", "Hello", "OTP"))
                .thenThrow(new NotificationException("Airtel gateway down"));
        when(twilioSmsService.sendSms("9591234567", "Hello", "OTP"))
                .thenReturn("SM_TWILIO_FALLBACK_123");

        String result = primarySmsService.sendSms("9591234567", "Hello", "OTP");

        assertEquals("SM_TWILIO_FALLBACK_123", result);
        verify(airtelSmsService).sendSms("9591234567", "Hello", "OTP");
        verify(twilioSmsService).sendSms("9591234567", "Hello", "OTP");
    }

    @Test
    void testSendSms_TwilioFails_FallbackEnabled_AirtelSucceeds() {
        smsProperties.setProvider(SmsProperties.SmsProvider.TWILIO);
        smsProperties.setFallbackEnabled(true);

        when(twilioSmsService.sendSms("9591234567", "Hello", "OTP"))
                .thenThrow(new NotificationException("Twilio account suspended"));
        when(airtelSmsService.sendSms("9591234567", "Hello", "OTP"))
                .thenReturn("REQ_AIRTEL_FALLBACK_123");

        String result = primarySmsService.sendSms("9591234567", "Hello", "OTP");

        assertEquals("REQ_AIRTEL_FALLBACK_123", result);
        verify(twilioSmsService).sendSms("9591234567", "Hello", "OTP");
        verify(airtelSmsService).sendSms("9591234567", "Hello", "OTP");
    }
}
