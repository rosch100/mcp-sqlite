package com.example.mcp.sqlite.config;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Verschlüsselungsklasse für Passphrasen mit AES-256-GCM.
 * GCM (Galois/Counter Mode) bietet authenticated encryption, ist sicher und schnell.
 */
public final class PassphraseEncryption {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits für GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits für Authentifizierung
    private static final int KEY_LENGTH = 256; // 256 bits für AES-256
    private static final String ENCRYPTED_PREFIX = "encrypted:";
    
    private final SecretKey secretKey;
    
    /**
     * Erstellt eine PassphraseEncryption-Instanz mit einem Schlüssel.
     * Versucht zuerst macOS Keychain, dann Umgebungsvariable.
     * 
     * @return PassphraseEncryption-Instanz
     * @throws IllegalStateException wenn kein Schlüssel gefunden wird
     */
    public static PassphraseEncryption fromEnvironment() {
        String keyBase64 = null;
        String keySource = null;
        
        // Versuche zuerst macOS Keychain (falls verfügbar)
        if (KeychainKeyStore.isAvailable()) {
            try {
                keyBase64 = KeychainKeyStore.loadKey();
                if (keyBase64 != null && !keyBase64.isEmpty()) {
                    keySource = "Keychain";
                }
            } catch (Exception e) {
                // Ignoriere Fehler und versuche Umgebungsvariable
                System.err.println("Warnung: Fehler beim Laden aus Keychain: " + e.getMessage());
            }
        }
        
        // Fallback auf Umgebungsvariable
        if (keyBase64 == null || keyBase64.isEmpty()) {
            keyBase64 = System.getenv("MCP_SQLITE_ENCRYPTION_KEY");
            if (keyBase64 != null && !keyBase64.isEmpty()) {
                keySource = "Umgebungsvariable";
            }
        }
        
        if (keyBase64 == null || keyBase64.isEmpty()) {
            StringBuilder message = new StringBuilder(
                "Verschlüsselungsschlüssel nicht gefunden. "
            );
            
            if (KeychainKeyStore.isAvailable()) {
                message.append(
                    "Bitte speichern Sie einen Schlüssel in der macOS Keychain:\n" +
                    "  java -cp <jar> com.example.mcp.sqlite.util.StoreKeyInKeychain <schlüssel>\n" +
                    "Oder setzen Sie die Umgebungsvariable:\n" +
                    "  export MCP_SQLITE_ENCRYPTION_KEY=\"<schlüssel>\""
                );
            } else {
                message.append(
                    "Bitte setzen Sie die Umgebungsvariable:\n" +
                    "  export MCP_SQLITE_ENCRYPTION_KEY=\"$(java -cp <jar> com.example.mcp.sqlite.util.GenerateKey)\""
                );
            }
            
            throw new IllegalStateException(message.toString());
        }
        
        // Debug-Ausgabe (nur wenn System Property gesetzt ist)
        if (System.getProperty("mcp.sqlite.debug") != null) {
            System.err.println("Debug: Verschlüsselungsschlüssel geladen aus: " + keySource);
        }
        
        return fromBase64Key(keyBase64);
    }
    
    /**
     * Erstellt eine PassphraseEncryption-Instanz mit einem Schlüssel aus einer Umgebungsvariable.
     * Falls die Umgebungsvariable nicht gesetzt ist, wird eine Warnung ausgegeben und ein
     * deterministischer Schlüssel verwendet (NUR FÜR ENTWICKLUNG - NICHT FÜR PRODUKTION!).
     * 
     * @param allowFallback true, um einen Fallback-Schlüssel zu erlauben (nur für Entwicklung)
     * @return PassphraseEncryption-Instanz
     * @deprecated Verwenden Sie fromEnvironment() ohne Fallback für Produktion
     */
    @Deprecated
    public static PassphraseEncryption fromEnvironment(boolean allowFallback) {
        String keyBase64 = System.getenv("MCP_SQLITE_ENCRYPTION_KEY");
        if (keyBase64 != null && !keyBase64.isEmpty()) {
            return fromBase64Key(keyBase64);
        }
        if (!allowFallback) {
            throw new IllegalStateException(
                "MCP_SQLITE_ENCRYPTION_KEY Umgebungsvariable ist nicht gesetzt. " +
                "Bitte setzen Sie einen sicheren Verschlüsselungsschlüssel."
            );
        }
        // Warnung ausgeben
        System.err.println("WARNUNG: MCP_SQLITE_ENCRYPTION_KEY ist nicht gesetzt!");
        System.err.println("WARNUNG: Es wird ein deterministischer Fallback-Schlüssel verwendet.");
        System.err.println("WARNUNG: Dies ist UNSICHER und sollte nur für Entwicklung verwendet werden!");
        System.err.println("WARNUNG: Für Produktion setzen Sie bitte: export MCP_SQLITE_ENCRYPTION_KEY=\"<schlüssel>\"");
        return fromBase64Key(generateDeterministicKey());
    }
    
    /**
     * Erstellt eine PassphraseEncryption-Instanz aus einem Base64-kodierten Schlüssel.
     * 
     * @param keyBase64 Base64-kodierter 256-bit Schlüssel
     * @return PassphraseEncryption-Instanz
     * @throws IllegalArgumentException wenn der Schlüssel ungültig ist
     */
    public static PassphraseEncryption fromBase64Key(String keyBase64) {
        if (keyBase64 == null || keyBase64.isEmpty()) {
            throw new IllegalArgumentException("Schlüssel darf nicht leer sein");
        }
        
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Ungültiges Base64-Format für den Schlüssel", e);
        }
        
