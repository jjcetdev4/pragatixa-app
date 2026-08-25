package jjcet.PragatiX.util.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute Converter that applies AES-256-GCM encryption on database write
 * and AES-256-GCM decryption on database read.
 */
@Converter
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return AesGcmEncryptionUtil.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return AesGcmEncryptionUtil.decrypt(dbData);
    }
}
