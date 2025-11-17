package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.PassphraseEncryption;

/**
 * CLI-Tool zum Verschlüsseln von Passphrasen.
 * 
 * Verwendung:
 *   java -cp <classpath> com.example.mcp.sqlite.util.EncryptPassphrase <passphrase>
 * 
 * Oder mit Umgebungsvariable für den Schlüssel:
 *   MCP_SQLITE_ENCRYPTION_KEY=<key> java -cp <classpath> com.example.mcp.sqlite.util.EncryptPassphrase <passphrase>
 */
public class EncryptPassphrase {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Verwendung: EncryptPassphrase <passphrase>");
            System.err.println("");
            System.err.println("Beispiel:");
            System.err.println("  java -cp ... EncryptPassphrase \"meine-passphrase\"");
            System.err.println("");
            System.err.println("Hinweis: Setzen Sie MCP_SQLITE_ENCRYPTION_KEY als Umgebungsvariable,");
            System.err.println("         um einen benutzerdefinierten Verschlüsselungsschlüssel zu verwenden.");
            System.exit(1);
        }
        
        String passphrase = args[0];
        
        try {
            PassphraseEncryption encryption = PassphraseEncryption.fromEnvironment();
            String encrypted = encryption.encrypt(passphrase);
            
            System.out.println("Verschlüsselte Passphrase:");
            System.out.println(encrypted);
            System.out.println("");
            System.out.println("Verwenden Sie diese verschlüsselte Passphrase in Ihrer Konfiguration");
            System.out.println("mit dem Präfix 'encrypted:' (bereits enthalten).");
        } catch (Exception e) {
            System.err.println("Fehler beim Verschlüsseln: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