        if (keyBytes.length != KEY_LENGTH / 8) {
            throw new IllegalArgumentException(
                String.format("Schlüssel muss genau %d Bytes (256 bits) lang sein, aber war %d Bytes", 
                    KEY_LENGTH / 8, keyBytes.length)
            );
        }
        
        // Prüfe auf schwache Schlüssel (z.B. alle Nullen oder zu wenig Entropie)
        if (isWeakKey(keyBytes)) {
            throw new IllegalArgumentException(
                "Der Schlüssel ist zu schwach. Bitte verwenden Sie einen zufällig generierten Schlüssel."
            );
        }
        
        SecretKey key = new SecretKeySpec(keyBytes, ALGORITHM);
        return new PassphraseEncryption(key);
    }
    
    /**
     * Prüft, ob ein Schlüssel zu schwach ist (z.B. alle Nullen oder zu wenig Entropie).
     * 
     * @param keyBytes Die Schlüsselbytes
     * @return true wenn der Schlüssel schwach ist
     */
    private static boolean isWeakKey(byte[] keyBytes) {
        // Prüfe auf alle Nullen
        boolean allZeros = true;
        for (byte b : keyBytes) {
            if (b != 0) {
                allZeros = false;
                break;
            }
        }
        if (allZeros) {
            return true;
        }
        
        // Prüfe auf zu wenig Entropie (z.B. sich wiederholende Muster)
        // Einfache Heuristik: Wenn mehr als 75% der Bytes gleich sind, ist der Schlüssel schwach
        Map<Byte, Integer> byteCounts = new HashMap<>();
        for (byte b : keyBytes) {
            byteCounts.put(b, byteCounts.getOrDefault(b, 0) + 1);
        }
        int maxCount = Collections.max(byteCounts.values());
        if (maxCount > keyBytes.length * 0.75) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Generiert einen neuen zufälligen Verschlüsselungsschlüssel.
     * 
     * @return Base64-kodierter Schlüssel
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Generieren des Schlüssels", e);
        }
    }
    
    /**
     * Generiert einen deterministischen Schlüssel für Entwicklung (nur für Tests).
     * 
     * @return Base64-kodierter Schlüssel
     */
    private static String generateDeterministicKey() {
        // Einfacher deterministischer Schlüssel für Entwicklung
        // In Produktion sollte immer ein zufälliger Schlüssel verwendet werden
        String seed = "mcp-sqlite-default-key-development-only";
        byte[] keyBytes = new byte[32];
        byte[] seedBytes = seed.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < 32; i++) {
            keyBytes[i] = seedBytes[i % seedBytes.length];
        }
        return Base64.getEncoder().encodeToString(keyBytes);
    }
    
    private PassphraseEncryption(SecretKey secretKey) {
        this.secretKey = secretKey;
    }
    
    /**
     * Verschlüsselt eine Passphrase.
     * 
     * @param passphrase Die zu verschlüsselnde Passphrase
     * @return Verschlüsselte Passphrase mit Präfix "encrypted:" und Base64-Kodierung
     */
    public String encrypt(String passphrase) {
        if (passphrase == null || passphrase.isEmpty()) {
            throw new IllegalArgumentException("Passphrase darf nicht leer sein");
        }
        
        try {
            // Generiere zufälligen IV für jede Verschlüsselung
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] plaintext = passphrase.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Kombiniere IV und Ciphertext
            byte[] encrypted = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, encrypted, GCM_IV_LENGTH, ciphertext.length);
            
            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Verschlüsseln der Passphrase", e);
        }
    }
    
    /**
     * Entschlüsselt eine verschlüsselte Passphrase.
     * 
     * @param encryptedPassphrase Die verschlüsselte Passphrase (mit oder ohne Präfix)
     * @return Die entschlüsselte Passphrase
     */
    public String decrypt(String encryptedPassphrase) {
        if (encryptedPassphrase == null || encryptedPassphrase.isEmpty()) {
            throw new IllegalArgumentException("Verschlüsselte Passphrase darf nicht leer sein");
        }
        
        // Entferne Präfix falls vorhanden
        String toDecrypt = encryptedPassphrase.startsWith(ENCRYPTED_PREFIX)
                ? encryptedPassphrase.substring(ENCRYPTED_PREFIX.length())
                : encryptedPassphrase;
        
        try {
            byte[] encrypted = Base64.getDecoder().decode(toDecrypt);
            
            if (encrypted.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Ungültiges verschlüsseltes Format");
            }
            
            // Extrahiere IV und Ciphertext
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
            
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Entschlüsseln der Passphrase", e);
        }
    }
    
    /**
     * Prüft, ob eine Passphrase verschlüsselt ist.
     * 
     * @param passphrase Die zu prüfende Passphrase
     * @return true wenn verschlüsselt, false sonst
     */
    public static boolean isEncrypted(String passphrase) {
        return passphrase != null && passphrase.startsWith(ENCRYPTED_PREFIX);
    }
    
    /**
     * Entfernt das Verschlüsselungspräfix von einer Passphrase.
     * 
     * @param passphrase Die Passphrase
     * @return Passphrase ohne Präfix
     */
    public static String removePrefix(String passphrase) {
        if (passphrase != null && passphrase.startsWith(ENCRYPTED_PREFIX)) {
            return passphrase.substring(ENCRYPTED_PREFIX.length());
        }
        return passphrase;
    }
}

