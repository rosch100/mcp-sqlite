package com.example.mcp.sqlite.util;

import java.util.regex.Pattern;

/**
 * Utility class for validating SQL identifiers (table names, column names) to prevent SQL injection.
 * SQLite identifiers can contain letters, digits, underscores, and must not contain quotes or other special characters.
 */
public class SqlIdentifierValidator {
    // SQLite identifier pattern: letters, digits, underscores, and dollar signs
    // Must start with a letter or underscore
    // Maximum length: SQLite allows up to 64 characters for identifiers
    private static final Pattern VALID_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_$]{0,63}$");
    
    // Maximum length for SQLite identifiers
    private static final int MAX_IDENTIFIER_LENGTH = 64;
    
    private SqlIdentifierValidator() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates a SQL identifier (table name or column name).
     * 
     * @param identifier The identifier to validate
     * @return true if the identifier is valid, false otherwise
     */
    public static boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return false;
        }
        
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            return false;
        }
        
        return VALID_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }
    
    /**
     * Validates a SQL identifier and throws an exception if invalid.
     * 
     * @param identifier The identifier to validate
     * @param parameterName The name of the parameter (for error messages)
     * @throws IllegalArgumentException if the identifier is invalid
     */
    public static void validateIdentifier(String identifier, String parameterName) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be null or empty");
        }
        
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            throw new IllegalArgumentException(parameterName + " exceeds maximum length of " + MAX_IDENTIFIER_LENGTH + " characters");
        }
        
        if (!VALID_IDENTIFIER_PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException(parameterName + " contains invalid characters. " +
                    "Identifiers must start with a letter or underscore and contain only letters, digits, underscores, and dollar signs");
        }
    }
    
    /**
     * Validates multiple identifiers (e.g., column names).
     * 
     * @param identifiers The identifiers to validate
     * @param parameterName The name of the parameter (for error messages)
     * @throws IllegalArgumentException if any identifier is invalid
     */
    public static void validateIdentifiers(java.util.List<String> identifiers, String parameterName) {
        if (identifiers == null) {
            return; // null is allowed for optional parameters
        }
        
        for (String identifier : identifiers) {
            validateIdentifier(identifier, parameterName);
        }
    }
}

