#!/bin/sh
# Wrapper script to run the MCP SQLite server
# Automatically builds the project if needed
cd "$(dirname "$0")" || exit 1

# Use JAVA_HOME if set, otherwise let the system find Java
# The Gradle wrapper will handle Java detection if JAVA_HOME is not set
if [ -z "$JAVA_HOME" ]; then
    # Try common macOS Homebrew location (optional)
    if [ -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]; then
        export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    fi
fi

BINARY="./build/install/mcp-sqlite/bin/mcp-sqlite"
if [ ! -f "$BINARY" ]; then
    ./gradlew -q --console=plain installDist >&2 || exit 1
fi
exec "$BINARY" "$@"

