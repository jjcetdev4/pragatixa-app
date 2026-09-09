package jjcet.PragatiX.util.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Enterprise AES-256-GCM Encryption Utility for database field-level security.
 * Uses AES with Galois/Counter Mode (GCM) and 128-bit authentication tag.
 * Employs deterministic HMAC-derived 12-byte IV for exact-match query indexing.
 */
public final class AesGcmEncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(AesGcmEncryptionUtil.class);

    public static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static final String LEGACY_DEFAULT_SECRET = "PragatiXAES256GCMDatabaseEncryptionSecretKey2026!";

    private static volatile String secretKeyStr = null;
    private static volatile byte[] aesKey256 = null;

    private AesGcmEncryptionUtil() {}

    public static synchronized void setSecretKey(String secret) {
        if (secret != null && !secret.trim().isEmpty()) {
            secretKeyStr = secret.trim();
            aesKey256 = deriveKey(secretKeyStr);
        }
    }

    private static byte[] getAesKey() {
        if (aesKey256 == null) {
            synchronized (AesGcmEncryptionUtil.class) {
                if (aesKey256 == null) {
                    String envSecret = System.getenv("DATABASE_ENCRYPTION_SECRET");
                    if (envSecret == null || envSecret.trim().isEmpty()) {
                        envSecret = System.getProperty("app.security.encryption.secret");
                    }
                    if (envSecret == null || envSecret.trim().isEmpty()) {
                        envSecret = System.getenv("JWT_SECRET");
                    }
                    if (envSecret == null || envSecret.trim().isEmpty()) {
                        envSecret = LEGACY_DEFAULT_SECRET;
                    }
                    setSecretKey(envSecret);
                }
            }
        }
        return aesKey256;
    }

    private static List<byte[]> getCandidateKeys() {
        List<byte[]> candidateKeys = new ArrayList<>();
        byte[] primary = getAesKey();
        if (primary != null) {
            candidateKeys.add(primary);
        }

        // Add legacy secret as fallback for data already encrypted in DB
        byte[] legacyKey = deriveKey(LEGACY_DEFAULT_SECRET);
        boolean hasLegacy = false;
        for (byte[] k : candidateKeys) {
            if (Arrays.equals(k, legacyKey)) {
                hasLegacy = true;
                break;
            }
        }
        if (!hasLegacy) {
            candidateKeys.add(legacyKey);
        }

        // Add JWT_SECRET if distinct
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret != null && !jwtSecret.trim().isEmpty()) {
            byte[] jwtKey = deriveKey(jwtSecret.trim());
            boolean hasJwt = false;
            for (byte[] k : candidateKeys) {
                if (Arrays.equals(k, jwtKey)) {
                    hasJwt = true;
                    break;
                }
            }
            if (!hasJwt) {
                candidateKeys.add(jwtKey);
            }
        }

        return candidateKeys;
    }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive AES-256 key", e);
        }
    }

    /**
     * Encrypts plain text using AES-256-GCM with deterministic IV derivation.
     * Returns "ENC:<Base64(IV + CipherText + Tag)>"
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (plainText.isEmpty()) {
            return "";
        }
        // If already encrypted, do not re-encrypt
        if (plainText.startsWith(PREFIX)) {
            return plainText;
        }

        try {
            byte[] key = getAesKey();
            byte[] inputBytes = plainText.getBytes(StandardCharsets.UTF_8);

            // Deterministic 12-byte IV using HMAC-SHA256
            byte[] iv = generateDeterministicIv(inputBytes, key);

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec);

            byte[] cipherText = cipher.doFinal(inputBytes);

            // Combine IV (12 bytes) + CipherText with GCM Tag
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES-256-GCM encryption failed: {}", e.getMessage());
            throw new RuntimeException("Error encrypting field", e);
        }
    }

    /**
     * Decrypts encrypted text ("ENC:<Base64(IV + CipherText + Tag)>") back to plain text.
     * If the input is not encrypted (e.g. legacy plain text), returns as-is.
     * Gracefully checks primary and fallback candidate keys to avoid tag mismatch.
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        if (encryptedText.isEmpty()) {
            return "";
        }
        // Gracefully handle unencrypted legacy values
        if (!encryptedText.startsWith(PREFIX)) {
            return encryptedText;
        }

        try {
            String base64Payload = encryptedText.substring(PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Payload);

            if (combined.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                log.warn("Ciphertext too short for GCM payload, returning as-is");
                return encryptedText;
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);

            for (byte[] candidateKey : getCandidateKeys()) {
                try {
                    SecretKeySpec secretKeySpec = new SecretKeySpec(candidateKey, "AES");
                    GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

                    Cipher cipher = Cipher.getInstance(ALGORITHM);
                    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec);

                    byte[] plainBytes = cipher.doFinal(cipherText);
                    return new String(plainBytes, StandardCharsets.UTF_8);
                } catch (AEADBadTagException tagEx) {
                    // Tag mismatch with this key, try next candidate key in pool
                    continue;
                }
            }

            log.warn("AES-256-GCM decryption failed: Tag mismatch with all candidate keys. Returning original text.");
            return encryptedText;
        } catch (Exception e) {
            log.error("AES-256-GCM decryption encountered error: {}", e.getMessage());
            return encryptedText;
        }
    }

    private static byte[] generateDeterministicIv(byte[] inputBytes, byte[] key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(key, HMAC_ALGO));
        byte[] hmac = mac.doFinal(inputBytes);
        return Arrays.copyOf(hmac, GCM_IV_LENGTH_BYTES);
    }
}
