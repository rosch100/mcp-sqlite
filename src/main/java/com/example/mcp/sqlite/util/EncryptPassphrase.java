package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.PassphraseEncryption;

/**
 * CLI tool for encrypting passphrases.
 * 
 * Usage:
 *   java -cp <classpath> com.example.mcp.sqlite.util.EncryptPassphrase <passphrase>
 * 
 * Or with environment variable for the key:
 *   MCP_SQLITE_ENCRYPTION_KEY=<key> java -cp <classpath> com.example.mcp.sqlite.util.EncryptPassphrase <passphrase>
 */
public class EncryptPassphrase {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: EncryptPassphrase <passphrase>");
            System.err.println("");
            System.err.println("Example:");
            System.err.println("  java -cp ... EncryptPassphrase \"my-passphrase\"");
            System.err.println("");
            System.err.println("Note: Set MCP_SQLITE_ENCRYPTION_KEY as an environment variable");
            System.err.println("      to use a custom encryption key.");
            System.exit(1);
        }
        
        String passphrase = args[0];
        
        try {
            PassphraseEncryption encryption = PassphraseEncryption.fromEnvironment();
            String encrypted = encryption.encrypt(passphrase);
            
            System.out.println("Encrypted passphrase:");
            System.out.println(encrypted);
            System.out.println("");
            System.out.println("Use this encrypted passphrase in your configuration");
            System.out.println("with the prefix 'encrypted:' (already included).");
        } catch (Exception e) {
            System.err.println("Error encrypting: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
