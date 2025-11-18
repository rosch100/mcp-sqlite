# Submission Templates für MCP Server Verzeichnisse

Vorbereitete Informationen für die Veröffentlichung in verschiedenen MCP Server Verzeichnissen.

## 1. MCP Registry (Offiziell) - https://registry.modelcontextprotocol.io

### Status
⚠️ **Benötigt Authentifizierung**: Token ist abgelaufen

### Schritte:
```bash
# 1. Neu authentifizieren
mcp-publisher login github

# 2. Veröffentlichen
mcp-publisher publish
```

### Datei
- `server.json` ist bereits vorbereitet und validiert

---

## 2. MCPList.ai - https://www.mcplist.ai/

### Submission-Informationen:

**Server Name:**
```
Encrypted SQLite MCP Server
```

**GitHub Repository:**
```
https://github.com/rosch100/mcp-sqlite
```

**Beschreibung:**
```
MCP server for SQLCipher 4 encrypted SQLite databases. Provides full CRUD operations, query support, and database exploration tools via Model Context Protocol. Supports encrypted passphrases with macOS Keychain integration.
```

**Kategorien:**
- Database
- Encryption
- SQLite
- CRUD Operations

**Features:**
- SQLCipher 4 support
- Database exploration
- Query support
- CRUD operations
- Configurable cipher profiles
- Encrypted passphrase support

**Installation:**
```bash
docker pull ghcr.io/rosch100/mcp-sqlite:latest
```

**Konfiguration:**
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
        "ghcr.io/rosch100/mcp-sqlite:latest",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

**Dokumentation:**
- README: https://github.com/rosch100/mcp-sqlite/blob/main/README.md
- Docker Guide: https://github.com/rosch100/mcp-sqlite/blob/main/DOCKER_QUICKSTART.md

---

## 3. MCP Index - https://mcpindex.net/

### Submission-Informationen:

**Name:**
```
encrypted-sqlite-mcp
```

**Display Name:**
```
Encrypted SQLite MCP Server
```

**Description:**
```
MCP server for working with SQLCipher 4 encrypted SQLite databases. Provides tools to read database structures, query tables, and perform CRUD operations.
```

**Repository:**
```
https://github.com/rosch100/mcp-sqlite
```

**Version:**
```
0.2.4
```

**Language:**
```
Java
```

**Transport:**
```
stdio
```

**Tools:**
- `list_tables` - List all tables in the database with their columns and metadata
- `get_table_data` - Read data from a table with optional filtering, column selection, and pagination
- `exec_query` - Execute arbitrary SQL statements (SELECT, INSERT, UPDATE, DELETE, DDL)
- `insert_or_update` - Perform UPSERT operations (INSERT or UPDATE on conflict)
- `delete_rows` - Delete rows from a table based on filters
- `get_table_schema` - Retrieve detailed schema information (columns, indexes, foreign keys)
- `list_indexes` - List all indexes for a given table

**Installation:**
```bash
docker pull ghcr.io/rosch100/mcp-sqlite:latest
```

**Configuration Example:**
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
        "/path/to/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:latest",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

**Keywords:**
```
mcp, mcp-server, sqlite, sqlcipher, encryption, database, java, crud, database-tools
```

**License:**
```
Apache-2.0
```

---

## 4. MCPServ.club - https://www.mcpserv.club/

### Submission-Informationen:

**Server Name:**
```
Encrypted SQLite MCP Server
```

**GitHub URL:**
```
https://github.com/rosch100/mcp-sqlite
```

**Short Description:**
```
MCP server for SQLCipher 4 encrypted SQLite databases with full CRUD operations
```

**Long Description:**
```
A Model Context Protocol server for working with SQLCipher 4 encrypted SQLite databases. Provides comprehensive tools for database exploration, querying, and CRUD operations. Features include encrypted passphrase support with macOS Keychain integration, configurable cipher profiles, and full SQL query support.
```

**Category:**
```
Database
```

**Tags:**
```
sqlite, sqlcipher, encryption, database, crud, java
```

**Installation Method:**
```
Docker
```

**Docker Image:**
```
ghcr.io/rosch100/mcp-sqlite:latest
```

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
        "/path/to/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:latest",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

**Documentation Links:**
- Main README: https://github.com/rosch100/mcp-sqlite
- Docker Quickstart: https://github.com/rosch100/mcp-sqlite/blob/main/DOCKER_QUICKSTART.md
- Docker Configuration: https://github.com/rosch100/mcp-sqlite/blob/main/DOCKER_CONFIGURATION.md

---

## 5. Directory MCP - https://directorymcp.com/

### Submission-Informationen:

**Component Name:**
```
Encrypted SQLite MCP Server
```

**Type:**
```
MCP Server
```

**Repository:**
```
https://github.com/rosch100/mcp-sqlite
```

**Description:**
```
MCP server implementation for SQLCipher 4 encrypted SQLite databases. Provides full CRUD operations, database exploration, and query support via Model Context Protocol.
```

**Features:**
- SQLCipher 4 encryption support
- Database structure exploration
- SQL query execution
- CRUD operations (Create, Read, Update, Delete)
- UPSERT operations
- Encrypted passphrase support
- macOS Keychain integration
- Configurable cipher profiles

**Installation:**
```bash
docker pull ghcr.io/rosch100/mcp-sqlite:latest
```

**Usage:**
See README.md and DOCKER_QUICKSTART.md for detailed configuration instructions.

**License:**
```
Apache License 2.0
```

---

## 6. MCPHub - https://mcphub.com/

### Submission-Informationen:

**Server Name:**
```
encrypted-sqlite-mcp
```

**Display Name:**
```
Encrypted SQLite MCP Server
```

**Description:**
```
MCP server for SQLCipher 4 encrypted SQLite databases with full CRUD operations and query support
```

**GitHub:**
```
rosch100/mcp-sqlite
```

**Version:**
```
0.2.4
```

**Language:**
```
Java
```

**Installation:**
```bash
docker pull ghcr.io/rosch100/mcp-sqlite:latest
```

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
        "/path/to/database.sqlite:/data/database.sqlite:ro",
        "ghcr.io/rosch100/mcp-sqlite:latest",
        "--args",
        "{\"db_path\":\"/data/database.sqlite\",\"passphrase\":\"your-passphrase\"}"
      ]
    }
  }
}
```

---

## Checkliste für Submission

- [ ] MCP Registry: Neu authentifizieren und publishen
- [ ] MCPList.ai: Submission-Formular ausfüllen
- [ ] MCP Index: Server hinzufügen
- [ ] MCPServ.club: Submission über Guidelines
- [ ] Directory MCP: Component einreichen
- [ ] MCPHub: Server hinzufügen

## Nächste Schritte

1. **MCP Registry** (höchste Priorität):
   ```bash
   mcp-publisher login github
   mcp-publisher publish
   ```

2. **Community-Verzeichnisse**: Manuelle Submission über die jeweiligen Websites
   - Verwende die oben vorbereiteten Templates
   - Kopiere die Informationen in die Submission-Formulare

3. **Verifizierung**: Nach Submission prüfen, ob der Server in den Verzeichnissen erscheint

