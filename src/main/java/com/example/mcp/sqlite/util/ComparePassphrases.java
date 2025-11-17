package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.DatabaseConfig;

/**
 * Vergleichstool für Passphrasen
 */
public class ComparePassphrases {
    public static void main(String[] args) {
        String encrypted = "encrypted:REMOVED_ENCRYPTED_PASSPHRASE";
        String plain = "lusikac7";
        
        System.out.println("Plain-Passphrase:");
        System.out.println("  Länge: " + plain.length());
        System.out.println("  Wert: '" + plain + "'");
        System.out.println("  Bytes: " + java.util.Arrays.toString(plain.getBytes()));
        System.out.println();
        
        try {
            String decrypted = DatabaseConfig.decryptPassphraseIfNeeded(encrypted);
            System.out.println("Entschlüsselte Passphrase:");
            System.out.println("  Länge: " + decrypted.length());
            System.out.println("  Wert: '" + decrypted + "'");
            System.out.println("  Bytes: " + java.util.Arrays.toString(decrypted.getBytes()));
            System.out.println();
            
            System.out.println("Vergleich:");
            System.out.println("  Gleich: " + plain.equals(decrypted));
            System.out.println("  Längen gleich: " + (plain.length() == decrypted.length()));
            
            if (!plain.equals(decrypted)) {
                System.out.println("  Unterschiede:");
                int minLen = Math.min(plain.length(), decrypted.length());
                for (int i = 0; i < minLen; i++) {
                    if (plain.charAt(i) != decrypted.charAt(i)) {
                        System.out.println("    Position " + i + ": '" + plain.charAt(i) + "' vs '" + decrypted.charAt(i) + "'");
                    }
                }
                if (plain.length() != decrypted.length()) {
                    System.out.println("    Längen unterschiedlich: " + plain.length() + " vs " + decrypted.length());
                }
            }
        } catch (Exception e) {
            System.err.println("Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

