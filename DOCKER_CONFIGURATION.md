# Docker Configuration for MCP SQLite Server

This guide explains how to configure the MCP SQLite Server using Docker Desktop in various MCP clients.

## Prerequisites

1. **Docker Desktop** installed and running
2. **Docker image** pulled: `ghcr.io/rosch100/mcp-sqlite:0.2.2`
3. **Encrypted SQLite database** file ready

## Pull the Docker Image

```bash
docker pull ghcr.io/rosch100/mcp-sqlite:0.2.2
```

Or use the latest version:

```bash
docker pull ghcr.io/rosch100/mcp-sqlite:latest
```

## Configuration Examples

### Cursor (macOS/Linux/Windows)

Edit `~/.cursor/mcp.json` (or `%APPDATA%\Cursor\mcp.json` on Windows):

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

**Important Notes:**
- Replace `/path/to/your/database.sqlite` with the actual path to your database file
- Replace `your-passphrase` with your actual database passphrase
- The `:ro` flag mounts the database as read-only. Remove it if you need write access
- On Windows, use Windows-style paths: `C:/path/to/database.sqlite:/data/database.sqlite:ro`

### Claude Desktop (macOS)

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

### Claude Desktop (Windows)

Edit `%APPDATA%\Claude\claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "C:/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

### Claude Desktop (Linux)

Edit `~/.config/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

## Advanced Configuration

### Using Encrypted Passphrases

When using encrypted passphrases (with `encrypted:` prefix), you **must** pass the encryption key as an environment variable to the Docker container using the `-e` flag.

#### Step 1: Get Your Encryption Key

**On macOS (if stored in Keychain):**
```bash
security find-generic-password -s "mcp-sqlite" -a "encryption-key" -w
```

**Or if you have it stored elsewhere:**
Use your encryption key directly.

#### Step 2: Configure Docker with Encryption Key

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-e",
        "MCP_SQLITE_ENCRYPTION_KEY=your-encryption-key-here",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"encrypted:your-encrypted-passphrase\"}"
      ]
    }
  }
}
```

**Important Notes:**
- The `-e` flag **must** come before the `-v` flag in the Docker command
- The encryption key is passed as an environment variable to the container
- **macOS Keychain is NOT accessible from inside Docker containers**, so you must pass the key explicitly
- Replace `your-encryption-key-here` with your actual encryption key
- Replace `your-encrypted-passphrase` with your encrypted passphrase (starting with `encrypted:`)

#### Example: Complete Configuration with Encrypted Passphrase

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-e",
        "MCP_SQLITE_ENCRYPTION_KEY=your-encryption-key-here",
        "-v",
        "/Users/username/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"encrypted:your-encrypted-passphrase-here\"}"
      ]
    }
  }
}
```

#### ⚠️ Security Warning: Storing Encryption Keys in Configuration Files

**IMPORTANT:** If both the encryption key and encrypted passphrase are stored as plain text in your configuration file (e.g., `mcp.json`), anyone with access to that file can decrypt your passphrase and gain access to your database.

**Risks:**
- Configuration files are typically not encrypted
- Files may be included in backups, screenshots, or logs
- Other processes may read the configuration file
- Docker environment variables are visible in `docker ps` output
- If the file is compromised, both values can be extracted

**Recommended Solutions:**

Choose one of the following secure approaches:

### Option 1: Environment Variable from External Source (Recommended)

Load the encryption key from an environment variable that is set outside the configuration file.

**Step 1:** Create a secure script to load the key:

**macOS/Linux** (`~/.config/mcp-sqlite/env.sh`):
```bash
#!/bin/bash
# Load encryption key from macOS Keychain or secure storage
export MCP_SQLITE_ENCRYPTION_KEY=$(security find-generic-password -s "mcp-sqlite" -a "encryption-key" -w 2>/dev/null)
```

**Or use a password manager:**
```bash
#!/bin/bash
# Load from password manager (example with 1Password CLI)
export MCP_SQLITE_ENCRYPTION_KEY=$(op read "op://Private/MCP-SQLite/encryption-key")
```

**Step 2:** Source the script before starting Cursor/Claude Desktop:

```bash
source ~/.config/mcp-sqlite/env.sh
# Then start Cursor/Claude Desktop
```

