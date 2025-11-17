package com.example.mcp.sqlite.config;

import java.nio.file.Path;
import java.util.Objects;

public record DatabaseConfig(Path databasePath, String passphrase, CipherProfile cipherProfile) {

    public DatabaseConfig {
        Objects.requireNonNull(databasePath, "databasePath");
        Objects.requireNonNull(passphrase, "passphrase");
        Objects.requireNonNull(cipherProfile, "cipherProfile");
    }
    
    /**
     * Erstellt eine DatabaseConfig mit automatischer Entschlüsselung der Passphrase falls verschlüsselt.
     * 
     * @param databasePath Pfad zur Datenbank
     * @param passphrase Passphrase (kann verschlüsselt sein mit Präfix "encrypted:")
     * @param cipherProfile Cipher-Profil
     * @return DatabaseConfig mit entschlüsselter Passphrase
     */
    public static DatabaseConfig withDecryptedPassphrase(Path databasePath, String passphrase, CipherProfile cipherProfile) {
        String decryptedPassphrase = decryptPassphraseIfNeeded(passphrase);
        return new DatabaseConfig(databasePath, decryptedPassphrase, cipherProfile);
    }
    
    /**
     * Entschlüsselt eine Passphrase falls sie verschlüsselt ist.
     * 
     * @param passphrase Die Passphrase (kann verschlüsselt sein)
     * @return Die entschlüsselte Passphrase oder die ursprüngliche Passphrase falls nicht verschlüsselt
     * @throws IllegalStateException wenn verschlüsselte Passphrase verwendet wird, aber MCP_SQLITE_ENCRYPTION_KEY nicht gesetzt ist
     */
    public static String decryptPassphraseIfNeeded(String passphrase) {
        if (passphrase == null) {
            return null;
        }
        
        if (PassphraseEncryption.isEncrypted(passphrase)) {
            try {
                PassphraseEncryption encryption = PassphraseEncryption.fromEnvironment();
                String decrypted = encryption.decrypt(passphrase);
                // Debug: Prüfe ob Entschlüsselung erfolgreich war
                if (decrypted == null || decrypted.isEmpty()) {
                    throw new IllegalStateException("Entschlüsselte Passphrase ist leer");
                }
                return decrypted;
            } catch (IllegalStateException e) {
                // Fehler weiterleiten mit hilfreicher Nachricht
                throw new IllegalStateException(
                    "Verschlüsselte Passphrase erkannt, aber MCP_SQLITE_ENCRYPTION_KEY ist nicht gesetzt. " +
                    "Bitte setzen Sie die Umgebungsvariable mit dem Verschlüsselungsschlüssel.", e
                );
            } catch (Exception e) {
                // Andere Fehler beim Entschlüsseln
                throw new IllegalStateException(
                    "Fehler beim Entschlüsseln der Passphrase: " + e.getMessage(), e
                );
            }
        }
        
        return passphrase;
    }
}
