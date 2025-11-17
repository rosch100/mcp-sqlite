package com.example.mcp.sqlite.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keychain-basierte Schlüsselspeicherung für macOS.
 * Nutzt das macOS security Command-Line Tool für den Zugriff auf die Keychain.
 */
public final class KeychainKeyStore {
    private static final String KEYCHAIN_SERVICE = "mcp-sqlite";
    private static final String KEYCHAIN_ACCOUNT = "encryption-key";
    private static final String SECURITY_CMD = "/usr/bin/security";
    
    /**
     * Prüft, ob macOS Keychain verfügbar ist.
     * 
     * @return true wenn Keychain verfügbar ist
     */
    public static boolean isAvailable() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        // Prüfe auf macOS (kann "Mac OS X", "macOS", oder ähnlich sein)
        // Auch prüfe auf "darwin" für Unix-Systeme
        boolean isMacOS = osName.contains("mac") || osName.contains("darwin");
        
        if (!isMacOS) {
            return false;
        }
        
        // Prüfe ob security Command verfügbar ist
        // Verwende "help" statt "--version", da --version nicht unterstützt wird
        try {
            Process process = new ProcessBuilder(SECURITY_CMD, "help").start();
            // Warte kurz und prüfe ob der Prozess läuft
            Thread.sleep(100);
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                // Exit Code 0 oder 2 sind beide OK (2 bedeutet nur, dass kein Command angegeben wurde)
                return exitCode == 0 || exitCode == 2;
            }
            process.destroy();
            return true; // Prozess läuft noch, also ist das Tool verfügbar
        } catch (Exception e) {
            // Wenn die Datei nicht existiert oder nicht ausführbar ist, ist Keychain nicht verfügbar
            return false;
        }
    }
    
    /**
     * Speichert einen Verschlüsselungsschlüssel in der macOS Keychain.
     * 
     * @param keyBase64 Der Base64-kodierte Verschlüsselungsschlüssel
     * @throws IOException wenn der Schlüssel nicht gespeichert werden kann
     */
    public static void storeKey(String keyBase64) throws IOException {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("macOS Keychain ist nicht verfügbar");
        }
        
        if (keyBase64 == null || keyBase64.isEmpty()) {
            throw new IllegalArgumentException("Schlüssel darf nicht leer sein");
        }
        
        try {
            // Entferne vorhandenen Eintrag falls vorhanden
            deleteKey();
            
            // Speichere neuen Schlüssel
            ProcessBuilder pb = new ProcessBuilder(
                SECURITY_CMD,
                "add-generic-password",
                "-a", KEYCHAIN_ACCOUNT,
                "-s", KEYCHAIN_SERVICE,
                "-w", keyBase64,
                "-U" // Update falls vorhanden
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // Lese Fehlerausgabe
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                    throw new IOException("Fehler beim Speichern in Keychain: " + error.toString());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Unterbrochen beim Speichern in Keychain", e);
        }
    }
    
    /**
     * Lädt einen Verschlüsselungsschlüssel aus der macOS Keychain.
     * 
     * @return Der Base64-kodierte Verschlüsselungsschlüssel oder null wenn nicht gefunden
     * @throws IOException wenn ein Fehler beim Zugriff auf die Keychain auftritt
     */
    public static String loadKey() throws IOException {
        if (!isAvailable()) {
            return null;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                SECURITY_CMD,
                "find-generic-password",
                "-a", KEYCHAIN_ACCOUNT,
                "-s", KEYCHAIN_SERVICE,
                "-w" // Nur Passwort ausgeben
            );
            
            Process process = pb.start();
            
            // Lese Ausgabe
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            // Warte auf Prozess und lese Fehlerausgabe falls vorhanden
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // Prüfe ob der Eintrag einfach nicht existiert
                if (exitCode == 44) { // 44 = item not found
                    return null;
                }
                // Lese Fehlerausgabe für besseres Debugging
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorOutput.append(errorLine).append("\n");
                    }
                }
                // Wenn Fehlerausgabe vorhanden ist, werfe Exception
                if (errorOutput.length() > 0) {
                    throw new IOException("Fehler beim Laden aus Keychain (Exit Code: " + exitCode + "): " + errorOutput.toString());
                }
                // Andere Fehler ignorieren und null zurückgeben
                return null;
            }
            
            String key = output.toString().trim();
            return key.isEmpty() ? null : key;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Unterbrochen beim Laden aus Keychain", e);
        }
    }
    
    /**
     * Löscht einen Verschlüsselungsschlüssel aus der macOS Keychain.
     * 
     * @throws IOException wenn der Schlüssel nicht gelöscht werden kann
     */
    public static void deleteKey() throws IOException {
        if (!isAvailable()) {
            return;
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                SECURITY_CMD,
                "delete-generic-password",
                "-a", KEYCHAIN_ACCOUNT,
                "-s", KEYCHAIN_SERVICE
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            // Exit Code 44 bedeutet "item not found" - das ist OK
            if (exitCode != 0 && exitCode != 44) {
                throw new IOException("Fehler beim Löschen aus Keychain (Exit Code: " + exitCode + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Unterbrochen beim Löschen aus Keychain", e);
        }
    }
    
    /**
     * Prüft, ob ein Schlüssel in der Keychain gespeichert ist.
     * 
     * @return true wenn ein Schlüssel gefunden wurde
     */
    public static boolean hasKey() {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            String key = loadKey();
            return key != null && !key.isEmpty();
        } catch (IOException e) {
            return false;
        }
    }
}

