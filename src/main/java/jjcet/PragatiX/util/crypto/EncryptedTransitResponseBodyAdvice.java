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
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Enterprise ResponseBodyAdvice to automatically encrypt sensitive fields
 * (emails, phone numbers) before writing HTTP JSON response payloads to the wire.
 */
@ControllerAdvice
public class EncryptedTransitResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedTransitResponseBodyAdvice.class);

    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>(Arrays.asList(
            "email",
            "phone",
            "phoneno",
            "guardianphone",
            "guardianemail",
            "guardianphoneno",
            "mobilenumber"
    ));

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        String path = request.getURI().getPath();
        if (path != null && (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/actuator"))) {
            return body;
        }

        if (body instanceof byte[] || body instanceof org.springframework.core.io.Resource || body instanceof String) {
            return body;
        }

        try {
            JsonNode root = objectMapper.valueToTree(body);
            if (root != null && (root.isObject() || root.isArray())) {
                boolean modified = encryptSensitiveNodes(root);
                if (modified) {
                    return root;
                }
            }
        } catch (Exception e) {
            log.trace("Skipping response body encryption: {}", e.getMessage());
        }

        return body;
    }

    private boolean encryptSensitiveNodes(JsonNode node) {
        boolean modified = false;
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                String normalizedKey = key.toLowerCase().replaceAll("[^a-z0-9]", "");
                JsonNode child = entry.getValue();

                if (child.isTextual() && SENSITIVE_FIELDS.contains(normalizedKey)) {
                    String plain = child.asText();
                    if (plain != null && !plain.trim().isEmpty() && !plain.startsWith(AesGcmEncryptionUtil.PREFIX)) {
                        String encrypted = AesGcmEncryptionUtil.encrypt(plain);
                        entry.setValue(new TextNode(encrypted));
                        modified = true;
                    }
                } else if (child.isObject() || child.isArray()) {
                    if (encryptSensitiveNodes(child)) {
                        modified = true;
                    }
                }
            }
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode child = arrayNode.get(i);
                if (child.isObject() || child.isArray()) {
                    if (encryptSensitiveNodes(child)) {
                        modified = true;
                    }
                }
            }
        }
        return modified;
    }
}
