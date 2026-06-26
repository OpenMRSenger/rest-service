package openmrsenger.restservice.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UNIT TEST: AesPayloadEncryptionService
 *
 * Proves AES-256 (GCM) round-trips correctly, never leaks plaintext into the ciphertext,
 * and rejects misconfigured (non-256-bit) keys - the key requirement for GDPR-grade
 * encryption-at-rest of patient phone numbers / IDs / message text.
 */
class AesPayloadEncryptionServiceTest {

    private static final String VALID_KEY = "hhTa0lgeWcYZ1CvUmAmAHpxbdxw4GNKD33gC8LfnswA=";

    private final PayloadEncryptionService service = new AesPayloadEncryptionService(VALID_KEY);

    @Test
    @DisplayName("Encrypt then decrypt returns the original plaintext")
    void encryptDecrypt_RoundTrip() {
        String plaintext = "{\"patientId\":\"PAT-001\",\"phoneNumber\":\"+31612345678\",\"messageText\":\"Reminder\"}";

        String ciphertext = service.encrypt(plaintext);
        String decrypted = service.decrypt(ciphertext);

        assertEquals(plaintext, decrypted);
    }

    @Test
    @DisplayName("Ciphertext never contains the plaintext PII in any encoding")
    void ciphertext_DoesNotLeakPlaintext() {
        String plaintext = "{\"patientId\":\"PAT-001\",\"phoneNumber\":\"+31612345678\"}";

        String ciphertext = service.encrypt(plaintext);

        assertNotEquals(plaintext, ciphertext);
        assertFalse(ciphertext.contains("PAT-001"));
        assertFalse(ciphertext.contains("+31612345678"));
        // also check raw decoded bytes (hex) don't contain the ASCII bytes of the PII
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        String hex = HexFormat.of().formatHex(raw);
        assertFalse(hex.contains(HexFormat.of().formatHex("PAT-001".getBytes())));
    }

    @Test
    @DisplayName("Same plaintext encrypted twice produces different ciphertext (random IV)")
    void encrypt_IsNonDeterministic() {
        String plaintext = "same input";

        String first = service.encrypt(plaintext);
        String second = service.encrypt(plaintext);

        assertNotEquals(first, second);
        assertEquals(plaintext, service.decrypt(first));
        assertEquals(plaintext, service.decrypt(second));
    }

    @Test
    @DisplayName("null plaintext/ciphertext passes through as null")
    void nullInput_ReturnsNull() {
        assertNull(service.encrypt(null));
        assertNull(service.decrypt(null));
    }

    @Test
    @DisplayName("Rejects a key that is not 256 bits (32 bytes) long")
    void constructor_RejectsWrongKeyLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]); // AES-128, not allowed

        assertThrows(IllegalStateException.class, () -> new AesPayloadEncryptionService(shortKey));
    }

    @Test
    @DisplayName("Rejects a key that is not valid Base64")
    void constructor_RejectsInvalidBase64() {
        assertThrows(IllegalStateException.class, () -> new AesPayloadEncryptionService("not-valid-base64!@#"));
    }

    @Test
    @DisplayName("Decrypting tampered ciphertext fails (authenticated encryption)")
    void decrypt_RejectsTamperedCiphertext() {
        String ciphertext = service.encrypt("sensitive data");
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        raw[raw.length - 1] ^= 0x01; // flip a bit in the auth tag
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered));
    }
}
