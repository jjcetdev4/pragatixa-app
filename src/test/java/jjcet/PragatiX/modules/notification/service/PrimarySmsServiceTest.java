package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.modules.notification.exception.NotificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrimarySmsServiceTest {

    private SmsService airtelSmsService;
    private PrimarySmsService primarySmsService;

    @BeforeEach
    void setUp() {
        airtelSmsService = mock(SmsService.class);
        primarySmsService = new PrimarySmsService(airtelSmsService);
    }

    @Test
    void testSendSms_Success() {
        when(airtelSmsService.sendSms("9591234567", "Hello", "GENERAL")).thenReturn("REQ_AIRTEL_123");

        String result = primarySmsService.sendSms("9591234567", "Hello");

        assertEquals("REQ_AIRTEL_123", result);
        verify(airtelSmsService).sendSms("9591234567", "Hello", "GENERAL");
    }

    @Test
    void testSendSms_WithPurpose_Success() {
        when(airtelSmsService.sendSms("9591234567", "Your OTP is 1234", "OTP")).thenReturn("REQ_AIRTEL_OTP_456");

        String result = primarySmsService.sendSms("9591234567", "Your OTP is 1234", "OTP");

        assertEquals("REQ_AIRTEL_OTP_456", result);
        verify(airtelSmsService).sendSms("9591234567", "Your OTP is 1234", "OTP");
    }

    @Test
    void testSendSms_AirtelFails() {
        when(airtelSmsService.sendSms(anyString(), anyString(), anyString()))
                .thenThrow(new NotificationException("Airtel gateway error"));

        assertThrows(NotificationException.class, () ->
                primarySmsService.sendSms("9591234567", "Hello", "OTP"));

        verify(airtelSmsService).sendSms("9591234567", "Hello", "OTP");
    }
}
