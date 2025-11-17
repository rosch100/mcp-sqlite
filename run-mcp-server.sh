#!/bin/sh
cd "$(dirname "$0")" || exit 1
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export PATH="/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin:/opt/homebrew/sbin:${PATH}"
BINARY="./build/install/mcp-sqlite/bin/mcp-sqlite"
if [ ! -f "$BINARY" ]; then
    ./gradlew -q --console=plain installDist >&2 || exit 1
fi
exec "$BINARY" "$@"

