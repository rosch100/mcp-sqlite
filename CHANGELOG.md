# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

