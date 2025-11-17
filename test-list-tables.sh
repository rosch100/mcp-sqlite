#!/bin/bash
# Test-Skript zum Testen des MCP-Servers mit listTables

# Konfiguration
DB_PATH="${1:-/path/to/your/database.sqlite}"
PASSPHRASE="${2:-your-passphrase}"

# Prüfe ob Server gebaut ist
BINARY="./build/install/mcp-sqlite/bin/mcp-sqlite"
if [ ! -f "$BINARY" ]; then
    echo "Server nicht gefunden. Baue Server..."
    ./gradlew installDist
fi

echo "=== Teste MCP Server: listTables ==="
echo "DB Path: $DB_PATH"
echo ""

# Erstelle JSON für initialize Request
INIT_REQUEST='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0.0"}}}'

# Erstelle JSON für tools/list Request
TOOLS_LIST_REQUEST='{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# Erstelle JSON für tools/call mit listTables
LIST_TABLES_REQUEST=$(cat <<EOF
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "listTables",
    "arguments": {
      "dbPath": "$DB_PATH",
      "passphrase": "$PASSPHRASE",
      "includeColumns": true
    }
  }
}
EOF
)

# Starte Server im Hintergrund und kommuniziere über STDIO
echo "Starte Server..."
"$BINARY" --args "{\"dbPath\":\"$DB_PATH\",\"passphrase\":\"$PASSPHRASE\"}" 2>&1 | tee mcp-test.log &
SERVER_PID=$!

# Warte kurz, damit Server startet
sleep 1

# Sende Requests
echo ""
echo "=== Sende initialize Request ==="
echo "$INIT_REQUEST" | "$BINARY" --args "{\"dbPath\":\"$DB_PATH\",\"passphrase\":\"$PASSPHRASE\"}" 2>&1 | head -20

echo ""
echo "=== Sende tools/list Request ==="
echo "$TOOLS_LIST_REQUEST" | "$BINARY" --args "{\"dbPath\":\"$DB_PATH\",\"passphrase\":\"$PASSPHRASE\"}" 2>&1 | head -20

echo ""
echo "=== Sende listTables Request ==="
echo "$LIST_TABLES_REQUEST" | "$BINARY" --args "{\"dbPath\":\"$DB_PATH\",\"passphrase\":\"$PASSPHRASE\"}" 2>&1 | grep -v "^\[.*\] \[DEBUG\]" | head -50

echo ""
echo "=== Test abgeschlossen ==="
echo "Vollständige Logs siehe: mcp-test.log"

