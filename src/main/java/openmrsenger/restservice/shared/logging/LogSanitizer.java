package openmrsenger.restservice.shared.logging;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.JsonProcessingException;

@SuppressWarnings("java:S2068")
public final class LogSanitizer {

    private static final String MASKED = "[MASKED]";
    private static final String DESTINATION = "destination";
    private static final String CONTENT = "content";
    private static final String CONTENT_UPPER = "Content";
    private static final String RECIPIENTS = "Recipients";

    private LogSanitizer() {
        throw new IllegalStateException("Utility class");
    }

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

    private static final Pattern JSON_RECIPIENT_PATTERN = Pattern.compile("(\"(?:recipient|destination|PhoneNumber|Recipients)\"\\s*:\\s*\")([^\"]++)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern JSON_BODY_PATTERN = Pattern.compile("(\"(?:body|content|Content)\"\\s*:\\s*\")([^\"]++)(\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_PHONE_PATTERN = Pattern.compile("(<PhoneNumber>)([^<]++)(</PhoneNumber>)", Pattern.CASE_INSENSITIVE);
    private static final Pattern XML_TEXT_PATTERN = Pattern.compile("(<MessageText>)([^<]++)(</MessageText>)", Pattern.CASE_INSENSITIVE);

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
        return MASKED;
    }

    public static String sanitizeExceptionMessage(Throwable t) {
        if (t == null) {
            return null;
        }
        if (t instanceof JsonProcessingException jsonEx) {
            return jsonEx.getOriginalMessage();
        }
        String msg = t.getMessage();
        if (msg == null) {
            return t.getClass().getName();
        }
        return maskStringPayload(msg);
    }

    public static Map<String, String> redactHeaders(Map<String, String> headers) {
        if (headers == null) {
            return Collections.emptyMap();
        }
        Map<String, String> redacted = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT))) {
                redacted.put(key, "[REDACTED]");
            } else {
                redacted.put(key, entry.getValue());
            }
        }
        return redacted;
    }

    public static Object maskPayload(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof Map<?, ?> rawMap) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String keyStr) {
                    copy.put(keyStr, entry.getValue());
                }
            }
            copy.computeIfPresent(DESTINATION, (k, dest) -> dest instanceof String destStr ? maskPhone(destStr) : dest);
            copy.computeIfPresent(CONTENT, (k, v) -> MASKED);
            copy.computeIfPresent(CONTENT_UPPER, (k, v) -> MASKED);
            copy.computeIfPresent(RECIPIENTS, (k, rec) -> {
                if (rec instanceof String[] arr) {
                    String[] maskedArr = new String[arr.length];
                    for (int i = 0; i < arr.length; i++) {
                        maskedArr[i] = maskPhone(arr[i]);
                    }
                    return java.util.Arrays.asList(maskedArr);
                } else if (rec instanceof java.util.List<?> list) {
                    return list.stream()
                            .map(item -> item instanceof String itemStr ? maskPhone(itemStr) : String.valueOf(item))
                            .toList();
                }
                return rec;
            });
            return copy;
        }
        if (payload instanceof String strPayload) {
            return maskStringPayload(strPayload);
        }
        return payload;
    }

    private static String replaceAll(String input, Pattern pattern, java.util.function.Function<Matcher, String> replacementFunction) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = replacementFunction.apply(matcher);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String maskStringPayload(String payload) {
        if (payload == null) {
            return null;
        }
        String result = payload;
        result = replaceAll(result, JSON_RECIPIENT_PATTERN, m -> m.group(1) + maskPhone(m.group(2)) + m.group(3));
        result = replaceAll(result, JSON_BODY_PATTERN, m -> m.group(1) + MASKED + m.group(3));
        result = replaceAll(result, XML_PHONE_PATTERN, m -> m.group(1) + maskPhone(m.group(2)) + m.group(3));
        result = replaceAll(result, XML_TEXT_PATTERN, m -> m.group(1) + MASKED + m.group(3));
        return result;
    }
}
