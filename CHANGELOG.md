# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2025-11-17

### Added
- **New Tools:**
  - `get_table_schema`: Retrieve detailed schema information (columns, indexes, foreign keys, CREATE SQL)
  - `list_indexes`: List all indexes for a given table
- **Security Enhancements:**
  - SQL identifier validation to prevent SQL injection attacks
  - Input validation for limits, offsets, and identifiers
  - Maximum limit cap (10,000 rows) to prevent excessive memory usage
- **Debug Mode:**
  - Optional debug output via `MCP_DEBUG` environment variable
  - Debug logs written to `stderr` (MCP protocol compliant)
- **Parameter Compatibility:**
  - Support for `db_path`, `dbPath`, and `db_Path` parameter variants
  - Backward compatibility with camelCase parameter names

### Changed
- **MCP Protocol Compliance:**
  - Fixed `tools/call` response format to use standard MCP `content` array structure
  - Moved all debug output from `stdout` to `stderr` (MCP requires stdout for JSON-RPC only)
  - Protocol version handling now dynamically uses client's protocol version
- **Naming Standardization:**
  - All tool names standardized to `snake_case` (e.g., `list_tables`, `get_table_data`)
  - All parameter names standardized to `snake_case` (e.g., `db_path`, `include_columns`, `primary_keys`)
  - Response fields standardized to `snake_case` (e.g., `affected_rows`, `table_name`, `not_null`)
- **Code Quality:**
  - Upgraded to Java 21
  - Replaced magic numbers with named constants
  - Translated all German comments and log messages to English
  - Improved error messages with exception types and stack traces
- **Documentation:**
  - Updated all examples to use `snake_case` naming
  - Added security warnings for `execute_sql` tool
  - Added debug mode documentation
  - Improved troubleshooting section

### Fixed
- JSON parsing errors caused by debug output on `stdout`
- Response structure issues with Cursor and other MCP clients
- Protocol version mismatch between client and server

### Technical Details
- Java 21+ required (upgraded from Java 17)
- Uses sqlite-jdbc-crypt library (version 3.50.1.0)
- Gradle build system
- JUnit 5 for testing

## [0.1.0] - 2025-11-17

### Added
- Initial release
- MCP server implementation with STDIO transport
- Support for SQLCipher 4 encrypted databases
- `listTables` tool to enumerate database tables and columns
- `getTableData` tool to read table data with filtering and pagination
- `execQuery` tool to execute arbitrary SQL statements
- `insertOrUpdate` tool for UPSERT operations
- `deleteRows` tool to delete rows with filtering
- Configurable cipher profiles
- Default SQLCipher 4 configuration
- Error handling for FTS virtual tables
- Comprehensive logging support

### Technical Details
- Java 17+ required
- Uses sqlite-jdbc-crypt library (version 3.50.1.0)
- Gradle build system
- JUnit 5 for testing

