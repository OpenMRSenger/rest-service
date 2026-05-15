package openmrsenger.restservice.shared.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * JPA AttributeConverter voor het transparant versleutelen en ontsleutelen van
 * databasevelden.
 * * SOLID (Single Responsibility Principle):
 * Deze klasse heeft slechts één verantwoordelijkheid: het transformeren van
 * data tussen de
 * object-wereld (plain-text) en de relationele-wereld (cipher-text). De
 * entiteiten en services
 * hoeven niets af te weten van cryptografie, waardoor hun verantwoordelijkheden
 * strikt gescheiden blijven.
 * * Encryption at Rest in SaaS:
 * In een multi-tenant applicatie is de impact van een datalek catastrofaal als
 * alle API-keys
 * van elk ziekenhuis gestolen worden. Encryption at Rest zorgt ervoor dat een
 * gecompromitteerde
 * database-dump waardeloos is zonder de encryptiesleutel, die idealiter in een
 * beveiligde
 * vault (zoals AWS KMS of HashiCorp Vault) op de applicatieserver leeft.
 */
@Converter
public class AesEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final byte[] KEY;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        String envKey = System.getenv("ENCRYPTION_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            KEY = envKey.getBytes();
        } else {
            KEY = "MijnGeheimeSleutel12345!".getBytes();
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Key key = new SecretKeySpec(KEY, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes());

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new EncryptionException("Error while encrypting database attribute.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            byte[] iv = Arrays.copyOfRange(decoded, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(decoded, GCM_IV_LENGTH, decoded.length);

            Key key = new SecretKeySpec(KEY, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new EncryptionException("Error while decrypting database attribute.", e);
        }
    }
}
