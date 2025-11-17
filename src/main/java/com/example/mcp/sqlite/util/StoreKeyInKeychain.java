package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.KeychainKeyStore;
import com.example.mcp.sqlite.config.PassphraseEncryption;

import java.io.IOException;

/**
 * CLI tool for storing an encryption key in macOS Keychain.
 * 
 * Usage:
 *   java -cp <classpath> com.example.mcp.sqlite.util.StoreKeyInKeychain <key>
 * 
 * Or generate and store automatically:
 *   java -cp <classpath> com.example.mcp.sqlite.util.StoreKeyInKeychain --generate
 */
public class StoreKeyInKeychain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: StoreKeyInKeychain <key>");
            System.err.println("       StoreKeyInKeychain --generate");
            System.err.println("");
            System.err.println("Examples:");
            System.err.println("  # With existing key:");
            System.err.println("  java -cp ... StoreKeyInKeychain \"<base64-key>\"");
            System.err.println("");
            System.err.println("  # Automatically generate and store:");
            System.err.println("  java -cp ... StoreKeyInKeychain --generate");
            System.err.println("");
            System.err.println("Note: Only works on macOS with available Keychain.");
            System.exit(1);
        }
        
        if (!KeychainKeyStore.isAvailable()) {
            System.err.println("Error: macOS Keychain is not available.");
            System.err.println("This tool only works on macOS.");
            System.exit(1);
        }
        
        String keyBase64;
        
        if ("--generate".equals(args[0])) {
            // Generate new key
            System.out.println("Generating new encryption key...");
            keyBase64 = PassphraseEncryption.generateKey();
            System.out.println("Generated key: " + keyBase64);
        } else {
            keyBase64 = args[0];
        }
        
        // Validate key
        try {
            PassphraseEncryption.fromBase64Key(keyBase64);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: Invalid key - " + e.getMessage());
            System.exit(1);
        }
        
        // Store in Keychain
        try {
            KeychainKeyStore.storeKey(keyBase64);
            System.out.println("");
            System.out.println("✓ Key successfully stored in macOS Keychain!");
            System.out.println("");
            System.out.println("The key will be automatically used when no");
            System.out.println("MCP_SQLITE_ENCRYPTION_KEY environment variable is set.");
            System.out.println("");
            System.out.println("You can now use the key from Keychain.");
            System.out.println("To delete the key:");
            System.out.println("  security delete-generic-password -a encryption-key -s mcp-sqlite");
        } catch (IOException e) {
            System.err.println("Error storing in Keychain: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
