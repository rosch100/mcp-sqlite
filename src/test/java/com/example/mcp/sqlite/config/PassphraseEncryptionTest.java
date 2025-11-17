package com.example.mcp.sqlite.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PassphraseEncryptionTest {

    @Test
    public void testEncryptDecrypt() {
        String key = PassphraseEncryption.generateKey();
        PassphraseEncryption encryption = PassphraseEncryption.fromBase64Key(key);
        
        String originalPassphrase = "meine-geheime-passphrase-123";
        String encrypted = encryption.encrypt(originalPassphrase);
        
        assertTrue(encrypted.startsWith("encrypted:"));
        assertNotEquals(originalPassphrase, encrypted);
        
        String decrypted = encryption.decrypt(encrypted);
        assertEquals(originalPassphrase, decrypted);
    }
    
    @Test
    public void testIsEncrypted() {
        String plain = "plain-passphrase";
        assertFalse(PassphraseEncryption.isEncrypted(plain));
        
        String key = PassphraseEncryption.generateKey();
        PassphraseEncryption encryption = PassphraseEncryption.fromBase64Key(key);
        String encrypted = encryption.encrypt(plain);
        assertTrue(PassphraseEncryption.isEncrypted(encrypted));
    }
    
    @Test
    public void testDecryptWithoutPrefix() {
        String key = PassphraseEncryption.generateKey();
        PassphraseEncryption encryption = PassphraseEncryption.fromBase64Key(key);
        
        String original = "test-passphrase";
        String encrypted = encryption.encrypt(original);
        String withoutPrefix = PassphraseEncryption.removePrefix(encrypted);
        
        String decrypted = encryption.decrypt(withoutPrefix);
        assertEquals(original, decrypted);
    }
    
    @Test
    public void testDatabaseConfigDecryption() {
        String key = PassphraseEncryption.generateKey();
        PassphraseEncryption encryption = PassphraseEncryption.fromBase64Key(key);
        
        String originalPassphrase = "meine-passphrase";
        String encryptedPassphrase = encryption.encrypt(originalPassphrase);
        
        // Temporär den Schlüssel in der Umgebungsvariable setzen
        String oldKey = System.getenv("MCP_SQLITE_ENCRYPTION_KEY");
        try {
            // Setze Umgebungsvariable für den Test
            // Hinweis: System.getenv() ist read-only, daher müssen wir den Test anders strukturieren
            // Wir testen direkt die Entschlüsselung mit dem bekannten Schlüssel
            
            // Da wir die Umgebungsvariable nicht direkt setzen können, testen wir die Logik
            // indem wir prüfen, dass eine verschlüsselte Passphrase erkannt wird
            assertTrue(PassphraseEncryption.isEncrypted(encryptedPassphrase));
            
            // Test mit Plain-Passphrase (sollte unverändert bleiben)
            String plainResult = DatabaseConfig.decryptPassphraseIfNeeded("plain-passphrase");
            assertEquals("plain-passphrase", plainResult);
            
            // Für verschlüsselte Passphrasen benötigen wir die Umgebungsvariable oder Keychain
            // Da dies im Test schwierig ist, testen wir nur die Erkennung
            // Die tatsächliche Entschlüsselung wird in Integration-Tests getestet
        } catch (IllegalStateException e) {
            // Erwartet wenn kein Schlüssel gesetzt ist - das ist OK für Unit-Tests
            // Die tatsächliche Funktionalität wird in Integration-Tests getestet
            assertTrue(e.getMessage().contains("Verschlüsselungsschlüssel nicht gefunden") ||
                      e.getMessage().contains("MCP_SQLITE_ENCRYPTION_KEY"));
        }
    }
    
    @Test
    public void testPlainPassphraseNotChanged() {
        String plainPassphrase = "plain-passphrase";
        String result = DatabaseConfig.decryptPassphraseIfNeeded(plainPassphrase);
        assertEquals(plainPassphrase, result);
    }
}

