#!/bin/bash
# Script to retrieve or generate the encryption key

set -e

echo "Checking Keychain..."
KEY=$(security find-generic-password -s "mcp-sqlite" -a "encryption-key" -w 2>/dev/null || echo "")

if [ -n "$KEY" ]; then
    echo "✅ Key found in Keychain!"
    echo ""
    echo "Use this key in your Cursor configuration:"
    echo ""
    echo "$KEY"
    echo ""
    echo "Or add it automatically to the configuration with:"
    echo "./get-encryption-key.sh --update-config"
else
    echo "⚠️  No key found in Keychain."
    echo ""
    echo "You need to generate a new key:"
    echo "1. Run: ./store-key-in-keychain.sh --generate"
    echo "2. Then run this script again"
fi

if [ "$1" = "--update-config" ] && [ -n "$KEY" ]; then
    echo ""
    echo "Updating Cursor configuration..."
    python3 << PYTHON
import json
import os

config_path = os.path.expanduser("~/.cursor/mcp.json")
key = "$KEY"

with open(config_path, 'r') as f:
    config = json.load(f)

if "encrypted-sqlite" in config["mcpServers"]:
    if "env" not in config["mcpServers"]["encrypted-sqlite"]:
        config["mcpServers"]["encrypted-sqlite"]["env"] = {}
    config["mcpServers"]["encrypted-sqlite"]["env"]["MCP_SQLITE_ENCRYPTION_KEY"] = key

with open(config_path, 'w') as f:
    json.dump(config, f, indent=2)

print("✅ Configuration updated!")
PYTHON
fi
