package com.example.mcp.sqlite.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlIdentifierValidatorTest {

    @Test
    void testValidIdentifiers() {
        assertTrue(SqlIdentifierValidator.isValidIdentifier("users"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("user_table"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("_private"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("table123"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("T1"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("a"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("A"));
        assertTrue(SqlIdentifierValidator.isValidIdentifier("table$name"));
    }

    @Test
    void testInvalidIdentifiers() {
        assertFalse(SqlIdentifierValidator.isValidIdentifier(null));
        assertFalse(SqlIdentifierValidator.isValidIdentifier(""));
        assertFalse(SqlIdentifierValidator.isValidIdentifier("123table")); // Starts with digit
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table-name")); // Contains hyphen
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table name")); // Contains space
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table.name")); // Contains dot
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table'name")); // Contains quote
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table\"name")); // Contains quote
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table;name")); // Contains semicolon
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table--name")); // Contains comment
        assertFalse(SqlIdentifierValidator.isValidIdentifier("table/*name*/")); // Contains comment
    }

    @Test
    void testMaxLength() {
        // Valid: exactly 64 characters
        String valid64 = "a" + "b".repeat(63);
        assertTrue(SqlIdentifierValidator.isValidIdentifier(valid64));
        
        // Invalid: exceeds 64 characters
        String invalid65 = "a" + "b".repeat(64);
        assertFalse(SqlIdentifierValidator.isValidIdentifier(invalid65));
    }

    @Test
    void testValidateIdentifierThrows() {
        assertThrows(IllegalArgumentException.class, () -> 
            SqlIdentifierValidator.validateIdentifier(null, "test"));
        assertThrows(IllegalArgumentException.class, () -> 
            SqlIdentifierValidator.validateIdentifier("", "test"));
        assertThrows(IllegalArgumentException.class, () -> 
            SqlIdentifierValidator.validateIdentifier("123table", "test"));
        assertThrows(IllegalArgumentException.class, () -> 
            SqlIdentifierValidator.validateIdentifier("table-name", "test"));
    }

    @Test
    void testValidateIdentifierDoesNotThrow() {
        assertDoesNotThrow(() -> 
            SqlIdentifierValidator.validateIdentifier("valid_table", "test"));
        assertDoesNotThrow(() -> 
            SqlIdentifierValidator.validateIdentifier("users", "test"));
    }

    @Test
    void testValidateIdentifiers() {
        assertDoesNotThrow(() -> 
            SqlIdentifierValidator.validateIdentifiers(null, "test")); // null is allowed
        
        assertDoesNotThrow(() -> 
            SqlIdentifierValidator.validateIdentifiers(
                java.util.List.of("col1", "col2", "col3"), "test"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            SqlIdentifierValidator.validateIdentifiers(
                java.util.List.of("valid", "123invalid"), "test"));
    }
}

