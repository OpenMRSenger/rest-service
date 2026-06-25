package openmrsenger.restservice.shared.security;

/**
 * Encrypts/decrypts outbox payloads (patient PII: phone numbers, patient IDs, message text)
 * before they are persisted or transmitted. Implementations must use authenticated encryption.
 */
public interface PayloadEncryptionService {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
