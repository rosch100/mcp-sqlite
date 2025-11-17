package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.DatabaseConfig;
import com.example.mcp.sqlite.config.PassphraseEncryption;

/**
 * Test-Tool zum Prüfen der Passphrase-Entschlüsselung
 */
public class TestDecryption {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Verwendung: TestDecryption <verschlüsselte-passphrase>");
            System.exit(1);
        }
        
        String encryptedPassphrase = args[0];
        
        System.out.println("Verschlüsselte Passphrase:");
        System.out.println(encryptedPassphrase);
        System.out.println();
        
        // Prüfe ob verschlüsselt
        boolean isEncrypted = PassphraseEncryption.isEncrypted(encryptedPassphrase);
        System.out.println("Ist verschlüsselt: " + isEncrypted);
        System.out.println();
        
        if (isEncrypted) {
            try {
                System.out.println("Versuche Entschlüsselung...");
                String decrypted = DatabaseConfig.decryptPassphraseIfNeeded(encryptedPassphrase);
                System.out.println("✓ Entschlüsselung erfolgreich!");
                System.out.println("Entschlüsselte Länge: " + decrypted.length() + " Zeichen");
                System.out.println("Erste 10 Zeichen: " + decrypted.substring(0, Math.min(10, decrypted.length())) + "...");
                System.out.println("Letzte 10 Zeichen: ..." + decrypted.substring(Math.max(0, decrypted.length() - 10)));
            } catch (Exception e) {
                System.err.println("✗ Fehler bei der Entschlüsselung:");
                System.err.println("  " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            System.out.println("Passphrase ist nicht verschlüsselt (kein 'encrypted:' Präfix)");
            System.out.println("Passphrase: " + encryptedPassphrase);
        }
    }
}

