package openmrsenger.restservice.shared.logging;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogSanitizerTest {

    @Test
    void testMaskPhone() {
        assertNull(LogSanitizer.maskPhone(null));
        assertEquals("****", LogSanitizer.maskPhone("123"));
        assertEquals("****", LogSanitizer.maskPhone("1234"));
        assertEquals("********5678", LogSanitizer.maskPhone("+31612345678"));
    }

    @Test
    void testMaskMessage() {
        assertNull(LogSanitizer.maskMessage(null));
        assertEquals("[MASKED]", LogSanitizer.maskMessage("Hello World"));
    }

    @Test
    void testRedactHeaders() {
        assertTrue(LogSanitizer.redactHeaders(null).isEmpty());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("X-API-KEY", "secret-api-key");
        headers.put("Content-Type", "application/json");

        Map<String, String> redacted = LogSanitizer.redactHeaders(headers);
        assertEquals("[REDACTED]", redacted.get("Authorization"));
        assertEquals("[REDACTED]", redacted.get("X-API-KEY"));
        assertEquals("application/json", redacted.get("Content-Type"));
    }

    @Test
    void testMaskPayloadMap() {
        assertNull(LogSanitizer.maskPayload(null));

        Map<String, Object> mapPayload = new HashMap<>();
        mapPayload.put("destination", "+31612345678");
        mapPayload.put("content", "Secret Message");
        mapPayload.put("Recipients", new String[]{"+31612345678"});
        mapPayload.put("Content", "Another Secret");
        mapPayload.put("priority", "high");

        Object masked = LogSanitizer.maskPayload(mapPayload);
        assertTrue(masked instanceof Map);
        Map<?, ?> result = (Map<?, ?>) masked;
        assertEquals("********5678", result.get("destination"));
        assertEquals("[MASKED]", result.get("content"));
        assertEquals(List.of("********5678"), result.get("Recipients"));
        assertEquals("[MASKED]", result.get("Content"));
        assertEquals("high", result.get("priority"));
    }

    @Test
    void testMaskPayloadStringJson() {
        String json = "{\"recipient\":\"+31612345678\",\"body\":\"Secret Message\",\"format\":\"SMS\"}";
        Object masked = LogSanitizer.maskPayload(json);
        assertTrue(masked instanceof String);
        String result = (String) masked;
        assertTrue(result.contains("\"recipient\":\"********5678\""));
        assertTrue(result.contains("\"body\":\"[MASKED]\""));
    }

    @Test
    void testMaskPayloadStringXml() {
        String xml = "<SendSmsRequest><PhoneNumber>+31612345678</PhoneNumber><MessageText>Secret Message</MessageText></SendSmsRequest>";
        Object masked = LogSanitizer.maskPayload(xml);
        assertTrue(masked instanceof String);
        String result = (String) masked;
        assertTrue(result.contains("<PhoneNumber>********5678</PhoneNumber>"));
        assertTrue(result.contains("<MessageText>[MASKED]</MessageText>"));
    }

    @Test
    void testSanitizeExceptionMessage() {
        assertNull(LogSanitizer.sanitizeExceptionMessage(null));

        Exception regularEx = new IllegalArgumentException("Normal error message");
        assertEquals("Normal error message", LogSanitizer.sanitizeExceptionMessage(regularEx));

        Exception sensitiveEx = new IllegalArgumentException("Error: {\"recipient\":\"+31612345678\",\"body\":\"Secret Message\"}");
        String sanitized = LogSanitizer.sanitizeExceptionMessage(sensitiveEx);
        assertTrue(sanitized.contains("\"recipient\":\"********5678\""));
        assertTrue(sanitized.contains("\"body\":\"[MASKED]\""));
    }
}
