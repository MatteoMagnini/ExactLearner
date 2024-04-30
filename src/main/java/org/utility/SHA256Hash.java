package org.utility;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class provides a method to hash a string using SHA-256 algorithm.
 */
public class SHA256Hash {
    /**
     * This method hashes a string using SHA-256 algorithm.
     * @param input the string to be hashed.
     * @return the hashed string.
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());

            // Convert byte array to a hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Handle exception if SHA-256 algorithm is not available
            e.printStackTrace();
            return null;
        }
    }
}
