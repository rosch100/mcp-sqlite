# Passphrase Encryption - Quick Guide

This guide shows you how to encrypt your passphrase for the MCP SQLite Server.

## macOS Keychain (Recommended for macOS)

On macOS, you can securely store the encryption key in the Keychain:

### Step 1: Build the Project
```bash
./gradlew build
```

### Step 2: Generate and Store Key in Keychain
```bash
./store-key-in-keychain.sh --generate
```

That's it! The key will be automatically loaded from the Keychain when no environment variable is set.

### Step 3: Encrypt Passphrase
```bash
./encrypt-passphrase.sh "your-plain-passphrase"
```

**Benefits of Keychain:**
- ✅ Key is securely encrypted and stored by macOS
- ✅ No environment variables needed
- ✅ Automatic unlock with macOS user password
- ✅ Works system-wide for all applications

---

## Environment Variable (Alternative/Cross-Platform)

### Step 1: Build the Project

Make sure the project is built:

```bash
./gradlew build
```

### Step 2: Generate Encryption Key

Generate a new encryption key:

```bash
./generate-key.sh
```

The script will output a key. Copy it.

### Step 3: Set Key as Environment Variable

Set the key as an environment variable:

```bash
export MCP_SQLITE_ENCRYPTION_KEY="<your-generated-key>"
```

**Important:** For permanent use, add this line to your shell configuration:
- Bash: `~/.bashrc` or `~/.bash_profile`
- Zsh: `~/.zshrc`
- Fish: `~/.config/fish/config.fish`

```bash
echo 'export MCP_SQLITE_ENCRYPTION_KEY="<your-key>"' >> ~/.zshrc
```

### Step 4: Encrypt Passphrase

Encrypt your passphrase:

```bash
./encrypt-passphrase.sh "your-plain-passphrase"
```

The script will output the encrypted passphrase (starts with `encrypted:`).

### Step 5: Use in Configuration

Use the encrypted passphrase in your MCP configuration (e.g., `~/.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"db_path\":\"/path/to/your/database.sqlite\",\"passphrase\":\"encrypted:<encrypted-passphrase>\"}"
      ],
      "env": {
        "MCP_SQLITE_ENCRYPTION_KEY": "<your-encryption-key>"
      }
    }
  }
}
```

## Alternative: Manual Usage

If you don't want to use the scripts:

### Generate Key:
```bash
java -cp build/libs/mcp-sqlite-0.2.1.jar com.example.mcp.sqlite.util.GenerateKey
```

### Encrypt Passphrase:
```bash
export MCP_SQLITE_ENCRYPTION_KEY="<your-key>"
java -cp build/libs/mcp-sqlite-0.2.1.jar com.example.mcp.sqlite.util.EncryptPassphrase "your-passphrase"
```

## Security Considerations

- ⚠️ **IMPORTANT:** The encryption key (`MCP_SQLITE_ENCRYPTION_KEY`) **MUST** be set, otherwise decryption will fail
- 🔒 Store the key securely and never commit it to version control
- 🔑 Use different keys for different environments (development, production)
- 🔄 Rotate the key regularly and re-encrypt all passphrases

## Example Workflow

```bash
# 1. Build project
./gradlew build

# 2. Generate and set key
KEY=$(./generate-key.sh | grep -A 1 "Encryption key:" | tail -1)
export MCP_SQLITE_ENCRYPTION_KEY="$KEY"

# 3. Encrypt passphrase
./encrypt-passphrase.sh "my-secret-passphrase"

# 4. Copy output and use in mcp.json
```

## Troubleshooting

### Error: "MCP_SQLITE_ENCRYPTION_KEY is not set"
- Make sure the environment variable is set: `echo $MCP_SQLITE_ENCRYPTION_KEY`
- Set it with: `export MCP_SQLITE_ENCRYPTION_KEY="<key>"`

### Error: "Key is too weak"
- Always use `generate-key.sh` or `GenerateKey` tool to generate keys
- Never use predictable keys

### Error: "Invalid Base64 format"
- Make sure the key was copied correctly (no spaces, complete)
