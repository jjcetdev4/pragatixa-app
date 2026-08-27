package jjcet.PragatiX.modules.notification.service;

import jjcet.PragatiX.modules.notification.config.AirtelSmsConfig;
import jjcet.PragatiX.modules.notification.dto.AirtelSmsRequest;
import jjcet.PragatiX.modules.notification.dto.AirtelSmsResponse;
import jjcet.PragatiX.modules.notification.exception.NotificationException;
import jjcet.PragatiX.modules.notification.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service("airtelSmsService")
public class AirtelSmsService implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(AirtelSmsService.class);

    private final AirtelSmsConfig airtelConfig;
    private final RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public AirtelSmsService(AirtelSmsConfig airtelConfig) {
        this.airtelConfig = airtelConfig;
        this.restTemplate = createRestTemplate(airtelConfig);
    }

    // For testing/mocking injection
    public AirtelSmsService(AirtelSmsConfig airtelConfig, RestTemplate restTemplate) {
        this.airtelConfig = airtelConfig;
        this.restTemplate = restTemplate;
    }

    private static RestTemplate createRestTemplate(AirtelSmsConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs() > 0 ? config.getConnectTimeoutMs() : 5000);
        factory.setReadTimeout(config.getReadTimeoutMs() > 0 ? config.getReadTimeoutMs() : 10000);
        RestTemplate rt = new RestTemplate(factory);
        rt.getMessageConverters().removeIf(converter -> converter instanceof StringHttpMessageConverter);
        rt.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return rt;
    }

    @Override
    public String sendSms(String phone, String message) {
        return sendSms(phone, message, "GENERAL");
    }

    @Override
    public String sendSms(String phone, String message, String purpose) {
        if (!airtelConfig.isEnabled()) {
            throw new NotificationException("Airtel SMS service is currently disabled in configuration.");
        }

        validateConfiguration();

        String normalizedDestination = PhoneNumberUtil.normalizeIndianPhoneNumber(phone);
        String maskedDestination = PhoneNumberUtil.maskPhoneNumber(normalizedDestination);
        String smsPurpose = (purpose != null && !purpose.trim().isEmpty()) ? purpose.trim().toUpperCase() : "GENERAL";

        String dltTemplateId = airtelConfig.getDltTemplateId();

        AirtelSmsRequest requestPayload = new AirtelSmsRequest(
                airtelConfig.getCustomerId(),
                normalizedDestination,
                dltTemplateId,
                airtelConfig.getEntityId(),
                message,
                airtelConfig.getMessageType() != null ? airtelConfig.getMessageType() : "SERVICE_IMPLICIT",
                airtelConfig.getSourceAddress() != null ? airtelConfig.getSourceAddress() : "JJECTR"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(airtelConfig.getUsername(), airtelConfig.getPassword());

        HttpEntity<AirtelSmsRequest> entity = new HttpEntity<>(requestPayload, headers);

        log.info("AIRTEL SMS REQUEST STARTED - Purpose: {}, Destination: {}", smsPurpose, maskedDestination);

        try {
            ResponseEntity<AirtelSmsResponse> response = restTemplate.exchange(
                    airtelConfig.getUrl(),
                    HttpMethod.POST,
                    entity,
                    AirtelSmsResponse.class
            );

            AirtelSmsResponse body = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && body != null && body.isSuccessful()) {
                String messageRequestId = body.getMessageRequestId() != null ? body.getMessageRequestId() : "REQ_ACCEPTED";

                log.info("\n================ AIRTEL SMS ================\n"
                                + "Purpose: {}\n"
                                + "Destination: {}\n"
                                + "Status: ACCEPTED\n"
                                + "MessageRequestId: {}\n"
                                + "============================================",
                        smsPurpose, maskedDestination, messageRequestId);

                return messageRequestId;
            } else {
                String errorDesc = (body != null && body.getDesc() != null) ? body.getDesc()
                        : (body != null && body.getError() != null) ? body.getError() : "Unknown Airtel API rejection";
                String statusStr = body != null && body.getStatus() != null ? body.getStatus() : "FAILED";

                log.error("AIRTEL SMS FAILED - Purpose: {}, Destination: {}, Status: {}, Reason: {}",
                        smsPurpose, maskedDestination, statusStr, errorDesc);

                throw new NotificationException("Airtel SMS failed: " + errorDesc);
            }

        } catch (HttpClientErrorException e) {
            log.error("AIRTEL SMS HTTP 4xx CLIENT ERROR - Status: {}, Purpose: {}, Destination: {}, Response: {}",
                    e.getStatusCode(), smsPurpose, maskedDestination, e.getResponseBodyAsString());
            throw new NotificationException("Unable to send SMS due to provider validation error (HTTP " + e.getStatusCode().value() + ").", e);

        } catch (HttpServerErrorException e) {
            log.error("AIRTEL SMS HTTP 5xx SERVER ERROR - Status: {}, Purpose: {}, Destination: {}, Response: {}",
                    e.getStatusCode(), smsPurpose, maskedDestination, e.getResponseBodyAsString());
            throw new NotificationException("Unable to send SMS. Airtel service is temporarily unavailable (HTTP " + e.getStatusCode().value() + ").", e);

        } catch (ResourceAccessException e) {
            log.error("AIRTEL SMS NETWORK/TIMEOUT ERROR - Purpose: {}, Destination: {}, Message: {}",
                    smsPurpose, maskedDestination, e.getMessage());
            throw new NotificationException("Unable to send SMS. Provider connection timeout.", e);

        } catch (NotificationException ne) {
            throw ne;

        } catch (Exception e) {
            log.error("AIRTEL SMS UNEXPECTED ERROR - Purpose: {}, Destination: {}, Error: {}",
                    smsPurpose, maskedDestination, e.getMessage(), e);
            throw new NotificationException("Unable to send SMS. Please try again.", e);
        }
    }

    private void validateConfiguration() {
        if (airtelConfig.getUsername() == null || airtelConfig.getUsername().trim().isEmpty()
                || airtelConfig.getPassword() == null || airtelConfig.getPassword().trim().isEmpty()) {
            log.error("Airtel SMS credentials (username/password) are missing.");
            throw new NotificationException("Airtel SMS credentials are not configured.");
        }
        if (airtelConfig.getCustomerId() == null || airtelConfig.getCustomerId().trim().isEmpty()) {
            log.error("Airtel Customer ID is missing.");
            throw new NotificationException("Airtel Customer ID is not configured.");
        }
        if (airtelConfig.getUrl() == null || airtelConfig.getUrl().trim().isEmpty()) {
            log.error("Airtel SMS URL is missing.");
            throw new NotificationException("Airtel SMS URL is not configured.");
        }
    }
}
