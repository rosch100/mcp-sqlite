package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.CipherProfile;
import com.example.mcp.sqlite.config.DatabaseConfig;
import com.example.mcp.sqlite.EncryptedSqliteClient;

import java.sql.SQLException;

/**
 * Test-Tool zum vollständigen Test der verschlüsselten Passphrase
 */
public class TestFullDecryption {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Verwendung: TestFullDecryption <db-path> <verschlüsselte-passphrase>");
            System.exit(1);
        }
        
        String dbPath = args[0];
        String encryptedPassphrase = args[1];
        
        System.out.println("Datenbank: " + dbPath);
        System.out.println("Verschlüsselte Passphrase: " + encryptedPassphrase);
        System.out.println();
        
        try {
            // Simuliere genau das, was McpServer.main() macht
            System.out.println("1. Entschlüssele Passphrase...");
            String decryptedPassphrase = DatabaseConfig.decryptPassphraseIfNeeded(encryptedPassphrase);
            System.out.println("   ✓ Entschlüsselt: " + decryptedPassphrase.length() + " Zeichen");
            System.out.println("   Erste 10 Zeichen: " + decryptedPassphrase.substring(0, Math.min(10, decryptedPassphrase.length())) + "...");
            System.out.println();
            
            // Erstelle DatabaseConfig genau wie im Server
            System.out.println("2. Erstelle DatabaseConfig...");
            CipherProfile profile = CipherProfile.sqlCipher4Defaults();
            DatabaseConfig config = DatabaseConfig.withDecryptedPassphrase(
                java.nio.file.Path.of(dbPath),
                encryptedPassphrase,
                profile
            );
            System.out.println("   ✓ DatabaseConfig erstellt");
            System.out.println("   Passphrase in Config: " + config.passphrase().length() + " Zeichen");
            System.out.println();
            
            // Versuche Verbindung zu öffnen
            System.out.println("3. Versuche Datenbankverbindung...");
            EncryptedSqliteClient client = new EncryptedSqliteClient();
            client.withConnection(config, connection -> {
                System.out.println("   ✓ Verbindung erfolgreich!");
                System.out.println("   Versuche Tabellen aufzulisten...");
                var tables = client.listTables(connection);
                System.out.println("   ✓ " + tables.size() + " Tabellen gefunden!");
                return null;
            });
            System.out.println();
            System.out.println("✓ Alles funktioniert!");
            
        } catch (Exception e) {
            System.err.println("✗ Fehler:");
            System.err.println("  " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