**Step 3:** Use environment variable reference in configuration:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-e",
        "MCP_SQLITE_ENCRYPTION_KEY=${MCP_SQLITE_ENCRYPTION_KEY}",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"encrypted:your-encrypted-passphrase\"}"
      ]
    }
  }
}
```

**Note:** Some MCP clients may not expand shell variables. In that case, use Option 2 (Wrapper Script).

### Option 2: Wrapper Script (Most Secure)

Create a wrapper script that securely loads the encryption key and starts Docker.

**Step 1:** Create wrapper script (`~/bin/mcp-sqlite-docker.sh`):

```bash
#!/bin/bash
# Wrapper script to securely start MCP SQLite Server with Docker

# Load encryption key from macOS Keychain
ENCRYPTION_KEY=$(security find-generic-password -s "mcp-sqlite" -a "encryption-key" -w 2>/dev/null)

if [ -z "$ENCRYPTION_KEY" ]; then
    echo "Error: Encryption key not found in Keychain" >&2
    exit 1
fi

# Database path (adjust as needed)
DB_PATH="/path/to/your/database.sqlite"
ENCRYPTED_PASSPHRASE="encrypted:your-encrypted-passphrase"

# Start Docker container with encryption key
exec docker run --rm -i \
    -e "MCP_SQLITE_ENCRYPTION_KEY=$ENCRYPTION_KEY" \
    -v "$DB_PATH:/data/database.sqlite:ro" \
    ghcr.io/rosch100/mcp-sqlite:0.2.2 \
    --args "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"$ENCRYPTED_PASSPHRASE\"}"
```

**Step 2:** Make it executable:

```bash
chmod +x ~/bin/mcp-sqlite-docker.sh
```

**Step 3:** Use the wrapper script in your configuration:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/Users/username/bin/mcp-sqlite-docker.sh",
      "args": []
    }
  }
}
```

**Benefits:**
- Encryption key never appears in configuration file
- Key is loaded securely from Keychain at runtime
- Script can be protected with file permissions (`chmod 600`)
- Works with all MCP clients

### Option 3: Plain Passphrase (Less Secure, but Acceptable for Isolated Containers)

If your Docker container is properly isolated and you trust your system security, you can use a plain passphrase directly. This is less secure than encrypted passphrases but avoids the key storage problem.

**Configuration:**

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-plain-passphrase\"}"
      ]
    }
  }
}
```

**Security Considerations:**
- ⚠️ Passphrase is visible in configuration file
- ⚠️ Passphrase is visible in `docker ps` output
- ✅ No encryption key to manage
- ✅ Suitable for isolated, trusted environments
- ✅ Simpler configuration

**When to use:**
- Development environments
- Isolated systems with strong access controls
- When the database passphrase is not highly sensitive

### Option 4: Separate Secrets File (Alternative)

Store sensitive values in a separate file with restricted permissions.

**Step 1:** Create secrets file (`~/.config/mcp-sqlite/secrets.env`):

```bash
# File permissions: chmod 600 ~/.config/mcp-sqlite/secrets.env
MCP_SQLITE_ENCRYPTION_KEY=your-encryption-key-here
```

**Step 2:** Use a wrapper script that sources the secrets file:

```bash
#!/bin/bash
source ~/.config/mcp-sqlite/secrets.env
exec docker run --rm -i \
    -e "MCP_SQLITE_ENCRYPTION_KEY=$MCP_SQLITE_ENCRYPTION_KEY" \
    -v "/path/to/database.sqlite:/data/database.sqlite:ro" \
    ghcr.io/rosch100/mcp-sqlite:0.2.2 \
    --args '{"db_path":"/data/database.sqlite","passphrase":"encrypted:..."}'
```

**Step 3:** Protect the file:

```bash
chmod 600 ~/.config/mcp-sqlite/secrets.env
```

### Security Best Practices Summary

1. **Never store encryption keys in version control**
2. **Use macOS Keychain** (Option 1 or 2) for the most secure key storage
3. **Use wrapper scripts** (Option 2) when MCP clients don't support environment variable expansion
4. **Restrict file permissions** on any files containing secrets (`chmod 600`)
5. **Use encrypted passphrases** with secure key storage (Options 1-2) for production
6. **Consider plain passphrases** (Option 3) only for isolated, trusted environments
7. **Rotate encryption keys** periodically and re-encrypt all passphrases
8. **Monitor access** to systems storing encryption keys

### Custom Cipher Profile

To use a custom cipher profile:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\",\"cipherProfile\":{\"name\":\"Custom\",\"pageSize\":4096,\"kdfIterations\":256000,\"hmacAlgorithm\":\"HMAC_SHA512\",\"kdfAlgorithm\":\"PBKDF2_HMAC_SHA512\"}}"
      ]
    }
  }
}
```

