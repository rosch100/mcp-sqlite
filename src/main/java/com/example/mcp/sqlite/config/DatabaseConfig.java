package com.example.mcp.sqlite.config;

import java.nio.file.Path;
import java.util.Objects;

public record DatabaseConfig(Path databasePath, String passphrase, CipherProfile cipherProfile) {

    public DatabaseConfig {
        Objects.requireNonNull(databasePath, "databasePath");
        Objects.requireNonNull(passphrase, "passphrase");
        Objects.requireNonNull(cipherProfile, "cipherProfile");
    }
}
