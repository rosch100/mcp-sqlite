#!/bin/bash

# Script to store an encryption key in macOS Keychain
# Usage: ./store-key-in-keychain.sh [<key>|--generate]

set -e

JAR_FILE="build/libs/mcp-sqlite-0.2.1.jar"

# Check if macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "Error: This script only works on macOS."
    exit 1
fi

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found."
    echo "Please build the project first: ./gradlew build"
    exit 1
fi

if [ $# -eq 0 ]; then
    echo "Usage: $0 [<key>|--generate]"
    echo ""
    echo "Examples:"
    echo "  # Automatically generate and store:"
    echo "  $0 --generate"
    echo ""
    echo "  # With existing key:"
    echo "  $0 \"<base64-key>\""
    echo ""
    exit 1
fi

if [ "$1" = "--generate" ]; then
    echo "Generating new key and storing it in Keychain..."
    java -cp "$JAR_FILE" com.example.mcp.sqlite.util.StoreKeyInKeychain --generate
else
    echo "Storing key in Keychain..."
    java -cp "$JAR_FILE" com.example.mcp.sqlite.util.StoreKeyInKeychain "$1"
fi
