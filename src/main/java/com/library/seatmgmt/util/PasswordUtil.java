package com.library.seatmgmt.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing user passwords using SHA-256 with a salt.
 */
public final class PasswordUtil {

    private static final String SALT = "LMS_SECRET_SALT_2024";

    private PasswordUtil() {
    }

    /**
     * Hashes a raw password string using SHA-256 with salt.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = plainPassword + SALT;
            byte[] hashBytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 hashing algorithm not available", e);
        }
    }

    /**
     * Verifies whether a plain password matches a stored hash.
     */
    public static boolean verifyPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        String calculated = hashPassword(plainPassword);
        return calculated.equalsIgnoreCase(storedHash);
    }
}
