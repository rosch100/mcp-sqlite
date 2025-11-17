package com.example.mcp.sqlite;

import com.example.mcp.sqlite.config.CipherProfile;
import com.example.mcp.sqlite.config.DatabaseConfig;
import com.example.mcp.sqlite.util.SqlIdentifierValidator;
import com.example.mcp.sqlite.util.SqliteUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

import org.sqlite.SQLiteConfig;
import org.sqlite.mc.HmacAlgorithm;
import org.sqlite.mc.KdfAlgorithm;
import org.sqlite.mc.SQLiteMCConfig;
import org.sqlite.mc.SQLiteMCSqlCipherConfig;

public class EncryptedSqliteClient {
    public EncryptedSqliteClient() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to load org.sqlite.JDBC", e);
        }
    }

    public <T> T withConnection(DatabaseConfig config, SqlFunction<Connection, T> action) throws SQLException {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(action, "action");
        try (Connection connection = openConnection(config)) {
            return action.apply(connection);
        }
    }

    private Connection openConnection(DatabaseConfig config) throws SQLException {
        CipherProfile profile = config.cipherProfile();
        SQLiteMCSqlCipherConfig builder = SQLiteMCSqlCipherConfig.getV4Defaults()
                .setLegacy(4)
                .setLegacyPageSize(profile.pageSize())
                .setKdfIter(profile.kdfIterations())
                .setHmacUse(true)
                .setHmacAlgorithm(mapHmacAlgorithm(profile.hmacAlgorithm()))
                .setKdfAlgorithm(mapKdfAlgorithm(profile.kdfAlgorithm()))
                .setPlaintextHeaderSize(0);
        builder.withKey(config.passphrase());

        SQLiteMCConfig mcConfig = builder.build();
        mcConfig.setPragma(SQLiteConfig.Pragma.FOREIGN_KEYS, "ON");
        // Set busy timeout to 30 seconds to wait for locks to be released
        mcConfig.setBusyTimeout(30000);
        // Note: Read-only mode is set, but executeQuery can still execute writes
        // This is intentional for the execute_sql tool, but should be documented
        mcConfig.setReadOnly(true);

        String url = "jdbc:sqlite:" + config.databasePath();
        return mcConfig.createConnection(url);
    }

    private HmacAlgorithm mapHmacAlgorithm(String value) {
        if (value == null) {
            return HmacAlgorithm.SHA512;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (normalized.contains("SHA256")) {
            return HmacAlgorithm.SHA256;
        }
        if (normalized.contains("SHA1")) {
            return HmacAlgorithm.SHA1;
        }
        return HmacAlgorithm.SHA512;
    }

    private KdfAlgorithm mapKdfAlgorithm(String value) {
        if (value == null) {
            return KdfAlgorithm.SHA512;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        if (normalized.contains("256")) {
            return KdfAlgorithm.SHA256;
        }
        if (normalized.contains("SHA1")) {
            return KdfAlgorithm.SHA1;
        }
        return KdfAlgorithm.SHA512;
    }

    private boolean isIgnorablePragmaError(SQLException ex) {
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("unknown tokenizer") || normalized.contains("no such module");
    }

    public List<TableMetadata> listTables(Connection connection) throws SQLException {
        List<TableMetadata> tables = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name FROM sqlite_schema WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("name");
                    tables.add(new TableMetadata(tableName, describeColumns(connection, tableName)));
                }
            }
        }
        return tables;
    }

    public List<ColumnMetadata> describeColumns(Connection connection, String tableName) throws SQLException {
        SqlIdentifierValidator.validateIdentifier(tableName, "tableName");
        List<ColumnMetadata> columns = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("PRAGMA table_info(\"" + tableName + "\")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(new ColumnMetadata(
                            rs.getInt("cid"),
                            rs.getString("name"),
                            rs.getString("type"),
                            rs.getInt("notnull") == 1,
                            rs.getString("dflt_value"),
                            rs.getInt("pk") == 1
                    ));
                }
            }
        } catch (SQLException ex) {
            if (isIgnorablePragmaError(ex)) {
                return List.of();
            }
            throw ex;
        }
        return columns;
    }

    public QueryResult selectTable(Connection connection,
                                   String tableName,
                                   Map<String, Object> filters,
                                   List<String> columns,
                                   int limit,
                                   int offset) throws SQLException {
        SqlIdentifierValidator.validateIdentifier(tableName, "tableName");
        SqlIdentifierValidator.validateIdentifiers(columns, "columns");
        if (filters != null) {
            for (String column : filters.keySet()) {
                SqlIdentifierValidator.validateIdentifier(column, "filter column");
            }
        }
        
        String resolvedColumns = (columns == null || columns.isEmpty()) ? "*" : String.join(", ", columns);
        StringBuilder sql = new StringBuilder("SELECT ").append(resolvedColumns)
                .append(" FROM \"").append(tableName).append("\"");
        List<Object> parameters = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (String column : filters.keySet()) {
                if (!first) {
                    sql.append(" AND ");
                }
                sql.append('\"').append(column).append('\"').append(" = ?");
                parameters.add(filters.get(column));
                first = false;
            }
        }
        sql.append(" LIMIT ? OFFSET ?");
        parameters.add(limit);
        parameters.add(offset);

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                ps.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return QueryResult.from(rs);
            }
        }
    }

    /**
     * Executes an arbitrary SQL query. 
     * 
     * WARNING: This method executes raw SQL without parameterization. 
     * It should only be used when the SQL is trusted or when parameterized queries are not possible.
     * For user-provided SQL, ensure proper validation and sanitization is performed before calling this method.
     * 
     * @param connection The database connection
     * @param sql The SQL statement to execute
     * @return QueryResult containing the query results or affected rows
     * @throws SQLException if a database error occurs
     */
    public QueryResult executeQuery(Connection connection, String sql) throws SQLException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty");
        }
        try (Statement statement = connection.createStatement()) {
            boolean hasResultSet = statement.execute(sql);
            if (hasResultSet) {
                try (ResultSet rs = statement.getResultSet()) {
                    return QueryResult.from(rs);
                }
            }
            int affected = statement.getUpdateCount();
            return QueryResult.affectedRows(affected);
        }
    }

    public int insertOrUpdate(Connection connection,
                              String tableName,
                              List<String> primaryKeys,
                              List<Map<String, Object>> rows) throws SQLException {
        SqlIdentifierValidator.validateIdentifier(tableName, "tableName");
        if (primaryKeys == null || primaryKeys.isEmpty()) {
            throw new IllegalArgumentException("primaryKeys must be provided for insertOrUpdate");
        }
        SqlIdentifierValidator.validateIdentifiers(primaryKeys, "primaryKeys");
        int affected = 0;
        for (Map<String, Object> row : rows) {
            affected += upsertSingleRow(connection, tableName, primaryKeys, row);
        }
        return affected;
    }

    private int upsertSingleRow(Connection connection,
                                String tableName,
                                List<String> primaryKeys,
                                Map<String, Object> row) throws SQLException {
        List<String> columns = new ArrayList<>(row.keySet());
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Row data must contain at least one column");
        }
        SqlIdentifierValidator.validateIdentifiers(columns, "row columns");
        String columnList = String.join(", ", columns.stream().map(c -> '\"' + c + '\"').toList());
        String placeholders = String.join(", ", columns.stream().map(c -> "?").toList());
        List<String> assignmentTokens = columns.stream()
                .filter(col -> !primaryKeys.contains(col))
                .map(col -> '\"' + col + '\"' + " = excluded." + '\"' + col + '\"')
                .toList();
        String conflictTargets = String.join(", ", primaryKeys.stream().map(c -> '\"' + c + '\"').toList());
        String sql;
        if (assignmentTokens.isEmpty()) {
            sql = "INSERT INTO \"" + tableName + "\" (" + columnList + ") VALUES (" + placeholders + ") " +
                    "ON CONFLICT(" + conflictTargets + ") DO NOTHING";
        } else {
            String assignments = String.join(", ", assignmentTokens);
            sql = "INSERT INTO \"" + tableName + "\" (" + columnList + ") VALUES (" + placeholders + ") " +
                    "ON CONFLICT(" + conflictTargets + ") DO UPDATE SET " + assignments;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (String column : columns) {
                ps.setObject(index++, row.get(column));
            }
            return ps.executeUpdate();
        }
    }

    public int deleteRows(Connection connection,
                          String tableName,
                          Map<String, Object> filters) throws SQLException {
        SqlIdentifierValidator.validateIdentifier(tableName, "tableName");
        if (filters == null || filters.isEmpty()) {
            throw new IllegalArgumentException("filters are required for deleteRows");
        }
        for (String column : filters.keySet()) {
            SqlIdentifierValidator.validateIdentifier(column, "filter column");
        }
        StringBuilder sql = new StringBuilder("DELETE FROM \"")
                .append(tableName)
                .append("\" WHERE ");
        boolean first = true;
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            if (!first) {
                sql.append(" AND ");
            }
            sql.append('\"').append(entry.getKey()).append('\"').append(" = ?");
            params.add(entry.getValue());
            first = false;
        }
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        }
    }

    public List<IndexMetadata> listIndexes(Connection connection, String tableName) throws SQLException {
        SqlIdentifierValidator.validateIdentifier(tableName, "tableName");
        List<IndexMetadata> indexes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("PRAGMA index_list(\"" + tableName + "\")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String indexName = rs.getString("name");
                    boolean unique = rs.getInt("unique") == 1;
                    String origin = rs.getString("origin");
                    
                    // Hole Index-Spalten
                    List<String> columns = new ArrayList<>();
                    try (PreparedStatement ps2 = connection.prepareStatement("PRAGMA index_info(\"" + indexName + "\")")) {
                        try (ResultSet rs2 = ps2.executeQuery()) {
                            while (rs2.next()) {
                                String colName = rs2.getString("name");
                                if (colName != null) {
                                    columns.add(colName);
                                }
                            }
                        }
                    }
                    
                    indexes.add(new IndexMetadata(indexName, unique, origin, columns));
                }
            }
        } catch (SQLException ex) {
            if (isIgnorablePragmaError(ex)) {
                return List.of();
            }
            throw ex;
        }
        return indexes;
    }

    public TableSchemaMetadata getTableSchema(Connection connection, String tableName) throws SQLException {
        SqlIdentifierValidator.validateIdentifier(tableName, "tableName");
        // Columns
        List<ColumnMetadata> columns = describeColumns(connection, tableName);
        
        // Indexes
        List<IndexMetadata> indexes = listIndexes(connection, tableName);
        
        // Foreign Keys
        List<ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("PRAGMA foreign_key_list(\"" + tableName + "\")")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    foreignKeys.add(new ForeignKeyMetadata(
                        rs.getInt("id"),
                        rs.getInt("seq"),
                        rs.getString("table"),
                        rs.getString("from"),
                        rs.getString("to"),
                        rs.getString("on_update"),
                        rs.getString("on_delete"),
                        rs.getString("match")
                    ));
                }
            }
        } catch (SQLException ex) {
            if (!isIgnorablePragmaError(ex)) {
                throw ex;
            }
        }
        
        // Table Info (SQL CREATE Statement)
        String createSql = null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT sql FROM sqlite_schema WHERE type='table' AND name=?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    createSql = rs.getString("sql");
                }
            }
        }
        
        return new TableSchemaMetadata(tableName, columns, indexes, foreignKeys, createSql);
    }

    public record TableMetadata(String name, List<ColumnMetadata> columns) {}

    public record ColumnMetadata(int cid, String name, String type, boolean notNull, String defaultValue, boolean primaryKey) {}
    
    public record IndexMetadata(String name, boolean unique, String origin, List<String> columns) {}
    
    public record ForeignKeyMetadata(int id, int seq, String table, String from, String to, 
                                     String onUpdate, String onDelete, String match) {}
    
    public record TableSchemaMetadata(String tableName, List<ColumnMetadata> columns, 
                                     List<IndexMetadata> indexes, List<ForeignKeyMetadata> foreignKeys, 
                                     String createSql) {}

    public record QueryResult(List<String> columns, List<Map<String, Object>> rows, int affectedRows) {
        public static QueryResult from(ResultSet rs) throws SQLException {
            List<Map<String, Object>> rows = SqliteUtil.toRowList(rs);
            List<String> columns = rows.isEmpty() ? extractColumns(rs) : new ArrayList<>(rows.get(0).keySet());
            return new QueryResult(columns, rows, -1);
        }

        private static List<String> extractColumns(ResultSet rs) throws SQLException {
            ResultSetMetaData metaData = rs.getMetaData();
            int count = metaData.getColumnCount();
            List<String> columns = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                columns.add(metaData.getColumnLabel(i));
            }
            return columns;
        }

        public static QueryResult affectedRows(int affected) {
            return new QueryResult(List.of(), List.of(), affected);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T value) throws SQLException;
    }
}
