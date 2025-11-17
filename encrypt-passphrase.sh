#!/bin/bash

# Script to encrypt passphrases for the MCP SQLite Server
# Usage: ./encrypt-passphrase.sh "my-passphrase"

set -e

if [ $# -eq 0 ]; then
    echo "Usage: $0 <passphrase>"
    echo ""
    echo "Example:"
    echo "  $0 \"my-secret-passphrase\""
    echo ""
    echo "Note: Make sure MCP_SQLITE_ENCRYPTION_KEY is set:"
    echo "  export MCP_SQLITE_ENCRYPTION_KEY=\"<your-key>\""
    exit 1
fi

PASSPHRASE="$1"
JAR_FILE="build/libs/mcp-sqlite-0.2.1.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found."
    echo "Please build the project first: ./gradlew build"
    exit 1
fi

# Check if key is set (environment variable or Keychain)
# The Java tool automatically checks the Keychain if no environment variable is set
# So we don't need to check here - the Java tool will provide a helpful error message

# Encrypt the passphrase
echo "Encrypting passphrase..."
ENCRYPTED=$(java -cp "$JAR_FILE" com.example.mcp.sqlite.util.EncryptPassphrase "$PASSPHRASE" 2>&1 | grep "encrypted:" | head -1)

if [ -z "$ENCRYPTED" ]; then
    echo "Error encrypting passphrase."
    echo ""
    echo "Make sure that:"
    echo "  1. MCP_SQLITE_ENCRYPTION_KEY is set"
    echo "  2. The key is valid (32 bytes Base64-encoded)"
    exit 1
fi

echo ""
echo "Encrypted passphrase:"
echo "$ENCRYPTED"
echo ""
echo "Use this in your configuration (e.g., in ~/.cursor/mcp.json):"
echo "  \"passphrase\": \"$ENCRYPTED\""
echo ""

# Check if Keychain is being used
if [ -z "$MCP_SQLITE_ENCRYPTION_KEY" ]; then
    echo "ℹ️  The key is being loaded from macOS Keychain."
    echo "   No env section needed!"
else
    echo "Don't forget to also set MCP_SQLITE_ENCRYPTION_KEY in the env section:"
    echo "  \"env\": {"
    echo "    \"MCP_SQLITE_ENCRYPTION_KEY\": \"$MCP_SQLITE_ENCRYPTION_KEY\""
    echo "  }"
fi
echo ""
