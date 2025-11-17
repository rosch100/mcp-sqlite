#!/bin/bash

# Skript zum Verschlüsseln von Passphrasen für den MCP SQLite Server
# Verwendung: ./encrypt-passphrase.sh "meine-passphrase"

set -e

if [ $# -eq 0 ]; then
    echo "Verwendung: $0 <passphrase>"
    echo ""
    echo "Beispiel:"
    echo "  $0 \"meine-geheime-passphrase\""
    echo ""
    echo "Hinweis: Stellen Sie sicher, dass MCP_SQLITE_ENCRYPTION_KEY gesetzt ist:"
    echo "  export MCP_SQLITE_ENCRYPTION_KEY=\"<ihr-schlüssel>\""
    exit 1
fi

PASSPHRASE="$1"
JAR_FILE="build/libs/mcp-sqlite-0.1.0.jar"

# Prüfe ob JAR existiert
if [ ! -f "$JAR_FILE" ]; then
    echo "Fehler: $JAR_FILE nicht gefunden."
    echo "Bitte bauen Sie das Projekt zuerst: ./gradlew build"
    exit 1
fi

# Prüfe ob Schlüssel gesetzt ist (Umgebungsvariable oder Keychain)
# Das Java-Tool prüft automatisch die Keychain, wenn keine Umgebungsvariable gesetzt ist
# Daher müssen wir hier keine Prüfung machen - das Java-Tool gibt eine hilfreiche Fehlermeldung

# Verschlüssele die Passphrase
echo "Verschlüssele Passphrase..."
ENCRYPTED=$(java -cp "$JAR_FILE" com.example.mcp.sqlite.util.EncryptPassphrase "$PASSPHRASE" 2>&1 | grep "encrypted:" | head -1)

if [ -z "$ENCRYPTED" ]; then
    echo "Fehler beim Verschlüsseln der Passphrase."
    echo ""
    echo "Stellen Sie sicher, dass:"
    echo "  1. MCP_SQLITE_ENCRYPTION_KEY gesetzt ist"
    echo "  2. Der Schlüssel gültig ist (32 Bytes Base64-kodiert)"
    exit 1
fi

echo ""
echo "Verschlüsselte Passphrase:"
echo "$ENCRYPTED"
echo ""
echo "Verwenden Sie diese in Ihrer Konfiguration (z.B. in ~/.cursor/mcp.json):"
echo "  \"passphrase\": \"$ENCRYPTED\""
echo ""

# Prüfe ob Keychain verwendet wird
if [ -z "$MCP_SQLITE_ENCRYPTION_KEY" ]; then
    echo "ℹ️  Der Schlüssel wird aus der macOS Keychain geladen."
    echo "   Keine env-Sektion nötig!"
else
    echo "Vergessen Sie nicht, auch MCP_SQLITE_ENCRYPTION_KEY in der env-Sektion zu setzen:"
    echo "  \"env\": {"
    echo "    \"MCP_SQLITE_ENCRYPTION_KEY\": \"$MCP_SQLITE_ENCRYPTION_KEY\""
    echo "  }"
fi
echo ""

