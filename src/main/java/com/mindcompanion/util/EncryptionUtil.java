package com.mindcompanion.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    @Value("${app.encryption.secret}")
    private String encryptionSecret;

    // Encrypt plain text → Base64 encoded cipher text
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            SecretKey key = getSecretKey();

            // Generate random IV for each encryption
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to encrypted data, then Base64 encode
            byte[] encryptedWithIv = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, encryptedWithIv,
                    IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    // Decrypt Base64 encoded cipher text → plain text
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            SecretKey key = getSecretKey();

            byte[] encryptedWithIv = Base64.getDecoder()
                    .decode(encryptedText);

            // Extract IV from first 16 bytes
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedWithIv, 0, iv, 0, IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // Extract actual encrypted data
            byte[] encrypted = new byte[encryptedWithIv.length - IV_LENGTH];
            System.arraycopy(encryptedWithIv, IV_LENGTH,
                    encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // Build AES key from secret string
    private SecretKey getSecretKey() {
        byte[] keyBytes = new byte[32]; // 256 bits
        byte[] secretBytes = encryptionSecret
                .getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0,
                Math.min(secretBytes.length, keyBytes.length));
        return new SecretKeySpec(keyBytes, "AES");
    }
}