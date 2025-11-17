package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.KeychainKeyStore;
import com.example.mcp.sqlite.config.PassphraseEncryption;

/**
 * Test-Tool zum Prüfen des Keychain-Zugriffs
 */
public class TestKeychainAccess {
    public static void main(String[] args) {
        System.out.println("Prüfe Keychain-Verfügbarkeit...");
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("Keychain verfügbar: " + KeychainKeyStore.isAvailable());
        System.out.println();
        
        if (KeychainKeyStore.isAvailable()) {
            try {
                String key = KeychainKeyStore.loadKey();
                if (key != null && !key.isEmpty()) {
                    System.out.println("✓ Schlüssel aus Keychain geladen!");
                    System.out.println("Schlüssel-Länge: " + key.length() + " Zeichen");
                    System.out.println("Erste 20 Zeichen: " + key.substring(0, Math.min(20, key.length())) + "...");
                    
                    // Teste Verschlüsselung
                    try {
                        PassphraseEncryption encryption = PassphraseEncryption.fromEnvironment();
                        System.out.println("✓ PassphraseEncryption.fromEnvironment() erfolgreich!");
                    } catch (Exception e) {
                        System.err.println("✗ Fehler bei PassphraseEncryption.fromEnvironment():");
                        System.err.println("  " + e.getMessage());
                    }
                } else {
                    System.out.println("✗ Kein Schlüssel in Keychain gefunden");
                }
            } catch (Exception e) {
                System.err.println("✗ Fehler beim Laden aus Keychain:");
                System.err.println("  " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("✗ Keychain ist nicht verfügbar");
            String envKey = System.getenv("MCP_SQLITE_ENCRYPTION_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                System.out.println("✓ MCP_SQLITE_ENCRYPTION_KEY Umgebungsvariable ist gesetzt");
            } else {
                System.out.println("✗ MCP_SQLITE_ENCRYPTION_KEY Umgebungsvariable ist NICHT gesetzt");
            }
        }
    }
}

