package openmrsenger.restservice.shared.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;

public class LogSanitizer {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "authorization",
            "x-api-key",
            "api-key",
            "apikey",
            "x-provider-config",
            "password",
            "clientsecret",
            "client_secret"
    );

    private static final Pattern JSON_RECIPIENT_PATTERN = Pattern.compile("(\"(?:recipient|destination|PhoneNumber|Recipients)\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_BODY_PATTERN = Pattern.compile("(\"(?:body|content|Content)\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_PHONE_PATTERN = Pattern.compile("(<PhoneNumber>)(.*?)(</PhoneNumber>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_TEXT_PATTERN = Pattern.compile("(<MessageText>)(.*?)(</MessageText>)", Pattern.CASE_INSENSITIVE);

    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        if (phone.length() <= 4) {
            return "****";
        }
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    public static String maskMessage(String message) {
        if (message == null) {
            return null;
        }
        return "[MASKED]";
    }

    public static String sanitizeExceptionMessage(Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof JsonProcessingException) {
            return ((JsonProcessingException) t).getOriginalMessage();
        }
        String msg = t.getMessage();
        if (msg == null) {
            return t.getClass().getName();
        }
        return maskStringPayload(msg);
    }

    public static Map<String, String> redactHeaders(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }
        Map<String, String> redacted = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (SENSITIVE_KEYS.contains(key.toLowerCase())) {
                redacted.put(key, "[REDACTED]");
            } else {
                redacted.put(key, entry.getValue());
            }
        }
        return redacted;
    }

    @SuppressWarnings("unchecked")
    public static Object maskPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof Map) {
            try {
                Map<String, Object> copy = new LinkedHashMap<>((Map<String, Object>) payload);
                if (copy.containsKey("destination")) {
                    Object dest = copy.get("destination");
                    copy.put("destination", dest instanceof String ? maskPhone((String) dest) : dest);
                }
                if (copy.containsKey("content")) {
                    copy.put("content", "[MASKED]");
                }
                if (copy.containsKey("Recipients")) {
                    Object rec = copy.get("Recipients");
                    if (rec instanceof String[]) {
                        String[] arr = (String[]) rec;
                        String[] maskedArr = new String[arr.length];
                        for (int i = 0; i < arr.length; i++) {
                            maskedArr[i] = maskPhone(arr[i]);
                        }
                        copy.put("Recipients", java.util.Arrays.asList(maskedArr));
                    } else if (rec instanceof java.util.List<?>) {
                        java.util.List<?> list = (java.util.List<?>) rec;
                        java.util.List<String> maskedList = list.stream()
                                .map(item -> item instanceof String ? maskPhone((String) item) : String.valueOf(item))
                                .toList();
                        copy.put("Recipients", maskedList);
                    }
                }
                if (copy.containsKey("Content")) {
                    copy.put("Content", "[MASKED]");
                }
                return copy;
            } catch (Exception e) {
                return "[MASKED PAYLOAD ERROR]";
            }
        }
        if (payload instanceof String) {
            return maskStringPayload((String) payload);
        }
        return payload;
    }

    private static String maskStringPayload(String payload) {
        if (payload == null) {
            return null;
        }
        String result = payload;

        // Match JSON phone numbers
        Matcher jsonPhoneMatcher = JSON_RECIPIENT_PATTERN.matcher(result);
        StringBuilder sb = new StringBuilder();
        while (jsonPhoneMatcher.find()) {
            jsonPhoneMatcher.appendReplacement(sb, jsonPhoneMatcher.group(1) + maskPhone(jsonPhoneMatcher.group(2)) + jsonPhoneMatcher.group(3));
        }
        jsonPhoneMatcher.appendTail(sb);
        result = sb.toString();

        // Match JSON body/content
        Matcher jsonBodyMatcher = JSON_BODY_PATTERN.matcher(result);
        sb = new StringBuilder();
        while (jsonBodyMatcher.find()) {
            jsonBodyMatcher.appendReplacement(sb, jsonBodyMatcher.group(1) + "[MASKED]" + jsonBodyMatcher.group(3));
        }
        jsonBodyMatcher.appendTail(sb);
        result = sb.toString();

        // Match XML phone numbers
        Matcher xmlPhoneMatcher = XML_PHONE_PATTERN.matcher(result);
        sb = new StringBuilder();
        while (xmlPhoneMatcher.find()) {
            xmlPhoneMatcher.appendReplacement(sb, xmlPhoneMatcher.group(1) + maskPhone(xmlPhoneMatcher.group(2)) + xmlPhoneMatcher.group(3));
        }
        xmlPhoneMatcher.appendTail(sb);
        result = sb.toString();

        // Match XML text
        Matcher xmlTextMatcher = XML_TEXT_PATTERN.matcher(result);
        sb = new StringBuilder();
        while (xmlTextMatcher.find()) {
            xmlTextMatcher.appendReplacement(sb, xmlTextMatcher.group(1) + "[MASKED]" + xmlTextMatcher.group(3));
        }
        xmlTextMatcher.appendTail(sb);
        result = sb.toString();

        return result;
    }
}
