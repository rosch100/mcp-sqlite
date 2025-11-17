package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.KeychainKeyStore;
import com.example.mcp.sqlite.config.PassphraseEncryption;

import java.io.IOException;

/**
 * CLI-Tool zum Speichern eines Verschlüsselungsschlüssels in der macOS Keychain.
 * 
 * Verwendung:
 *   java -cp <classpath> com.example.mcp.sqlite.util.StoreKeyInKeychain <schlüssel>
 * 
 * Oder generiere und speichere automatisch:
 *   java -cp <classpath> com.example.mcp.sqlite.util.StoreKeyInKeychain --generate
 */
public class StoreKeyInKeychain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Verwendung: StoreKeyInKeychain <schlüssel>");
            System.err.println("           StoreKeyInKeychain --generate");
            System.err.println("");
            System.err.println("Beispiele:");
            System.err.println("  # Mit vorhandenem Schlüssel:");
            System.err.println("  java -cp ... StoreKeyInKeychain \"<base64-schlüssel>\"");
            System.err.println("");
            System.err.println("  # Automatisch generieren und speichern:");
            System.err.println("  java -cp ... StoreKeyInKeychain --generate");
            System.err.println("");
            System.err.println("Hinweis: Funktioniert nur auf macOS mit verfügbarer Keychain.");
            System.exit(1);
        }
        
        if (!KeychainKeyStore.isAvailable()) {
            System.err.println("Fehler: macOS Keychain ist nicht verfügbar.");
            System.err.println("Dieses Tool funktioniert nur auf macOS.");
            System.exit(1);
        }
        
        String keyBase64;
        
        if ("--generate".equals(args[0])) {
            // Generiere neuen Schlüssel
            System.out.println("Generiere neuen Verschlüsselungsschlüssel...");
            keyBase64 = PassphraseEncryption.generateKey();
            System.out.println("Generierter Schlüssel: " + keyBase64);
        } else {
            keyBase64 = args[0];
        }
        
        // Validiere Schlüssel
        try {
            PassphraseEncryption.fromBase64Key(keyBase64);
        } catch (IllegalArgumentException e) {
            System.err.println("Fehler: Ungültiger Schlüssel - " + e.getMessage());
            System.exit(1);
        }
        
        // Speichere in Keychain
        try {
            KeychainKeyStore.storeKey(keyBase64);
            System.out.println("");
            System.out.println("✓ Schlüssel erfolgreich in macOS Keychain gespeichert!");
            System.out.println("");
            System.out.println("Der Schlüssel wird automatisch verwendet, wenn keine");
            System.out.println("MCP_SQLITE_ENCRYPTION_KEY Umgebungsvariable gesetzt ist.");
            System.out.println("");
            System.out.println("Sie können den Schlüssel jetzt aus der Keychain verwenden.");
            System.out.println("Um den Schlüssel zu löschen:");
            System.out.println("  security delete-generic-password -a encryption-key -s mcp-sqlite");
        } catch (IOException e) {
            System.err.println("Fehler beim Speichern in Keychain: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

