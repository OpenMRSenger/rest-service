package openmrsenger.restservice.shared.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

/**
 * JPA AttributeConverter voor het transparant versleutelen en ontsleutelen van databasevelden.
 * * SOLID (Single Responsibility Principle): 
 * Deze klasse heeft slechts één verantwoordelijkheid: het transformeren van data tussen de 
 * object-wereld (plain-text) en de relationele-wereld (cipher-text). De entiteiten en services 
 * hoeven niets af te weten van cryptografie, waardoor hun verantwoordelijkheden strikt gescheiden blijven.
 * * Encryption at Rest in SaaS:
 * In een multi-tenant applicatie is de impact van een datalek catastrofaal als alle API-keys 
 * van elk ziekenhuis gestolen worden. Encryption at Rest zorgt ervoor dat een gecompromitteerde 
 * database-dump waardeloos is zonder de encryptiesleutel, die idealiter in een beveiligde 
 * vault (zoals AWS KMS of HashiCorp Vault) op de applicatieserver leeft.
 */
@Converter
public class AesEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/ECB/PKCS5Padding";
    // LET OP: In productie NOOIT hardcoden. Haal dit uit een environment variable of Secret Manager.
    private static final byte[] KEY = "MijnGeheimeSleutel12345!".getBytes();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            Key key = new SecretKeySpec(KEY, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Fout tijdens het versleutelen van het database attribuut.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            Key key = new SecretKeySpec(KEY, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Fout tijdens het ontsleutelen van het database attribuut.", e);
        }
    }
}
