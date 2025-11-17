package com.example.mcp.sqlite.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keychain-based key storage for macOS.
 * Uses the macOS security command-line tool to access the Keychain.
 */
public final class KeychainKeyStore {
    private static final String KEYCHAIN_SERVICE = "mcp-sqlite";
    private static final String KEYCHAIN_ACCOUNT = "encryption-key";
    private static final String SECURITY_CMD = "/usr/bin/security";
    
    /**
     * Checks if macOS Keychain is available.
     * 
     * @return true if Keychain is available
     */
    public static boolean isAvailable() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        // Check for macOS (can be "Mac OS X", "macOS", or similar)
        // Also check for "darwin" for Unix systems
        boolean isMacOS = osName.contains("mac") || osName.contains("darwin");
        
        if (!isMacOS) {
            return false;
        }
        
        // Check if security command is available
        // Use "help" instead of "--version", as --version is not supported
        try {
            Process process = new ProcessBuilder(SECURITY_CMD, "help").start();
            // Wait briefly and check if the process is running
            Thread.sleep(100);
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                // Exit codes 0 or 2 are both OK (2 just means no command was specified)
                return exitCode == 0 || exitCode == 2;
            }
            process.destroy();
            return true; // Process is still running, so the tool is available
        } catch (Exception e) {
            // If the file doesn't exist or is not executable, Keychain is not available
            return false;
        }
    }
    
    /**
     * Stores an encryption key in the macOS Keychain.
     * 
     * @param keyBase64 The Base64-encoded encryption key
     * @throws IOException if the key cannot be stored
     */
    public static void storeKey(String keyBase64) throws IOException {
        if (!isAvailable()) {
            throw new UnsupportedOperationException("macOS Keychain is not available");
        }
        
        if (keyBase64 == null || keyBase64.isEmpty()) {
            throw new IllegalArgumentException("Key must not be empty");
        }
        
        try {
            // Remove existing entry if present
            deleteKey();
            
            // Store new key
            ProcessBuilder pb = new ProcessBuilder(
                SECURITY_CMD,
                "add-generic-password",
                "-a", KEYCHAIN_ACCOUNT,
                "-s", KEYCHAIN_SERVICE,
                "-w", keyBase64,
                "-U" // Update if present
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // Read error output
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line).append("\n");
                    }
                    throw new IOException("Error storing in Keychain: " + error.toString());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while storing in Keychain", e);
        }
    }
    
    /**
     * Loads an encryption key from the macOS Keychain.
     * 
     * @return The Base64-encoded encryption key or null if not found
     * @throws IOException if an error occurs while accessing the Keychain
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
                "-w" // Output password only
            );
            
            Process process = pb.start();
            
            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            // Wait for process and read error output if present
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                // Check if the entry simply doesn't exist
                if (exitCode == 44) { // 44 = item not found
                    return null;
                }
                // Read error output for better debugging
                StringBuilder errorOutput = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorOutput.append(errorLine).append("\n");
                    }
                }
                // If error output is present, throw exception
                if (errorOutput.length() > 0) {
                    throw new IOException("Error loading from Keychain (Exit Code: " + exitCode + "): " + errorOutput.toString());
                }
                // Ignore other errors and return null
                return null;
            }
            
            String key = output.toString().trim();
            return key.isEmpty() ? null : key;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while loading from Keychain", e);
        }
    }
    
    /**
     * Deletes an encryption key from the macOS Keychain.
     * 
     * @throws IOException if the key cannot be deleted
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
            
            // Exit code 44 means "item not found" - that's OK
            if (exitCode != 0 && exitCode != 44) {
                throw new IOException("Error deleting from Keychain (Exit Code: " + exitCode + ")");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while deleting from Keychain", e);
        }
    }
    
    /**
     * Checks if a key is stored in the Keychain.
     * 
     * @return true if a key was found
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