### Write Access

To enable write access to the database, remove the `:ro` flag:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

### Debug Mode

To enable debug output:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/path/to/your/database.sqlite:/data/database.sqlite:ro",
        "-e",
        "MCP_DEBUG=true",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

## Testing the Configuration

### Manual Test

Test the Docker container manually:

```bash
docker run --rm -i \
  -v /path/to/your/database.sqlite:/data/database.sqlite:ro \
  ghcr.io/rosch100/mcp-sqlite:0.2.2 \
  --args '{"db_path":"/data/database.sqlite","passphrase":"your-passphrase"}'
```

### Test MCP Protocol

Send an initialize request:

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | \
  docker run --rm -i \
  -v /path/to/your/database.sqlite:/data/database.sqlite:ro \
  ghcr.io/rosch100/mcp-sqlite:0.2.2 \
  --args '{"db_path":"/data/database.sqlite","passphrase":"your-passphrase"}'
```

## Troubleshooting

### Docker not found

- Ensure Docker Desktop is running
- Verify Docker is in your PATH: `docker --version`
- Restart your MCP client after starting Docker

### "mounts denied" or "path not shared" error

**On macOS:**
1. Open Docker Desktop
2. Go to Settings → Resources → File Sharing
3. Add the parent directory (e.g., `/Users` or `/Users/username`)
4. Click "Apply & Restart"
5. Ensure your path starts with `/` (absolute path)

**Common mistake:** Using relative paths like `Users/...` instead of `/Users/...`

### "invalid volume specification" error

**Problem:** Path contains spaces or special characters

**Solution:** Use absolute paths with proper escaping. For paths with spaces:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "docker",
      "args": [
        "run",
        "--rm",
        "-i",
        "-v",
        "/Users/username/Library/Containers/com.moneymoney-app.retail/Data/Library/Application Support/MoneyMoney/Database/MoneyMoney.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:0.2.2",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

**Important:** Always use absolute paths starting with `/` on macOS/Linux.

### Permission denied

- Check file permissions on your database file
- Ensure Docker has access to the mounted directory
- On macOS, check Docker Desktop → Settings → Resources → File Sharing
- Make sure the parent directory is shared (e.g., `/Users`)

### Container exits immediately

- Check Docker logs: `docker logs <container-id>`
- Verify the database path is correct (must be absolute)
- Ensure the passphrase is correct
- Verify the path is shared in Docker Desktop

### Path issues on Windows

- Use forward slashes: `C:/path/to/file.sqlite`
- Or escape backslashes: `C:\\path\\to\\file.sqlite`
- Use Windows-style paths in the volume mount

### Path issues on macOS

**Common errors:**
- ❌ `Users/...` (missing leading `/`)
- ✅ `/Users/...` (correct absolute path)

**For paths with spaces:**
- The path in the `-v` argument should be properly quoted or escaped
- Docker handles spaces in paths automatically if the path is absolute
- Example: `/Users/username/Library/Application Support/...` works fine

### Image not found

- Pull the image: `docker pull ghcr.io/rosch100/mcp-sqlite:0.2.2`
- Check if the image exists: `docker images | grep mcp-sqlite`
- Verify the tag is correct

## Security Considerations

1. **Passphrases**: Never commit passphrases to version control
2. **File Permissions**: Use read-only mounts (`:ro`) when possible
3. **Encrypted Passphrases**: Use encrypted passphrases for better security
4. **Environment Variables**: Be careful with environment variables containing secrets

## Restart MCP Client

After making configuration changes:

1. **Cursor**: Restart the application
2. **Claude Desktop**: Restart the application
3. Check MCP logs for connection status

## Verification

Once configured, you should see:

1. The server appears in your MCP client's server list
2. Tools are available (list_tables, get_table_data, etc.)
3. You can query your encrypted database

For more information, see the main [README.md](README.md).

