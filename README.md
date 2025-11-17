# Encrypted SQLite MCP Server

A Model Context Protocol (MCP) server for working with encrypted SQLite databases using SQLCipher. This server provides tools to read database structures, query tables, and perform CRUD operations on encrypted SQLite databases.

## Features

- 🔐 **SQLCipher Support**: Works with SQLCipher 4 encrypted databases
- 📊 **Database Exploration**: List tables and columns with metadata
- 🔍 **Query Support**: Execute arbitrary SQL queries (SELECT, INSERT, UPDATE, DELETE, DDL)
- 📝 **CRUD Operations**: Insert, update, and delete rows with filtering
- ⚙️ **Configurable Cipher Profiles**: Support for different SQLCipher configurations
- 🚀 **MCP Protocol**: Full Model Context Protocol implementation via STDIO

## Requirements

- **Java 17** or higher (JDK)
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

The server communicates via STDIO (standard input/output) and can be configured in any MCP client. Below are examples for popular clients:

#### Cursor

Add the following to your Cursor `mcp.json` file (typically located at `~/.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"dbPath\":\"/path/to/your/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

#### Claude Desktop

Add the following to your Claude Desktop configuration file:
- **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
- **Windows**: `%APPDATA%\Claude\claude_desktop_config.json`

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"dbPath\":\"/path/to/your/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

#### Other MCP Clients

For other MCP clients, refer to their documentation for the configuration format. The server uses the standard MCP protocol via STDIO transport.

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
        "{\"dbPath\":\"/path/to/your/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ],
      "env": {
        "JAVA_HOME": "/path/to/java/home"
      }
    }
  }
}
```

### Custom Cipher Profile

You can override the default SQLCipher 4 settings by including a `cipherProfile` in the configuration:

```json
{
  "dbPath": "/path/to/database.sqlite",
  "passphrase": "your-passphrase",
  "cipherProfile": {
    "name": "SQLCipher 4 defaults",
    "pageSize": 4096,
    "kdfIterations": 256000,
    "hmacAlgorithm": "HMAC_SHA512",
    "kdfAlgorithm": "PBKDF2_HMAC_SHA512"
  }
}
```

## Available Tools

### `listTables`

List all tables in the database with their columns and metadata.

**Parameters:**
- `dbPath` (optional if configured): Path to the database file
- `passphrase` (optional if configured): Database passphrase

**Example:**
```json
{
  "name": "listTables",
  "arguments": {}
}
```

### `getTableData`

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
  "name": "getTableData",
  "arguments": {
    "table": "accounts",
    "columns": ["id", "name", "balance"],
    "filters": {"status": "active"},
    "limit": 50,
    "offset": 0
  }
}
```

### `execQuery`

Execute arbitrary SQL statements (SELECT, INSERT, UPDATE, DELETE, DDL).

**Parameters:**
- `sql` (required): SQL statement to execute

**Example:**
```json
{
  "name": "execQuery",
  "arguments": {
    "sql": "SELECT COUNT(*) FROM transactions WHERE amount > 1000"
  }
}
```

### `insertOrUpdate`

Perform UPSERT operations (INSERT or UPDATE on conflict).

**Parameters:**
- `table` (required): Table name
- `primaryKeys` (required): Array of primary key column names
- `rows` (required): Array of row objects to insert/update

**Example:**
```json
{
  "name": "insertOrUpdate",
  "arguments": {
    "table": "accounts",
    "primaryKeys": ["id"],
    "rows": [
      {"id": 1, "name": "Account 1", "balance": 1000.0},
      {"id": 2, "name": "Account 2", "balance": 2000.0}
    ]
  }
}
```

### `deleteRows`

Delete rows from a table based on filters.

**Parameters:**
- `table` (required): Table name
- `filters` (required): Object with column-value pairs for filtering

**Example:**
```json
{
  "name": "deleteRows",
  "arguments": {
    "table": "transactions",
    "filters": {"status": "cancelled"}
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
./gradlew run --args='{"dbPath":"/path/to/db.sqlite","passphrase":"secret"}'
```

Or use the installed binary:

```bash
./build/install/mcp-sqlite/bin/mcp-sqlite --args '{"dbPath":"/path/to/db.sqlite","passphrase":"secret"}'
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

- **Passphrases**: Passphrases are only stored in memory and never logged
- **Transport**: Use secure transport channels (e.g., encrypted sessions) when accessing the server remotely
- **File Permissions**: Ensure database files have appropriate file system permissions
- **Environment Variables**: Be cautious when passing sensitive data via environment variables

## Troubleshooting

### Database cannot be opened

- Verify the passphrase is correct
- Check that the database uses SQLCipher 4 defaults (or configure a custom cipher profile)
- Ensure the database file path is correct and accessible

### Connection issues

- Verify Java is installed and accessible: `java -version`
- Check that `JAVA_HOME` is set correctly in the MCP configuration
- Review your MCP client logs for detailed error messages

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
