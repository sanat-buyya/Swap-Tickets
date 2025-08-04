package com.example.SwapTicket.helper;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AES {

    private static SecretKeySpec secretKey;
    private static byte[] key;

    private static String secret;

    @Value("${aes.secret.key:DefaultSecretKey123}")
    private String secretFromProperties;

    @jakarta.annotation.PostConstruct
    public void init() {
        secret = secretFromProperties;
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("AES secret key is not configured. Please set aes.secret.key in application.properties");
        }
    }

    public static void setKey(final String myKey) {
        try {
            if (myKey == null || myKey.trim().isEmpty()) {
                throw new IllegalArgumentException("AES key cannot be null or empty");
            }
            key = myKey.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set AES key", e);
        }
    }

    public static String encrypt(final String strToEncrypt) {
        try {
            if (strToEncrypt == null || strToEncrypt.trim().isEmpty()) {
                throw new IllegalArgumentException("String to encrypt cannot be null or empty");
            }
            if (secret == null) {
                throw new IllegalStateException("AES secret key is not initialized");
            }
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        } catch (Exception e) {
            System.err.println("Error while encrypting: " + e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static String decrypt(final String strToDecrypt) {
        try {
            if (strToDecrypt == null || strToDecrypt.trim().isEmpty()) {
                throw new IllegalArgumentException("String to decrypt cannot be null or empty");
            }
            if (secret == null) {
                throw new IllegalStateException("AES secret key is not initialized");
            }
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            System.err.println("Error while decrypting: " + e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }
}