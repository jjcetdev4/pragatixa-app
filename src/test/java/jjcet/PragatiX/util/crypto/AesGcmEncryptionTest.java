package jjcet.PragatiX.util.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmEncryptionTest {

    private AesGcmAttributeConverter converter;

    @BeforeEach
    void setUp() {
        AesGcmEncryptionUtil.setSecretKey("TestSecretKeyForAesGcm256BitEncryption!");
        converter = new AesGcmAttributeConverter();
    }

    @Test
    void testEncryptionAndDecryption() {
        String originalEmail = "student.enrollment@jjcet.ac.in";
        String originalMobile = "9876543210";

        String encryptedEmail = AesGcmEncryptionUtil.encrypt(originalEmail);
        String encryptedMobile = AesGcmEncryptionUtil.encrypt(originalMobile);

        assertNotNull(encryptedEmail);
        assertNotNull(encryptedMobile);
        assertTrue(encryptedEmail.startsWith(AesGcmEncryptionUtil.PREFIX));
        assertTrue(encryptedMobile.startsWith(AesGcmEncryptionUtil.PREFIX));
        assertNotEquals(originalEmail, encryptedEmail);
        assertNotEquals(originalMobile, encryptedMobile);

        String decryptedEmail = AesGcmEncryptionUtil.decrypt(encryptedEmail);
        String decryptedMobile = AesGcmEncryptionUtil.decrypt(encryptedMobile);

        assertEquals(originalEmail, decryptedEmail);
        assertEquals(originalMobile, decryptedMobile);
    }

    @Test
    void testDeterministicEncryptionForDatabaseQueries() {
        String email = "admin@pragatix.in";
        String encrypted1 = AesGcmEncryptionUtil.encrypt(email);
        String encrypted2 = AesGcmEncryptionUtil.encrypt(email);

        // Deterministic IV ensures exact database match queries (findByEmail, etc.) work properly
        assertEquals(encrypted1, encrypted2);
    }

    @Test
    void testAttributeConverter() {
        String plainPhone = "9123456789";

        String dbColumn = converter.convertToDatabaseColumn(plainPhone);
        assertTrue(dbColumn.startsWith(AesGcmEncryptionUtil.PREFIX));

        String entityAttribute = converter.convertToEntityAttribute(dbColumn);
        assertEquals(plainPhone, entityAttribute);
    }

    @Test
    void testLegacyUnencryptedGracefulFallback() {
        String legacyPlaintext = "old_teacher@jjcet.ac.in";

        // If data in DB does not start with ENC:, converter returns it safely without crashing
        String result = converter.convertToEntityAttribute(legacyPlaintext);
        assertEquals(legacyPlaintext, result);
    }

    @Test
    void testNullAndEmptyHandling() {
        assertNull(AesGcmEncryptionUtil.encrypt(null));
        assertEquals("", AesGcmEncryptionUtil.encrypt(""));
        assertNull(AesGcmEncryptionUtil.decrypt(null));
        assertEquals("", AesGcmEncryptionUtil.decrypt(""));
    }
}
