#!/bin/bash
# Script to rotate encryption keys and re-encrypt all passphrases
# This should be run immediately after keys have been exposed

set -e

JAR_FILE="build/libs/mcp-sqlite-0.2.2.jar"

echo "⚠️  ENCRYPTION KEY ROTATION SCRIPT"
echo "=================================="
echo ""
echo "This script will help you:"
echo "1. Generate a new encryption key"
echo "2. Store it securely (macOS Keychain or environment variable)"
echo "3. Re-encrypt all your passphrases with the new key"
echo ""
echo "⚠️  IMPORTANT: After rotation, update all configuration files with new encrypted passphrases!"
echo ""

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: $JAR_FILE not found."
    echo "Please build the project first: ./gradlew build"
    exit 1
fi

# Step 1: Generate new key
echo "Step 1: Generating new encryption key..."
NEW_KEY=$(java -cp "$JAR_FILE" com.example.mcp.sqlite.util.GenerateKey 2>/dev/null)

if [ -z "$NEW_KEY" ]; then
    echo "Error: Could not generate new key."
    exit 1
fi

echo "✅ New encryption key generated"
echo ""

# Step 2: Ask how to store the key
echo "Step 2: How do you want to store the new key?"
echo "1) macOS Keychain (recommended for macOS)"
echo "2) Environment variable (for shell configuration)"
echo "3) Just display it (you'll store it manually)"
read -p "Choose option (1-3): " STORAGE_OPTION

case $STORAGE_OPTION in
    1)
        if [[ "$OSTYPE" != "darwin"* ]]; then
            echo "Error: macOS Keychain is only available on macOS."
            exit 1
        fi
        echo ""
        echo "Storing key in macOS Keychain..."
        java -cp "$JAR_FILE" com.example.mcp.sqlite.util.StoreKeyInKeychain "$NEW_KEY"
        echo "✅ Key stored in macOS Keychain"
        echo ""
        echo "The key will be automatically loaded from Keychain."
        ;;
    2)
        echo ""
        echo "Add this to your shell configuration (~/.zshrc, ~/.bashrc, etc.):"
        echo ""
        echo "export MCP_SQLITE_ENCRYPTION_KEY=\"$NEW_KEY\""
        echo ""
        read -p "Press Enter after you've added it to your shell config..."
        echo ""
        echo "⚠️  Don't forget to source your shell config or restart your terminal!"
        ;;
    3)
        echo ""
        echo "Your new encryption key:"
        echo "$NEW_KEY"
        echo ""
        echo "⚠️  Store this securely! You'll need it to encrypt your passphrases."
        ;;
    *)
        echo "Invalid option. Exiting."
        exit 1
        ;;
esac

echo ""
echo "Step 3: Re-encrypt your passphrases"
echo "===================================="
echo ""
echo "For each database passphrase you need to encrypt:"
echo ""
echo "1. Set the new key as environment variable:"
if [ "$STORAGE_OPTION" = "1" ]; then
    echo "   (Key is already in Keychain, but you can also set it manually)"
fi
echo "   export MCP_SQLITE_ENCRYPTION_KEY=\"$NEW_KEY\""
echo ""
echo "2. Encrypt each passphrase:"
echo "   ./encrypt-passphrase.sh \"your-plain-passphrase\""
echo ""
echo "3. Update your configuration files (mcp.json, etc.) with the new encrypted passphrases"
echo ""
echo "Example:"
echo "  ./encrypt-passphrase.sh \"my-database-password\""
echo ""
echo "⚠️  IMPORTANT REMINDERS:"
echo "- Delete the old key from macOS Keychain (if stored there)"
echo "- Remove old encrypted passphrases from configuration files"
echo "- Update all configuration files with new encrypted passphrases"
echo "- Test that everything works with the new key"
echo ""
echo "✅ Key rotation process started!"
echo "   Complete the steps above to finish the rotation."

