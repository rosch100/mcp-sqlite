# Encrypted SQLite MCP Server

A Model Context Protocol (MCP) server for working with encrypted SQLite databases using SQLCipher. This server provides tools to read database structures, query tables, and perform CRUD operations on encrypted SQLite databases.

## Features

- 🔐 **SQLCipher Support**: Works with SQLCipher 4 encrypted databases
- 📊 **Database Exploration**: List tables, columns, indexes, and schema metadata
- 🔍 **Query Support**: Execute arbitrary SQL queries (SELECT, INSERT, UPDATE, DELETE, DDL)
- 📝 **CRUD Operations**: Insert, update, and delete rows with filtering
- ⚙️ **Configurable Cipher Profiles**: Support for different SQLCipher configurations
- 🚀 **MCP Protocol**: Full Model Context Protocol implementation via STDIO
- 🔒 **Security**: SQL identifier validation to prevent SQL injection
- 🐛 **Debug Mode**: Optional debug output via `MCP_DEBUG` environment variable
- 🌐 **Internationalization**: All messages and documentation in English
- 📏 **Input Validation**: Comprehensive validation for limits, offsets, and identifiers

## Requirements

- **Java 21** or higher (JDK)
- **Gradle** (wrapper included)
- SQLite JDBC driver with encryption support (`sqlite-jdbc-3.50.1.0.jar` from [sqlite-jdbc-crypt](https://github.com/Willena/sqlite-jdbc-crypt))

## Installation

1. Clone the repository:
```bash
git clone https://github.com/rosch100/mcp-sqlite.git
cd mcp-sqlite
```

2. Build the project (the SQLite JDBC driver will be downloaded automatically):
```bash
./gradlew build
```

The build process will automatically download `sqlite-jdbc-3.50.1.0.jar` from [sqlite-jdbc-crypt releases](https://github.com/Willena/sqlite-jdbc-crypt/releases) and place it in the `libs/` directory.

4. Install the distribution:
```bash
./gradlew installDist
```

The executable will be available at `build/install/mcp-sqlite/bin/mcp-sqlite`.

## Configuration

This MCP server can be used with any MCP-compatible client. The configuration format follows the [Model Context Protocol specification](https://modelcontextprotocol.io/).

### MCP Client Configuration

The server communicates via STDIO (standard input/output) and can be configured in any MCP client. Add the following configuration to your MCP client's configuration file:

**Configuration file locations:**
- **Cursor**: `~/.cursor/mcp.json`
- **Claude Desktop** (macOS): `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Claude Desktop** (Windows): `%APPDATA%\Claude\claude_desktop_config.json`
- **Other clients**: Refer to your client's documentation

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"db_path\":\"/path/to/your/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

**Optional Parameters:**

Most parameters can be omitted if your system is properly configured:

- **`transport`**: Defaults to `"stdio"` (can be omitted for most clients)
- **`cwd`**: Not needed when using absolute paths (can be omitted)
- **`env`**: Only needed if Java is not in your system PATH

If you need to specify a custom Java installation, you can add:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"db_path\":\"/path/to/your/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ],
      "env": {
        "JAVA_HOME": "/path/to/java/home"
      }
    }
  }
}
```

### Custom Cipher Profile

You can override the default SQLCipher 4 settings by including a `cipherProfile` in the MCP server configuration. Add the `cipherProfile` object to the JSON string passed in the `args` parameter:

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"db_path\":\"/path/to/your/database.sqlite\",\"passphrase\":\"your-passphrase\",\"cipherProfile\":{\"name\":\"SQLCipher 4 defaults\",\"pageSize\":4096,\"kdfIterations\":256000,\"hmacAlgorithm\":\"HMAC_SHA512\",\"kdfAlgorithm\":\"PBKDF2_HMAC_SHA512\"}}"
      ]
    }
  }
}
```

**Note:** The `cipherProfile` must be included in the JSON string within the `args` array, not as a separate configuration parameter. All fields in `cipherProfile` are optional - only specify the ones you want to override from the defaults.

**Alternative:** You can also specify `cipherProfile` in individual tool calls (e.g., `listTables`, `getTableData`) to override the default configuration for that specific operation. However, it's recommended to configure it once in the MCP server configuration for consistency.

### Encrypted Passphrases

For enhanced security, you can store passphrases in encrypted form. The server uses **AES-256-GCM** encryption, which provides authenticated encryption and is both secure and fast.

#### macOS Keychain (Recommended for macOS)

On macOS, you can securely store the encryption key in the Keychain:

1. **Generate and store key in Keychain:**
   ```bash
   ./store-key-in-keychain.sh --generate
   ```

2. **Encrypt your passphrase:**
   ```bash
   ./encrypt-passphrase.sh "your-plain-passphrase"
   ```

The key will be automatically loaded from the Keychain when no environment variable is set.

**Benefits:**
- ✅ Key is securely encrypted and stored by macOS
- ✅ No environment variables needed
- ✅ Automatic unlock with macOS user password
- ✅ Works system-wide for all applications

#### Environment Variable (Alternative/Cross-Platform)

1. **Generate an encryption key:**
   ```bash
   java -cp build/libs/mcp-sqlite-0.1.0.jar com.example.mcp.sqlite.config.PassphraseEncryption
   ```
   Or use this simple Java snippet:
   ```java
   import com.example.mcp.sqlite.config.PassphraseEncryption;
   String key = PassphraseEncryption.generateKey();
   System.out.println(key);
   ```

2. **Set the encryption key as an environment variable:**
   ```bash
   export MCP_SQLITE_ENCRYPTION_KEY="<your-generated-key>"
   ```

3. **Encrypt your passphrase:**
   
   Using the CLI tool (after building):
   ```bash
   java -cp build/libs/mcp-sqlite-0.1.0.jar com.example.mcp.sqlite.util.EncryptPassphrase "your-plain-passphrase"
   ```
   
   Or programmatically:
   ```java
   import com.example.mcp.sqlite.config.PassphraseEncryption;
   PassphraseEncryption encryption = PassphraseEncryption.fromBase64Key(System.getenv("MCP_SQLITE_ENCRYPTION_KEY"));
   String encrypted = encryption.encrypt("your-plain-passphrase");
   System.out.println(encrypted);
   ```

#### Usage

Use the encrypted passphrase (with `encrypted:` prefix) in your configuration:

**On macOS with Keychain (recommended):**
```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"db_path\":\"/path/to/your/database.sqlite\",\"passphrase\":\"encrypted:<encrypted-passphrase>\"}"
      ]
    }
  }
}
```
*Note: No `env` section needed - the key is automatically loaded from macOS Keychain.*

**With environment variable (cross-platform):**
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

**Important Security Notes:**
- **REQUIRED**: The encryption key **MUST** be available either:
  - In macOS Keychain (automatically used on macOS)
  - Or as environment variable `MCP_SQLITE_ENCRYPTION_KEY`
  The server will fail if an encrypted passphrase is used without the key.
- The encryption key should be kept secure and never committed to version control
- Use a strong, randomly generated key (256 bits / 32 bytes) - use `PassphraseEncryption.generateKey()` to generate one
- The server automatically detects encrypted passphrases (those starting with `encrypted:`) and decrypts them before use
- Plain passphrases (without `encrypted:` prefix) work as before and are not modified
- Each encrypted passphrase uses a unique IV (Initialization Vector) for maximum security
- **AES-256-GCM** provides authenticated encryption, protecting against both tampering and disclosure
- Weak keys (e.g., all zeros, low entropy) are automatically rejected
- The encryption key is validated on startup to ensure it meets security requirements

## Available Tools

### `list_tables`

List all tables in the database. By default only table names, with `include_columns=true` also column details.

**Parameters:**
- `db_path` (optional if configured): Path to the database file
- `passphrase` (optional if configured): Database passphrase
- `include_columns` (optional, default: false): If true, column details are also returned

**Example:**
```json
{
  "name": "list_tables",
  "arguments": {
    "include_columns": true
  }
}
```

### `get_table_data`

Read data from a table with optional filtering, column selection, and pagination.

**Parameters:**
- `table` (required): Table name
- `columns` (optional): Array of column names to select
- `filters` (optional): Object with column-value pairs for filtering
- `limit` (optional, default: 200): Maximum number of rows
- `offset` (optional, default: 0): Offset for pagination

**Example:**
```json
{
  "name": "get_table_data",
  "arguments": {
    "table": "accounts",
    "columns": ["id", "name", "balance"],
    "filters": {"status": "active"},
    "limit": 50,
    "offset": 0
  }
}
```

### `execute_sql`

Execute arbitrary SQL statements (SELECT, INSERT, UPDATE, DELETE, DDL).

**⚠️ Security Warning**: This tool executes raw SQL without parameterization. Only use with trusted SQL or ensure proper validation and sanitization is performed before calling this tool. For safer operations, use the other tools (`get_table_data`, `insert_or_update`, `delete_rows`) which use parameterized queries.

**Parameters:**
- `sql` (required): SQL statement to execute

**Example:**
```json
{
  "name": "execute_sql",
  "arguments": {
    "sql": "SELECT COUNT(*) FROM transactions WHERE amount > 1000"
  }
}
```

### `insert_or_update`

Perform UPSERT operations (INSERT or UPDATE on conflict).

**Parameters:**
- `table` (required): Table name
- `primary_keys` (required): Array of primary key column names
- `rows` (required): Array of row objects to insert/update

**Example:**
```json
{
  "name": "insert_or_update",
  "arguments": {
    "table": "accounts",
    "primary_keys": ["id"],
    "rows": [
      {"id": 1, "name": "Account 1", "balance": 1000.0},
      {"id": 2, "name": "Account 2", "balance": 2000.0}
    ]
  }
}
```

### `delete_rows`

Delete rows from a table based on filters.

**Parameters:**
- `table` (required): Table name
- `filters` (required): Object with column-value pairs for filtering

**Example:**
```json
{
  "name": "delete_rows",
  "arguments": {
    "table": "transactions",
    "filters": {"status": "cancelled"}
  }
}
```

### `get_table_schema`

Retrieves detailed schema information for a table (columns, indexes, foreign keys, constraints).

**Parameters:**
- `table` (required): Table name

**Example:**
```json
{
  "name": "get_table_schema",
  "arguments": {
    "table": "accounts"
  }
}
```

### `list_indexes`

Lists all indexes of a table.

**Parameters:**
- `table` (required): Table name

**Example:**
```json
{
  "name": "list_indexes",
  "arguments": {
    "table": "accounts"
  }
}
```

## Default Cipher Profile

The server uses **SQLCipher 4 defaults** by default:

- `cipher_page_size`: 4096
- `kdf_iter`: 256000
- `cipher_hmac_algorithm`: HMAC_SHA512
- `cipher_kdf_algorithm`: PBKDF2_HMAC_SHA512
- `cipher_use_hmac`: ON
- `cipher_plaintext_header_size`: 0

These settings match the defaults used by tools like "DB Browser for SQLite" with SQLCipher 4.

## Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Running the Server

```bash
./gradlew run --args='{"db_path":"/path/to/db.sqlite","passphrase":"secret"}'
```

Or use the installed binary:

```bash
./build/install/mcp-sqlite/bin/mcp-sqlite --args '{"db_path":"/path/to/db.sqlite","passphrase":"secret"}'
```

### Project Structure

```
mcp-sqlite/
├── src/
│   ├── main/java/com/example/mcp/sqlite/
│   │   ├── McpServer.java          # Main MCP server implementation
│   │   ├── EncryptedSqliteClient.java  # SQLite client with encryption
│   │   ├── config/
│   │   │   ├── DatabaseConfig.java     # Database configuration
│   │   │   └── CipherProfile.java      # Cipher profile configuration
│   │   └── util/
│   │       └── SqliteUtil.java         # SQLite utilities
│   └── test/                           # Unit tests
├── libs/
│   └── sqlite-jdbc-3.50.1.0.jar        # SQLite JDBC driver with encryption
├── build.gradle                         # Gradle build configuration
└── README.md                            # This file
```

## Security Considerations

### Passphrase Encryption

- **Encryption Algorithm**: AES-256-GCM (Galois/Counter Mode) - provides authenticated encryption
- **Key Management**: The encryption key (`MCP_SQLITE_ENCRYPTION_KEY`) **MUST** be set as an environment variable
- **Key Generation**: Always use `PassphraseEncryption.generateKey()` to generate keys - never use weak or predictable keys
- **Key Storage**: Store the encryption key securely (e.g., in a secrets manager) and never commit it to version control
- **Key Validation**: Weak keys (all zeros, low entropy) are automatically rejected

### General Security

- **Passphrases**: Passphrases are only stored in memory and never logged
- **Encrypted Passphrases**: Use encrypted passphrases with AES-256-GCM for storing passphrases in configuration files
- **Memory**: Note that decrypted passphrases remain in memory as Java Strings (immutable). For maximum security, consider using `char[]` arrays, though this is not currently implemented.
- **Transport**: Use secure transport channels (e.g., encrypted sessions) when accessing the server remotely
- **File Permissions**: Ensure database files have appropriate file system permissions
- **Environment Variables**: Be cautious when passing sensitive data via environment variables - use encrypted passphrases instead

### Security Best Practices

1. **Always set `MCP_SQLITE_ENCRYPTION_KEY`** when using encrypted passphrases
2. **Generate strong keys** using `PassphraseEncryption.generateKey()`
3. **Rotate keys periodically** - when rotating, re-encrypt all passphrases with the new key
4. **Use different keys** for different environments (development, staging, production)
5. **Never commit keys or encrypted passphrases** to version control
6. **Monitor access** to systems storing encryption keys

## Troubleshooting

### Debugging MCP Server Communication Issues

The MCP server includes extensive debugging features to help diagnose communication problems.

#### Viewing Logs

**In Cursor:**
1. Open the Output panel: `Ctrl+Shift+U` (Windows/Linux) or `Cmd+Shift+U` (macOS)
2. Select "MCP Logs" from the dropdown menu
3. All debug output is written to `stderr` and visible there

**Manual testing:**
```bash
./build/install/mcp-sqlite/bin/mcp-sqlite --args '{"db_path":"/path/to/db.sqlite","passphrase":"secret"}' 2>&1 | tee mcp-debug.log
```

#### Common Communication Problems

**1. Server does not start**
- **Symptom**: No logs visible, server does not respond
- **Debugging**: Check startup logs:
  - Java version is logged
  - Arguments are logged
  - Configuration parsing is logged
- **Solution**: 
  - Verify Java is correctly installed
  - Check the MCP configuration (`mcp.json`)
  - Check paths in `command` and `args` fields

**2. JSON Parsing Errors**
- **Symptom**: "Parse error" in logs
- **Debugging**: The server logs:
  - First 500 characters of received JSON
  - Exact exception with stack trace
- **Solution**:
  - Check JSON structure in MCP configuration
  - Ensure JSON is properly escaped
  - Verify all required fields are present

**3. Missing or Incorrect Responses**
- **Symptom**: Requests are not answered or timeouts occur
- **Debugging**: The server logs:
  - Each received request with ID and method
  - Response size and status
  - Flush status after writing
- **Solution**:
  - Check if `STDOUT` is available (logged at startup)
  - Check response size (very large responses can cause problems)
  - Verify flush was successful

**4. Invalid Requests**
- **Symptom**: "Invalid Request" errors
- **Debugging**: The server logs:
  - Missing fields (e.g., `method`, `id`)
  - JSON-RPC version mismatches
  - Invalid parameters
- **Solution**:
  - Ensure all requests comply with JSON-RPC 2.0 standard
  - Verify `method` and `id` fields are present
  - Check parameter structure

**5. Database Connection Problems**
- **Symptom**: "Database error" errors
- **Debugging**: The server logs:
  - Used DB paths (Default vs. Override)
  - Passphrase status (encrypted/decrypted)
  - CipherProfile configuration
- **Solution**:
  - Check DB path in logs
  - Verify passphrase was correctly decrypted
  - Check CipherProfile settings

#### Debugging Features in Detail

The server automatically logs:

- **Startup Information**:
  - Java version and Java Home
  - Operating system information
  - Number and content of arguments
  - Configuration parsing status

- **Request Processing**:
  - Each received request with number and length
  - JSON-RPC validation
  - Method calls with parameters
  - Response size and status

- **Error Handling**:
  - Detailed exception information with stack traces
  - JSON-RPC error codes according to specification
  - Error responses with additional debug data

- **Database Operations**:
  - Used configurations (Default vs. Override)
  - SQL queries (first 100 characters)
  - Result sizes and affected rows

### Database cannot be opened

- Verify the passphrase is correct
- Check that the database uses SQLCipher 4 defaults (or configure a custom cipher profile)
- Ensure the database file path is correct and accessible
- **Check logs**: The server logs detailed information about passphrase decryption and DB path

### Connection issues

- Verify Java is installed and accessible: `java -version`
- Check that `JAVA_HOME` is set correctly in the MCP configuration
- Review your MCP client logs for detailed error messages
- **Check startup logs**: The server logs Java version and home at startup

### FTS (Full-Text Search) tables

The server automatically handles FTS virtual tables that may not have accessible metadata. These tables will appear with empty column lists.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

### Third-Party Licenses

This project uses the following third-party libraries:

- **sqlite-jdbc-crypt** (Apache License 2.0) - SQLite JDBC driver with encryption support
  - Source: https://github.com/Willena/sqlite-jdbc-crypt
- **Gson** (Apache License 2.0) - JSON library for Java
  - Source: https://github.com/google/gson
- **JUnit Jupiter** (Eclipse Public License 2.0) - Testing framework
  - Source: https://junit.org/junit5/

See [NOTICE](NOTICE) for detailed attribution information.

## Acknowledgments

- [sqlite-jdbc-crypt](https://github.com/Willena/sqlite-jdbc-crypt) - SQLite JDBC driver with encryption support
- [Model Context Protocol](https://modelcontextprotocol.io/) - MCP specification

## License Compliance

All dependencies use licenses compatible with Apache License 2.0. See [LICENSES.md](LICENSES.md) for detailed license information and [NOTICE](NOTICE) for third-party attributions.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Support

For issues, questions, or contributions, please open an issue on [GitHub](https://github.com/rosch100/mcp-sqlite/issues).

## Metadata

This server includes `server.json` with metadata for MCP server directories and registries.
