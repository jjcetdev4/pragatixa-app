package jjcet.PragatiX.util.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransitEncryptionAdviceTest {

    private EncryptedTransitRequestBodyAdvice requestAdvice;
    private EncryptedTransitResponseBodyAdvice responseAdvice;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        AesGcmEncryptionUtil.setSecretKey("TestSecretKeyForAesGcm256BitEncryption!");
        objectMapper = new ObjectMapper();

        requestAdvice = new EncryptedTransitRequestBodyAdvice();
        ReflectionTestUtils.setField(requestAdvice, "objectMapper", objectMapper);

        responseAdvice = new EncryptedTransitResponseBodyAdvice();
        ReflectionTestUtils.setField(responseAdvice, "objectMapper", objectMapper);
    }

    @Test
    void testRequestBodyDecryption() throws Exception {
        String plainEmail = "student@jjcet.ac.in";
        String plainPhone = "9876543210";
        String encryptedEmail = AesGcmEncryptionUtil.encrypt(plainEmail);
        String encryptedPhone = AesGcmEncryptionUtil.encrypt(plainPhone);

        String json = "{"
                + "\"fullName\":\"John Doe\","
                + "\"email\":\"" + encryptedEmail + "\","
                + "\"phone\":\"" + encryptedPhone + "\","
                + "\"guardian\":{\"phone\":\"" + encryptedPhone + "\"}"
                + "}";

        HttpInputMessage inputMessage = new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };

        HttpInputMessage result = requestAdvice.beforeBodyRead(inputMessage, null, null, MappingJackson2HttpMessageConverter.class);
        String decryptedJson = new String(result.getBody().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(decryptedJson.contains("\"email\":\"" + plainEmail + "\""));
        assertTrue(decryptedJson.contains("\"phone\":\"" + plainPhone + "\""));
        assertFalse(decryptedJson.contains(AesGcmEncryptionUtil.PREFIX));
    }

    @Test
    void testResponseBodyEncryption() {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Jane Doe");
        body.put("email", "teacher@jjcet.ac.in");
        body.put("phone", "9123456789");

        Map<String, Object> guardian = new HashMap<>();
        guardian.put("phone", "9988776655");
        body.put("guardian", guardian);

        MockHttpServletRequest rawRequest = new MockHttpServletRequest("GET", "/api/v1/students/1");
        MockHttpServletResponse rawResponse = new MockHttpServletResponse();
        ServletServerHttpRequest request = new ServletServerHttpRequest(rawRequest);
        ServletServerHttpResponse response = new ServletServerHttpResponse(rawResponse);

        Object result = responseAdvice.beforeBodyWrite(
                body,
                null,
                MediaType.APPLICATION_JSON,
                MappingJackson2HttpMessageConverter.class,
                request,
                response
        );

        assertNotNull(result);
        String resultJson = result.toString();

        // Email and phone must now start with ENC:
        assertTrue(resultJson.contains(AesGcmEncryptionUtil.PREFIX));
        assertFalse(resultJson.contains("teacher@jjcet.ac.in"));
        assertFalse(resultJson.contains("9123456789"));
        assertFalse(resultJson.contains("9988776655"));
        // Non-sensitive fields must remain plain
        assertTrue(resultJson.contains("Jane Doe"));
    }
}
