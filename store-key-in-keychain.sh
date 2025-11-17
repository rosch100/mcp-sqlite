#!/bin/bash

# Skript zum Speichern eines Verschlüsselungsschlüssels in der macOS Keychain
# Verwendung: ./store-key-in-keychain.sh [<schlüssel>|--generate]

set -e

JAR_FILE="build/libs/mcp-sqlite-0.1.0.jar"

# Prüfe ob macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "Fehler: Dieses Skript funktioniert nur auf macOS."
    exit 1
fi

# Prüfe ob JAR existiert
if [ ! -f "$JAR_FILE" ]; then
    echo "Fehler: $JAR_FILE nicht gefunden."
    echo "Bitte bauen Sie das Projekt zuerst: ./gradlew build"
    exit 1
fi

if [ $# -eq 0 ]; then
    echo "Verwendung: $0 [<schlüssel>|--generate]"
    echo ""
    echo "Beispiele:"
    echo "  # Automatisch generieren und speichern:"
    echo "  $0 --generate"
    echo ""
    echo "  # Mit vorhandenem Schlüssel:"
    echo "  $0 \"<base64-schlüssel>\""
    echo ""
    exit 1
fi

if [ "$1" = "--generate" ]; then
    echo "Generiere neuen Schlüssel und speichere ihn in der Keychain..."
    java -cp "$JAR_FILE" com.example.mcp.sqlite.util.StoreKeyInKeychain --generate
else
    echo "Speichere Schlüssel in der Keychain..."
    java -cp "$JAR_FILE" com.example.mcp.sqlite.util.StoreKeyInKeychain "$1"
fi

