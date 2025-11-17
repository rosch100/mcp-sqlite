package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.CipherProfile;
import com.example.mcp.sqlite.config.DatabaseConfig;
import com.example.mcp.sqlite.EncryptedSqliteClient;

import java.sql.SQLException;

/**
 * Test-Tool zum Testen der Datenbankverbindung mit verschlüsselter Passphrase
 */
public class TestDatabaseConnection {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Verwendung: TestDatabaseConnection <db-path> <passphrase>");
            System.exit(1);
        }
        
        String dbPath = args[0];
        String passphrase = args[1];
        
        System.out.println("Datenbank: " + dbPath);
        System.out.println("Passphrase (verschlüsselt): " + passphrase.startsWith("encrypted:"));
        System.out.println();
        
        try {
            // Erstelle DatabaseConfig genau wie im Server
            CipherProfile profile = CipherProfile.sqlCipher4Defaults();
            DatabaseConfig config = DatabaseConfig.withDecryptedPassphrase(
                java.nio.file.Path.of(dbPath),
                passphrase,
                profile
            );
            
            System.out.println("DatabaseConfig erstellt:");
            System.out.println("  Passphrase Länge: " + config.passphrase().length());
            System.out.println("  Passphrase Wert: '" + config.passphrase() + "'");
            System.out.println("  Passphrase Bytes: " + java.util.Arrays.toString(config.passphrase().getBytes()));
            System.out.println();
            
            // Versuche Verbindung zu öffnen
            System.out.println("Versuche Datenbankverbindung...");
            EncryptedSqliteClient client = new EncryptedSqliteClient();
            client.withConnection(config, connection -> {
                System.out.println("✓ Verbindung erfolgreich!");
                System.out.println("Versuche Tabellen aufzulisten...");
                var tables = client.listTables(connection);
                System.out.println("✓ " + tables.size() + " Tabellen gefunden!");
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

