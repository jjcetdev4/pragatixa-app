package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.modules.notification.config.AirtelSmsConfig;
import jjcet.PragatiX.modules.notification.dto.AirtelSmsRequest;
import jjcet.PragatiX.modules.notification.dto.AirtelSmsResponse;
import jjcet.PragatiX.modules.notification.exception.NotificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AirtelSmsServiceTest {

    private AirtelSmsConfig airtelConfig;
    private RestTemplate restTemplate;
    private AirtelSmsService airtelSmsService;

    @BeforeEach
    void setUp() {
        airtelConfig = new AirtelSmsConfig();
        airtelConfig.setEnabled(true);
        airtelConfig.setUrl("https://iqsms.airtel.in/api/v1/send-sms");
        airtelConfig.setCustomerId("CUST_12345");
        airtelConfig.setUsername("airtel_user");
        airtelConfig.setPassword("airtel_pass");
        airtelConfig.setDltTemplateId("1077017800020412986");
        airtelConfig.setEntityId("ENTITY_12345");
        airtelConfig.setSourceAddress("JJECTR");
        airtelConfig.setMessageType("SERVICE_IMPLICIT");

        restTemplate = mock(RestTemplate.class);
        airtelSmsService = new AirtelSmsService(airtelConfig, restTemplate);
    }

    @Test
    void testSendSms_Success() {
        AirtelSmsResponse mockResponse = new AirtelSmsResponse();
        mockResponse.setMessageRequestId("REQ_987654321");
        mockResponse.setStatus("ACCEPTED");
        mockResponse.setStatusCode(200);

        ResponseEntity<AirtelSmsResponse> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://iqsms.airtel.in/api/v1/send-sms"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(AirtelSmsResponse.class)
        )).thenReturn(responseEntity);

        String result = airtelSmsService.sendSms("9591234567", "Student is absent", "ABSENCE");

        assertEquals("REQ_987654321", result);

        ArgumentCaptor<HttpEntity<AirtelSmsRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq("https://iqsms.airtel.in/api/v1/send-sms"), eq(HttpMethod.POST), captor.capture(), eq(AirtelSmsResponse.class));

        HttpEntity<AirtelSmsRequest> entity = captor.getValue();
        AirtelSmsRequest request = entity.getBody();

        assertNotNull(request);
        assertEquals("CUST_12345", request.getCustomerId());
        assertEquals("919591234567", request.getDestinationAddress());
        assertEquals("1077017800020412986", request.getDltTemplateId());
        assertEquals("ENTITY_12345", request.getEntityId());
        assertEquals("Student is absent", request.getMessage());
        assertEquals("SERVICE_IMPLICIT", request.getMessageType());
        assertEquals("JJECTR", request.getSourceAddress());

        // Verify Basic Auth header is present and valid
        assertTrue(entity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION));
        String authHeader = entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
    }

    @Test
    void testSendSms_MissingCredentialsThrowsException() {
        airtelConfig.setUsername("");
        assertThrows(NotificationException.class, () -> airtelSmsService.sendSms("9591234567", "Hello"));
    }

    @Test
    void testSendSms_DisabledThrowsException() {
        airtelConfig.setEnabled(false);
        assertThrows(NotificationException.class, () -> airtelSmsService.sendSms("9591234567", "Hello"));
    }

    @Test
    void testSendSms_Http4xxClientError() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(AirtelSmsResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        NotificationException ex = assertThrows(NotificationException.class, () ->
                airtelSmsService.sendSms("9591234567", "Hello", "OTP"));

        assertTrue(ex.getMessage().contains("validation error"));
    }

    @Test
    void testSendSms_Http5xxServerError() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(AirtelSmsResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error"));

        NotificationException ex = assertThrows(NotificationException.class, () ->
                airtelSmsService.sendSms("9591234567", "Hello", "OTP"));

        assertTrue(ex.getMessage().contains("temporarily unavailable"));
    }

    @Test
    void testSendSms_ConnectionTimeout() {
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(AirtelSmsResponse.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        NotificationException ex = assertThrows(NotificationException.class, () ->
                airtelSmsService.sendSms("9591234567", "Hello", "OTP"));

        assertTrue(ex.getMessage().contains("connection timeout"));
    }
}
