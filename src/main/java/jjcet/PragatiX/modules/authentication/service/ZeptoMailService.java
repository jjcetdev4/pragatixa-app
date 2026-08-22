package jjcet.PragatiX.modules.authentication.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ZeptoMailService {

    private static final Logger log = LoggerFactory.getLogger(ZeptoMailService.class);

    @Value("${zepto.mail.url}")
    private String mailUrl;

    @Value("${zepto.mail.token}")
    private String mailToken;

    @Value("${zepto.mail.sender-address}")
    private String senderAddress;

    @Value("${zepto.mail.sender-name}")
    private String senderName;

    @Value("${zepto.mail.template-key:#{null}}")
    private String templateKey;

    private final RestTemplate restTemplate;

    public ZeptoMailService() {
        this.restTemplate = new RestTemplate();
    }

    public boolean sendOtpEmail(String toEmail, String otp) {
        log.info("Sending OTP to email: {}", toEmail);

        // Safe Diagnostic Logging
        log.info("ZeptoMail Diagnostics - URL Configured: {}", (mailUrl != null && !mailUrl.isEmpty()));
        log.info("ZeptoMail Diagnostics - Token Length: {}", mailToken != null ? mailToken.length() : 0);
        log.info("ZeptoMail Diagnostics - Token Starts with Zoho-enczapikey: {}",
                mailToken != null && mailToken.trim().toLowerCase().startsWith("zoho-enczapikey"));
        log.info("ZeptoMail Diagnostics - Sender Address Configured: {}",
                (senderAddress != null && !senderAddress.isEmpty()));
        log.info("ZeptoMail Diagnostics - Template Key Present: {}",
                (templateKey != null && !templateKey.trim().isEmpty()));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String token = mailToken.trim();
            if (!token.toLowerCase().startsWith("zoho-enczapikey")) {
                token = "Zoho-enczapikey " + token;
            }

            headers.set("Authorization", token);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            Map<String, Object> requestBody = new HashMap<>();

            Map<String, String> from = new HashMap<>();
            from.put("address", senderAddress);
            from.put("name", senderName);
            requestBody.put("from", from);

            Map<String, Object> toObj = new HashMap<>();
            Map<String, String> emailAddress = new HashMap<>();
            emailAddress.put("address", toEmail);
            toObj.put("email_address", emailAddress);
            requestBody.put("to", List.of(toObj));

            String targetUrl = mailUrl;

            if (templateKey != null && !templateKey.trim().isEmpty()) {
                requestBody.put("mail_template_key", templateKey);
                Map<String, String> mergeInfo = new HashMap<>();
                mergeInfo.put("otp", otp); // Lowercase variable
                mergeInfo.put("OTP", otp); // Uppercase variable
                requestBody.put("merge_info", mergeInfo);

                // Switch endpoint URL for templates if not already set
                if (targetUrl.endsWith("/email")) {
                    targetUrl = targetUrl + "/template";
                }
            } else {
                requestBody.put("subject", "Your Login OTP for Pragatix Dashboard");
                String htmlBody = "<html><body>"
                        + "<h2>Login OTP</h2>"
                        + "<p>Your One Time Password (OTP) for login is: <b>" + otp + "</b></p>"
                        + "<p>This OTP is valid for 5 minutes. Do not share it with anyone.</p>"
                        + "</body></html>";
                requestBody.put("htmlbody", htmlBody);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Executing ZeptoMail API Request to {}", targetUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(targetUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("OTP email sent successfully to {}", toEmail);
                return true;
            } else {
                log.error("Failed to send OTP email. Status: {}, Response: {}", response.getStatusCode(),
                        response.getBody());
                return false;
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("ZeptoMail API Error: Status {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Exception occurred while sending OTP email via ZeptoMail: {}", e.getMessage());
            return false;
        }
    }
}
