#!/bin/bash

# Skript zum Generieren eines Verschlüsselungsschlüssels
# Verwendung: ./generate-key.sh

set -e

JAR_FILE="build/libs/mcp-sqlite-0.1.0.jar"

# Prüfe ob JAR existiert
if [ ! -f "$JAR_FILE" ]; then
    echo "Fehler: $JAR_FILE nicht gefunden."
    echo "Bitte bauen Sie das Projekt zuerst: ./gradlew build"
    exit 1
fi

echo "Generiere neuen Verschlüsselungsschlüssel..."
echo ""

# Generiere den Schlüssel mit dem GenerateKey Tool
KEY=$(java -cp "$JAR_FILE" com.example.mcp.sqlite.util.GenerateKey 2>/dev/null)

if [ -z "$KEY" ]; then
    echo "Fehler: Konnte keinen Schlüssel generieren."
    echo ""
    echo "Bitte stellen Sie sicher, dass das Projekt gebaut wurde:"
    echo "  ./gradlew build"
    exit 1
fi

echo "Verschlüsselungsschlüssel:"
echo "$KEY"
echo ""
echo "Setzen Sie diese Umgebungsvariable:"
echo "  export MCP_SQLITE_ENCRYPTION_KEY=\"$KEY\""
echo ""
echo "Oder fügen Sie sie zu Ihrer Shell-Konfiguration hinzu (~/.bashrc, ~/.zshrc, etc.):"
echo "  echo 'export MCP_SQLITE_ENCRYPTION_KEY=\"$KEY\"' >> ~/.bashrc"
echo ""
echo "Dann können Sie Ihre Passphrase verschlüsseln mit:"
echo "  ./encrypt-passphrase.sh \"ihre-passphrase\""
echo ""

