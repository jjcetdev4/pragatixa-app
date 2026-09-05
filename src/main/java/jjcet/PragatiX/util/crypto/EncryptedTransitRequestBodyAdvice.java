package jjcet.PragatiX.util.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;

/**
 * Enterprise RequestBodyAdvice to automatically decrypt sensitive fields
 * (emails, phone numbers) sent over HTTP JSON payloads before DTO binding
 * and Spring bean validation (@Valid) occur.
 */
@ControllerAdvice
public class EncryptedTransitRequestBodyAdvice extends RequestBodyAdviceAdapter {

    private static final Logger log = LoggerFactory.getLogger(EncryptedTransitRequestBodyAdvice.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                          Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        InputStream is = inputMessage.getBody();
        if (is == null) {
            return inputMessage;
        }

        byte[] bytes = is.readAllBytes();
        if (bytes.length == 0) {
            return new DecryptedHttpInputMessage(inputMessage.getHeaders(), new ByteArrayInputStream(bytes));
        }

        try {
            JsonNode root = objectMapper.readTree(bytes);
            if (root != null && (root.isObject() || root.isArray())) {
                boolean modified = decryptSensitiveNodes(root);
                if (modified) {
                    byte[] decryptedBytes = objectMapper.writeValueAsBytes(root);
                    return new DecryptedHttpInputMessage(inputMessage.getHeaders(), new ByteArrayInputStream(decryptedBytes));
                }
            }
        } catch (Exception e) {
            log.trace("Skipping payload decryption (non-json or unparseable): {}", e.getMessage());
        }

        return new DecryptedHttpInputMessage(inputMessage.getHeaders(), new ByteArrayInputStream(bytes));
    }

    private boolean decryptSensitiveNodes(JsonNode node) {
        boolean modified = false;
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode child = entry.getValue();

                if (child.isTextual()) {
                    String val = child.asText();
                    if (val != null && val.startsWith(AesGcmEncryptionUtil.PREFIX)) {
                        String decrypted = AesGcmEncryptionUtil.decrypt(val);
                        entry.setValue(new TextNode(decrypted));
                        modified = true;
                    }
                } else if (child.isObject() || child.isArray()) {
                    if (decryptSensitiveNodes(child)) {
                        modified = true;
                    }
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode child = arrayNode.get(i);
                if (child.isTextual()) {
                    String val = child.asText();
                    if (val != null && val.startsWith(AesGcmEncryptionUtil.PREFIX)) {
                        String decrypted = AesGcmEncryptionUtil.decrypt(val);
                        arrayNode.set(i, new TextNode(decrypted));
                        modified = true;
                    }
                } else if (child.isObject() || child.isArray()) {
                    if (decryptSensitiveNodes(child)) {
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }

    private static class DecryptedHttpInputMessage implements HttpInputMessage {
        private final HttpHeaders headers;
        private final InputStream body;

        public DecryptedHttpInputMessage(HttpHeaders headers, InputStream body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
