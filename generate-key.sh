#!/bin/bash

# Script to generate an encryption key
# Usage: ./generate-key.sh

set -e

JAR_FILE="build/libs/mcp-sqlite-0.2.1.jar"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found."
    echo "Please build the project first: ./gradlew build"
    exit 1
fi

echo "Generating new encryption key..."
echo ""

# Generate the key using the GenerateKey tool
KEY=$(java -cp "$JAR_FILE" com.example.mcp.sqlite.util.GenerateKey 2>/dev/null)

if [ -z "$KEY" ]; then
    echo "Error: Could not generate key."
    echo ""
    echo "Please make sure the project is built:"
    echo "  ./gradlew build"
    exit 1
fi

echo "Encryption key:"
echo "$KEY"
echo ""
echo "Set this environment variable:"
echo "  export MCP_SQLITE_ENCRYPTION_KEY=\"$KEY\""
echo ""
echo "Or add it to your shell configuration (~/.bashrc, ~/.zshrc, etc.):"
echo "  echo 'export MCP_SQLITE_ENCRYPTION_KEY=\"$KEY\"' >> ~/.bashrc"
echo ""
echo "Then you can encrypt your passphrase with:"
echo "  ./encrypt-passphrase.sh \"your-passphrase\""
echo ""
