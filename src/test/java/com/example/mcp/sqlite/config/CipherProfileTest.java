package com.example.mcp.sqlite.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CipherProfileTest {

    @Test
    void sqlCipher4DefaultsExposesExpectedValues() {
        CipherProfile profile = CipherProfile.sqlCipher4Defaults();
        assertEquals("SQLCipher 4 defaults", profile.name());
        assertEquals(4096, profile.pageSize());
        assertEquals(256_000, profile.kdfIterations());
        assertEquals("HMAC_SHA512", profile.hmacAlgorithm());
        assertEquals("PBKDF2_HMAC_SHA512", profile.kdfAlgorithm());
    }
}
