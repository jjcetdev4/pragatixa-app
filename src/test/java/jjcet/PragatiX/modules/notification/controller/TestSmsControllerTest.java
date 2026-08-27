package jjcet.PragatiX.modules.notification.controller;

import jjcet.PragatiX.modules.notification.config.SmsProperties;
import jjcet.PragatiX.modules.notification.dto.TestSmsRequest;
import jjcet.PragatiX.modules.notification.dto.TestSmsResponse;
import jjcet.PragatiX.modules.notification.exception.NotificationException;
import jjcet.PragatiX.modules.notification.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestSmsControllerTest {

    private SmsService smsService;
    private SmsProperties smsProperties;
    private TestSmsController controller;

    @BeforeEach
    void setUp() {
        smsService = mock(SmsService.class);
        smsProperties = new SmsProperties();
        smsProperties.setTestEndpointEnabled(true);
        smsProperties.setProvider(SmsProperties.SmsProvider.AIRTEL);
        controller = new TestSmsController(smsService, smsProperties);
    }

    @Test
    void testSendTestSms_Success() {
        when(smsService.sendSms("9591234567", "Test SMS from Pragatix", "TEST"))
                .thenReturn("REQ_TEST_123");

        TestSmsRequest request = new TestSmsRequest("9591234567", "Test SMS from Pragatix");
        ResponseEntity<TestSmsResponse> responseEntity = controller.sendTestSms(request);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        TestSmsResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("AIRTEL", body.getProvider());
        assertEquals("REQ_TEST_123", body.getMessageRequestId());
        assertEquals("******4567", body.getMaskedDestination());
        assertEquals("ACCEPTED", body.getStatus());
    }

    @Test
    void testSendTestSms_Disabled() {
        smsProperties.setTestEndpointEnabled(false);

        TestSmsRequest request = new TestSmsRequest("9591234567", "Test SMS");
        ResponseEntity<TestSmsResponse> responseEntity = controller.sendTestSms(request);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertFalse(responseEntity.getBody().isSuccess());
        assertEquals("DISABLED", responseEntity.getBody().getStatus());
    }

    @Test
    void testSendTestSms_Failure() {
        when(smsService.sendSms("9591234567", "Test SMS", "TEST"))
                .thenThrow(new NotificationException("Provider unreachable"));

        TestSmsRequest request = new TestSmsRequest("9591234567", "Test SMS");
        ResponseEntity<TestSmsResponse> responseEntity = controller.sendTestSms(request);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        TestSmsResponse body = responseEntity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("FAILED", body.getStatus());
        assertTrue(body.getNote().contains("Provider unreachable"));
    }
}
