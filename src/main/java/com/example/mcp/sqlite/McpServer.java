package com.example.mcp.sqlite;

import com.example.mcp.sqlite.config.CipherProfile;
import com.example.mcp.sqlite.config.DatabaseConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class McpServer {
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private static final Type LIST_MAP_TYPE = new TypeToken<List<Map<String, Object>>>() {}.getType();
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>() {}.getType();

    private static final Logger LOGGER = Logger.getLogger(McpServer.class.getName());

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final EncryptedSqliteClient sqliteClient = new EncryptedSqliteClient();
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final PrintStream writer = System.out;
    private DatabaseConfig defaultConfig;

    public static void main(String[] args) throws IOException {
        DatabaseConfig config = null;
        if (args.length > 0 && args[0].equals("--args") && args.length > 1) {
            Gson gson = new Gson();
            JsonObject configJson = gson.fromJson(args[1], JsonObject.class);
            if (configJson != null) {
                Path dbPath = Path.of(configJson.get("dbPath").getAsString());
                String passphrase = configJson.get("passphrase").getAsString();
                CipherProfile profile = CipherProfile.sqlCipher4Defaults();
                if (configJson.has("cipherProfile")) {
                    JsonObject cipherJson = configJson.getAsJsonObject("cipherProfile");
                    CipherProfile.Builder builder = profile.toBuilder();
                    if (cipherJson.has("name")) builder.name(cipherJson.get("name").getAsString());
                    if (cipherJson.has("pageSize")) builder.pageSize(cipherJson.get("pageSize").getAsInt());
                    if (cipherJson.has("kdfIterations")) builder.kdfIterations(cipherJson.get("kdfIterations").getAsInt());
                    if (cipherJson.has("hmacAlgorithm")) builder.hmacAlgorithm(cipherJson.get("hmacAlgorithm").getAsString());
                    if (cipherJson.has("kdfAlgorithm")) builder.kdfAlgorithm(cipherJson.get("kdfAlgorithm").getAsString());
                    profile = builder.build();
                }
                config = new DatabaseConfig(dbPath, passphrase, profile);
            }
        }
        new McpServer(config).run();
    }

    public McpServer(DatabaseConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    public void run() throws IOException {
        log("Server gestartet – warte auf MCP-Ereignisse.");
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            log("Empfange JSON: " + line);
            processLine(line.trim());
        }
        log("STDIN geschlossen, Server beendet.");
    }

    private void processLine(String json) {
        try {
            RpcRequest request = gson.fromJson(json, RpcRequest.class);
            if (request == null || request.method == null) {
                log("Ungültiger Request, wird ignoriert.");
                return;
            }

            // Handle notifications (no id)
            if (request.id == null) {
                handleNotification(request);
                return;
            }

            // Handle requests (with id)
            JsonObject response = new JsonObject();
            response.addProperty("jsonrpc", "2.0");
            response.addProperty("id", request.id);
            try {
                JsonElement result = handleRequest(request);
                response.add("result", result);
                log("Methode " + request.method + " erfolgreich beantwortet.");
            } catch (Exception ex) {
                JsonObject error = new JsonObject();
                error.addProperty("code", -32603);
                error.addProperty("message", ex.getMessage());
                response.add("error", error);
                logError("Fehler in Methode " + request.method, ex);
            }
            writer.println(gson.toJson(response));
            writer.flush();
        } catch (Exception ex) {
            logError("Ausnahme bei processLine", ex);
        }
    }

    private void handleNotification(RpcRequest request) {
        if ("initialized".equals(request.method) || "notifications/initialized".equals(request.method)) {
            log("Notification 'initialized' empfangen.");
        }
    }

    private JsonElement handleRequest(RpcRequest request) throws SQLException {
        String method = request.method;
        JsonObject params = request.params == null ? new JsonObject() : request.params.getAsJsonObject();

        return switch (method) {
            case "initialize" -> handleInitialize(params);
            case "tools/list" -> handleToolsList();
            case "tools/call" -> handleToolsCall(params);
            case "prompts/list" -> handlePromptsList();
            case "resources/list" -> handleResourcesList();
            default -> throw new IllegalArgumentException("Unknown method: " + method);
        };
    }

    private JsonElement handleInitialize(JsonObject params) {
        log("initialize aufgerufen");
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", "2024-11-05");
        result.addProperty("version", "0.1.0");
        result.addProperty("serverName", "encrypted-sqlite-mcp");

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "encrypted-sqlite-mcp");
        serverInfo.addProperty("version", "0.1.0");
        result.add("serverInfo", serverInfo);

        JsonObject capabilities = new JsonObject();
        JsonObject toolsCap = new JsonObject();
        toolsCap.addProperty("listChanged", false);
        capabilities.add("tools", toolsCap);
        capabilities.add("prompts", new JsonObject());
        capabilities.add("resources", new JsonObject());
        capabilities.add("logging", new JsonObject());
        JsonObject elicitationCap = new JsonObject();
        elicitationCap.addProperty("listChanged", false);
        capabilities.add("elicitation", elicitationCap);
        JsonObject rootsCap = new JsonObject();
        rootsCap.addProperty("listChanged", false);
        capabilities.add("roots", rootsCap);
        result.add("capabilities", capabilities);
        return result;
    }

    private JsonElement handleToolsList() {
        log("tools/list aufgerufen");
        JsonArray tools = new JsonArray();

        // listTables
        JsonObject listTables = new JsonObject();
        listTables.addProperty("name", "listTables");
        listTables.addProperty("description", "Listet alle Tabellen in der Datenbank mit ihren Spalten auf");
        JsonObject listTablesInput = new JsonObject();
        listTablesInput.addProperty("type", "object");
        JsonObject listTablesProps = new JsonObject();
        if (defaultConfig == null) {
            listTablesProps.add("dbPath", createSchemaProperty("string", "Pfad zur Datenbankdatei"));
            listTablesProps.add("passphrase", createSchemaProperty("string", "Passphrase für die Verschlüsselung"));
        }
        listTablesInput.add("properties", listTablesProps);
        if (defaultConfig == null) {
            JsonArray listTablesRequired = new JsonArray();
            listTablesRequired.add("dbPath");
            listTablesRequired.add("passphrase");
            listTablesInput.add("required", listTablesRequired);
        }
        listTables.add("inputSchema", listTablesInput);
        tools.add(listTables);

        // getTableData
        JsonObject getTableData = new JsonObject();
        getTableData.addProperty("name", "getTableData");
        getTableData.addProperty("description", "Liest Daten aus einer Tabelle mit optionalen Filtern, Limit und Offset");
        JsonObject getTableDataInput = new JsonObject();
        getTableDataInput.addProperty("type", "object");
        JsonObject getTableDataProps = new JsonObject();
        if (defaultConfig == null) {
            getTableDataProps.add("dbPath", createSchemaProperty("string", "Pfad zur Datenbankdatei"));
            getTableDataProps.add("passphrase", createSchemaProperty("string", "Passphrase für die Verschlüsselung"));
        }
        getTableDataProps.add("table", createSchemaProperty("string", "Name der Tabelle"));
        getTableDataProps.add("columns", createSchemaProperty("array", "Liste der Spaltennamen (optional)"));
        getTableDataProps.add("filters", createSchemaProperty("object", "Filter als Key-Value-Paare (optional)"));
        getTableDataProps.add("limit", createSchemaProperty("number", "Maximale Anzahl Zeilen (Standard: 200)"));
        getTableDataProps.add("offset", createSchemaProperty("number", "Offset für Pagination (Standard: 0)"));
        getTableDataInput.add("properties", getTableDataProps);
        JsonArray getTableDataRequired = new JsonArray();
        getTableDataRequired.add("table");
        if (defaultConfig == null) {
            getTableDataRequired.add("dbPath");
            getTableDataRequired.add("passphrase");
        }
        getTableDataInput.add("required", getTableDataRequired);
        getTableData.add("inputSchema", getTableDataInput);
        tools.add(getTableData);

        // execQuery
        JsonObject execQuery = new JsonObject();
        execQuery.addProperty("name", "execQuery");
        execQuery.addProperty("description", "Führt ein beliebiges SQL-Statement aus (SELECT, INSERT, UPDATE, DELETE, DDL)");
        JsonObject execQueryInput = new JsonObject();
        execQueryInput.addProperty("type", "object");
        JsonObject execQueryProps = new JsonObject();
        if (defaultConfig == null) {
            execQueryProps.add("dbPath", createSchemaProperty("string", "Pfad zur Datenbankdatei"));
            execQueryProps.add("passphrase", createSchemaProperty("string", "Passphrase für die Verschlüsselung"));
        }
        execQueryProps.add("sql", createSchemaProperty("string", "SQL-Statement"));
        execQueryInput.add("properties", execQueryProps);
        JsonArray execQueryRequired = new JsonArray();
        execQueryRequired.add("sql");
        if (defaultConfig == null) {
            execQueryRequired.add("dbPath");
            execQueryRequired.add("passphrase");
        }
        execQueryInput.add("required", execQueryRequired);
        execQuery.add("inputSchema", execQueryInput);
        tools.add(execQuery);

        // insertOrUpdate
        JsonObject insertOrUpdate = new JsonObject();
        insertOrUpdate.addProperty("name", "insertOrUpdate");
        insertOrUpdate.addProperty("description", "Führt einen UPSERT durch (INSERT oder UPDATE bei Konflikt)");
        JsonObject insertOrUpdateInput = new JsonObject();
        insertOrUpdateInput.addProperty("type", "object");
        JsonObject insertOrUpdateProps = new JsonObject();
        if (defaultConfig == null) {
            insertOrUpdateProps.add("dbPath", createSchemaProperty("string", "Pfad zur Datenbankdatei"));
            insertOrUpdateProps.add("passphrase", createSchemaProperty("string", "Passphrase für die Verschlüsselung"));
        }
        insertOrUpdateProps.add("table", createSchemaProperty("string", "Name der Tabelle"));
        insertOrUpdateProps.add("primaryKeys", createSchemaProperty("array", "Liste der Primärschlüssel-Spalten"));
        insertOrUpdateProps.add("rows", createSchemaProperty("array", "Liste der Zeilen als Objekte"));
        insertOrUpdateInput.add("properties", insertOrUpdateProps);
        JsonArray insertOrUpdateRequired = new JsonArray();
        insertOrUpdateRequired.add("table");
        insertOrUpdateRequired.add("primaryKeys");
        insertOrUpdateRequired.add("rows");
        if (defaultConfig == null) {
            insertOrUpdateRequired.add("dbPath");
            insertOrUpdateRequired.add("passphrase");
        }
        insertOrUpdateInput.add("required", insertOrUpdateRequired);
        insertOrUpdate.add("inputSchema", insertOrUpdateInput);
        tools.add(insertOrUpdate);

        // deleteRows
        JsonObject deleteRows = new JsonObject();
        deleteRows.addProperty("name", "deleteRows");
        deleteRows.addProperty("description", "Löscht Zeilen aus einer Tabelle anhand von Filtern");
        JsonObject deleteRowsInput = new JsonObject();
        deleteRowsInput.addProperty("type", "object");
        JsonObject deleteRowsProps = new JsonObject();
        if (defaultConfig == null) {
            deleteRowsProps.add("dbPath", createSchemaProperty("string", "Pfad zur Datenbankdatei"));
            deleteRowsProps.add("passphrase", createSchemaProperty("string", "Passphrase für die Verschlüsselung"));
        }
        deleteRowsProps.add("table", createSchemaProperty("string", "Name der Tabelle"));
        deleteRowsProps.add("filters", createSchemaProperty("object", "Filter als Key-Value-Paare"));
        deleteRowsInput.add("properties", deleteRowsProps);
        JsonArray deleteRowsRequired = new JsonArray();
        deleteRowsRequired.add("table");
        deleteRowsRequired.add("filters");
        if (defaultConfig == null) {
            deleteRowsRequired.add("dbPath");
            deleteRowsRequired.add("passphrase");
        }
        deleteRowsInput.add("required", deleteRowsRequired);
        deleteRows.add("inputSchema", deleteRowsInput);
        tools.add(deleteRows);

        JsonObject result = new JsonObject();
        result.add("tools", tools);
        return result;
    }

    private JsonObject createSchemaProperty(String type, String description) {
        JsonObject prop = new JsonObject();
        prop.addProperty("type", type);
        prop.addProperty("description", description);
        return prop;
    }

    private JsonElement handlePromptsList() {
        log("prompts/list aufgerufen");
        JsonObject result = new JsonObject();
        result.add("prompts", new JsonArray());
        return result;
    }

    private JsonElement handleResourcesList() {
        log("resources/list aufgerufen");
        JsonObject result = new JsonObject();
        result.add("resources", new JsonArray());
        return result;
    }

    private JsonElement handleToolsCall(JsonObject params) throws SQLException {
        String toolName = requiredString(params, "name");
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();
        log("tools/call für Tool: " + toolName);

        return switch (toolName) {
            case "listTables" -> handleListTables(arguments);
            case "getTableData" -> handleGetTableData(arguments);
            case "execQuery" -> handleExecQuery(arguments);
            case "insertOrUpdate" -> handleInsertOrUpdate(arguments);
            case "deleteRows" -> handleDelete(arguments);
            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    private JsonElement handleListTables(JsonObject params) throws SQLException {
        DatabaseConfig config = getDatabaseConfig(params);
        List<EncryptedSqliteClient.TableMetadata> tables = sqliteClient.withConnection(config, sqliteClient::listTables);
        
        JsonArray result = new JsonArray();
        for (EncryptedSqliteClient.TableMetadata table : tables) {
            JsonObject tableObj = new JsonObject();
            tableObj.addProperty("name", table.name());
            JsonArray columns = new JsonArray();
            for (EncryptedSqliteClient.ColumnMetadata col : table.columns()) {
                JsonObject colObj = new JsonObject();
                colObj.addProperty("name", col.name());
                colObj.addProperty("type", col.type());
                colObj.addProperty("notNull", col.notNull());
                colObj.addProperty("primaryKey", col.primaryKey());
                if (col.defaultValue() != null) {
                    colObj.addProperty("defaultValue", col.defaultValue());
                }
                columns.add(colObj);
            }
            tableObj.add("columns", columns);
            result.add(tableObj);
        }
        
        JsonObject response = new JsonObject();
        response.add("tables", result);
        return response;
    }

    private JsonElement handleGetTableData(JsonObject params) throws SQLException {
        DatabaseConfig config = getDatabaseConfig(params);
        String table = requiredString(params, "table");
        List<String> columns = params.has("columns")
                ? gson.fromJson(params.getAsJsonArray("columns"), LIST_STRING_TYPE)
                : null;
        Map<String, Object> filters = params.has("filters")
                ? gson.fromJson(params.getAsJsonObject("filters"), MAP_TYPE)
                : Map.of();
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 200;
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;

        EncryptedSqliteClient.QueryResult result = sqliteClient.withConnection(config,
                conn -> sqliteClient.selectTable(conn, table, filters, columns, limit, offset));
        
        JsonObject response = new JsonObject();
        response.add("columns", gson.toJsonTree(result.columns()));
        response.add("rows", gson.toJsonTree(result.rows()));
        return response;
    }

    private JsonElement handleExecQuery(JsonObject params) throws SQLException {
        DatabaseConfig config = getDatabaseConfig(params);
        String sql = requiredString(params, "sql");
        EncryptedSqliteClient.QueryResult result = sqliteClient.withConnection(config,
                conn -> sqliteClient.executeQuery(conn, sql));
        
        JsonObject response = new JsonObject();
        if (result.affectedRows() >= 0) {
            response.addProperty("affectedRows", result.affectedRows());
        } else {
            response.add("columns", gson.toJsonTree(result.columns()));
            response.add("rows", gson.toJsonTree(result.rows()));
        }
        return response;
    }

    private JsonElement handleInsertOrUpdate(JsonObject params) throws SQLException {
        DatabaseConfig config = getDatabaseConfig(params);
        String table = requiredString(params, "table");
        ensureParam(params, "primaryKeys");
        ensureParam(params, "rows");
        List<String> primaryKeys = gson.fromJson(params.getAsJsonArray("primaryKeys"), LIST_STRING_TYPE);
        List<Map<String, Object>> rows = gson.fromJson(params.getAsJsonArray("rows"), LIST_MAP_TYPE);
        Integer affected = sqliteClient.withConnection(config,
                conn -> sqliteClient.insertOrUpdate(conn, table, primaryKeys, rows));
        JsonObject response = new JsonObject();
        response.addProperty("affectedRows", affected);
        return response;
    }

    private JsonElement handleDelete(JsonObject params) throws SQLException {
        DatabaseConfig config = getDatabaseConfig(params);
        String table = requiredString(params, "table");
        ensureParam(params, "filters");
        Map<String, Object> filters = gson.fromJson(params.getAsJsonObject("filters"), MAP_TYPE);
        Integer affected = sqliteClient.withConnection(config,
                conn -> sqliteClient.deleteRows(conn, table, filters));
        JsonObject response = new JsonObject();
        response.addProperty("affectedRows", affected);
        return response;
    }

    private DatabaseConfig getDatabaseConfig(JsonObject params) {
        if (defaultConfig != null) {
            // Use default config if provided, allow override
            Path dbPath = params.has("dbPath") ? Path.of(params.get("dbPath").getAsString()) : defaultConfig.databasePath();
            String passphrase = params.has("passphrase") ? params.get("passphrase").getAsString() : defaultConfig.passphrase();
            CipherProfile profile = defaultConfig.cipherProfile();
            if (params.has("cipherProfile")) {
                profile = resolveCipherProfile(params.getAsJsonObject("cipherProfile"));
            }
            return new DatabaseConfig(dbPath, passphrase, profile);
        } else {
            // Must be provided in params
            return databaseConfig(params);
        }
    }

    private DatabaseConfig databaseConfig(JsonObject params) {
        Path dbPath = Path.of(requiredString(params, "dbPath"));
        String passphrase = requiredString(params, "passphrase");
        CipherProfile profile = resolveCipherProfile(params.has("cipherProfile") ? params.getAsJsonObject("cipherProfile") : null);
        return new DatabaseConfig(dbPath, passphrase, profile);
    }

    private CipherProfile resolveCipherProfile(JsonObject json) {
        CipherProfile profile = CipherProfile.sqlCipher4Defaults();
        if (json == null) {
            return profile;
        }
        CipherProfile.Builder builder = profile.toBuilder();
        if (json.has("name")) {
            builder.name(json.get("name").getAsString());
        }
        if (json.has("pageSize")) {
            builder.pageSize(json.get("pageSize").getAsInt());
        }
        if (json.has("kdfIterations")) {
            builder.kdfIterations(json.get("kdfIterations").getAsInt());
        }
        if (json.has("hmacAlgorithm")) {
            builder.hmacAlgorithm(json.get("hmacAlgorithm").getAsString());
        }
        if (json.has("kdfAlgorithm")) {
            builder.kdfAlgorithm(json.get("kdfAlgorithm").getAsString());
        }
        return builder.build();
    }

    private String requiredString(JsonObject params, String key) {
        if (!params.has(key)) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
        return Objects.requireNonNull(params.get(key).getAsString(), key + " may not be null");
    }

    private void ensureParam(JsonObject params, String key) {
        if (!params.has(key)) {
            throw new IllegalArgumentException("Missing required parameter: " + key);
        }
    }

    private record RpcRequest(String jsonrpc, String method, JsonObject params, String id) {}

    private void log(String message) {
        LOGGER.fine(message);
    }

    private void logError(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, "[MCP-SQLITE] " + message, throwable);
    }
}
