package com.example.mcp.sqlite.util;

import com.example.mcp.sqlite.config.PassphraseEncryption;

/**
 * CLI tool for generating an encryption key.
 * 
 * Usage:
 *   java -cp <classpath> com.example.mcp.sqlite.util.GenerateKey
 */
public class GenerateKey {
    public static void main(String[] args) {
        try {
            String key = PassphraseEncryption.generateKey();
            System.out.println(key);
        } catch (Exception e) {
            System.err.println("Error generating key: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
