package jjcet.PragatiX.modules.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.modules.notification.config.SmsProperties;
import jjcet.PragatiX.modules.notification.dto.TestSmsRequest;
import jjcet.PragatiX.modules.notification.dto.TestSmsResponse;
import jjcet.PragatiX.modules.notification.service.SmsService;
import jjcet.PragatiX.modules.notification.util.PhoneNumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@Tag(name = "SMS Test", description = "Development and administrative SMS testing endpoints")
public class TestSmsController {

    private static final Logger log = LoggerFactory.getLogger(TestSmsController.class);

    private final SmsService smsService;
    private final SmsProperties smsProperties;

    public TestSmsController(SmsService smsService, SmsProperties smsProperties) {
        this.smsService = smsService;
        this.smsProperties = smsProperties;
    }

    @PostMapping({"/api/test/sms", "/api/v1/test/sms"})
    @Operation(summary = "Send Test SMS", description = "Sends a test SMS through the configured SMS provider (Airtel IQ or Twilio).")
    public ResponseEntity<TestSmsResponse> sendTestSms(@Valid @RequestBody TestSmsRequest request) {
        if (!smsProperties.isTestEndpointEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new TestSmsResponse(false, null, null, null, "DISABLED", "SMS test endpoint is disabled."));
        }

        String maskedPhone = PhoneNumberUtil.maskPhoneNumber(request.getPhoneNumber());
        String providerName = smsProperties.getProvider() != null ? smsProperties.getProvider().name() : "AIRTEL";
        String purpose = request.getPurpose() != null ? request.getPurpose() : "TEST";

        log.info("Test SMS requested via {} to destination {}", providerName, maskedPhone);

        try {
            String messageRequestId = smsService.sendSms(request.getPhoneNumber(), request.getMessage(), purpose);

            TestSmsResponse response = new TestSmsResponse(
                    true,
                    providerName,
                    messageRequestId,
                    maskedPhone,
                    "ACCEPTED",
                    "SMS submitted successfully to " + providerName
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Test SMS sending failed to destination {}: {}", maskedPhone, e.getMessage());

            TestSmsResponse errorResponse = new TestSmsResponse(
                    false,
                    providerName,
                    null,
                    maskedPhone,
                    "FAILED",
                    e.getMessage() != null ? e.getMessage() : "Unable to send SMS. Please try again."
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}
