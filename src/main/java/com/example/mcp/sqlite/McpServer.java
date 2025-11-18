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
    
    // Constants for response size limits
    private static final int MAX_RESPONSE_SIZE_WARNING = 100_000; // 100KB
    private static final int MAX_PREVIEW_LENGTH = 200;
    private static final int MAX_PREVIEW_START = 100;
    private static final int MAX_STACK_TRACE_LENGTH = 2000;
    private static final int MAX_SQL_LOG_LENGTH = 100;

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final EncryptedSqliteClient sqliteClient = new EncryptedSqliteClient();
    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final PrintStream writer = System.out;
    private DatabaseConfig defaultConfig;
    private final boolean debugMode;

    public static void main(String[] args) throws IOException {
        // Check if debug mode is enabled via environment variable
        boolean debugMode = "true".equalsIgnoreCase(System.getenv("MCP_DEBUG"));
        
        // MCP protocol requires stdout to be used ONLY for JSON-RPC messages
        // All debug output must go to stderr to avoid interfering with the protocol
        if (debugMode) {
            System.err.println("=== MCP-SQLITE Server Start ===");
            System.err.println("Java Version: " + System.getProperty("java.version"));
            System.err.println("Java Home: " + System.getProperty("java.home"));
            System.err.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            System.err.println("Number of arguments: " + args.length);
            for (int i = 0; i < args.length; i++) {
                System.err.println("  Arg[" + i + "]: " + (args[i].length() > 100 ? args[i].substring(0, 100) + "..." : args[i]));
            }
        }
        
        DatabaseConfig config = null;
        if (args.length > 0 && args[0].equals("--args") && args.length > 1) {
            if (debugMode) {
                System.err.println("Parsing configuration from arguments...");
            }
            Gson gson = new Gson();
            JsonObject configJson = null;
            try {
                configJson = gson.fromJson(args[1], JsonObject.class);
            } catch (Exception parseEx) {
                System.err.println("ERROR: Could not parse configuration JSON: " + parseEx.getMessage());
                throw new IllegalArgumentException("Invalid JSON in --args: " + parseEx.getMessage(), parseEx);
            }
            
            if (configJson != null) {
                if (debugMode) {
                    System.err.println("Configuration parsed. Fields: " + configJson.keySet());
                }
                
                // Support db_path, dbPath, and db_Path for backward compatibility
                String dbPathKey = null;
                if (configJson.has("db_path")) {
                    dbPathKey = "db_path";
                } else if (configJson.has("dbPath")) {
                    dbPathKey = "dbPath";
                } else if (configJson.has("db_Path")) {
                    dbPathKey = "db_Path";
                }
                
                if (dbPathKey == null) {
                    throw new IllegalArgumentException("db_path is missing in configuration. " +
                            "Please use 'db_path' (recommended), 'dbPath', or 'db_Path'");
                }
                if (!configJson.has("passphrase")) {
                    throw new IllegalArgumentException("passphrase is missing in configuration");
                }
                
                Path dbPath = Path.of(configJson.get(dbPathKey).getAsString());
                String passphrase = configJson.get("passphrase").getAsString();
                
                if (debugMode) {
                    System.err.println("DB Path: " + dbPath);
                    System.err.println("Passphrase present: " + (passphrase != null && !passphrase.isEmpty()));
                }
                
                // Check if passphrase is encrypted (for logging)
                boolean isEncrypted = passphrase != null && passphrase.startsWith("encrypted:");
                if (debugMode && isEncrypted) {
                    System.err.println("Encrypted passphrase detected");
                }
                
                CipherProfile profile = CipherProfile.sqlCipher4Defaults();
                if (configJson.has("cipherProfile")) {
                    if (debugMode) {
                        System.err.println("Custom CipherProfile found");
                    }
                    JsonObject cipherJson = configJson.getAsJsonObject("cipherProfile");
                    CipherProfile.Builder builder = profile.toBuilder();
                    if (cipherJson.has("name")) builder.name(cipherJson.get("name").getAsString());
                    if (cipherJson.has("pageSize")) builder.pageSize(cipherJson.get("pageSize").getAsInt());
                    if (cipherJson.has("kdfIterations")) builder.kdfIterations(cipherJson.get("kdfIterations").getAsInt());
                    if (cipherJson.has("hmacAlgorithm")) builder.hmacAlgorithm(cipherJson.get("hmacAlgorithm").getAsString());
                    if (cipherJson.has("kdfAlgorithm")) builder.kdfAlgorithm(cipherJson.get("kdfAlgorithm").getAsString());
                    profile = builder.build();
                }
                
                try {
                    config = DatabaseConfig.withDecryptedPassphrase(dbPath, passphrase, profile);
                    if (debugMode && isEncrypted) {
                        System.err.println("Passphrase successfully decrypted");
                    }
                    if (debugMode) {
                        System.err.println("DatabaseConfig successfully created");
                    }
                } catch (Exception e) {
                    System.err.println("ERROR creating DatabaseConfig: " + e.getMessage());
                    e.printStackTrace(System.err);
                    LOGGER.severe("Error decrypting passphrase: " + e.getMessage());
                    throw e;
                }
            } else {
                if (debugMode) {
                    System.err.println("WARNING: configJson is null");
                }
            }
        } else {
            if (debugMode) {
                System.err.println("No configuration via arguments - server running without default config");
            }
        }
        
        if (debugMode) {
            System.err.println("=== Starting MCP Server ===");
        }
        new McpServer(config, debugMode).run();
    }

    public McpServer(DatabaseConfig defaultConfig) {
        this(defaultConfig, false);
    }
    
    public McpServer(DatabaseConfig defaultConfig, boolean debugMode) {
        this.defaultConfig = defaultConfig;
        this.debugMode = debugMode;
    }

    public void run() throws IOException {
        log("Server started - waiting for MCP events.");
        log("STDIN available: " + (System.in != null));
        log("STDOUT available: " + (System.out != null));
        log("STDERR available: " + (System.err != null));
        log("Default Config present: " + (defaultConfig != null));
        if (defaultConfig != null) {
            log("Default DB Path: " + defaultConfig.databasePath());
        }
        
        String line;
        int requestCount = 0;
        while ((line = reader.readLine()) != null) {
            requestCount++;
            if (line.isBlank()) {
                log("Empty line received (ignored)");
                continue;
            }
            
            // Log raw input with length and first/last chars for debugging
            int lineLength = line.length();
            String preview = lineLength > MAX_PREVIEW_LENGTH 
                ? line.substring(0, MAX_PREVIEW_START) + "..." + line.substring(lineLength - MAX_PREVIEW_START)
                : line;
            log("=== Request #" + requestCount + " ===");
            log("Received JSON (length: " + lineLength + " chars): " + preview);
            
            try {
                processLine(line.trim());
            } catch (Exception e) {
                logError("Critical error processing request #" + requestCount, e);
                // Try to send an error response if possible
                try {
                    sendErrorResponse(null, -32603, "Internal error: " + e.getMessage(), e);
                } catch (Exception sendError) {
                    logError("Could not send error response", sendError);
                }
            }
        }
        log("STDIN closed, server terminated. Processed requests: " + requestCount);
    }

    private void processLine(String json) {
        String requestId = null;
        try {
            // Validation: Check if JSON is valid
            if (json == null || json.trim().isEmpty()) {
                log("ERROR: Empty or null JSON string");
                return;
            }
            
            // Try to parse JSON
            RpcRequest request;
            try {
                request = gson.fromJson(json, RpcRequest.class);
            } catch (Exception parseEx) {
                logError("JSON parsing error", parseEx);
                log("Invalid JSON received. First 500 chars: " + 
                    (json.length() > 500 ? json.substring(0, 500) + "..." : json));
                sendErrorResponse(null, -32700, "Parse error: " + parseEx.getMessage(), parseEx);
                return;
            }
            
            if (request == null) {
                log("ERROR: Request is null after JSON parsing");
                sendErrorResponse(null, -32600, "Invalid Request: Request object is null", null);
                return;
            }
            
            // Validation: JSON-RPC Version
            if (request.jsonrpc != null && !request.jsonrpc.equals("2.0")) {
                log("WARNING: Unexpected JSON-RPC version: " + request.jsonrpc + " (expected: 2.0)");
            }
            
            if (request.method == null) {
                log("ERROR: Request without 'method' field");
                sendErrorResponse(request.id, -32600, "Invalid Request: Missing 'method' field", null);
                return;
            }
            
            requestId = request.id != null ? request.id : "<notification>";
            log("Processing request: method='" + request.method + "', id=" + requestId);
            
            if (request.params != null) {
                log("Params present: " + request.params.toString());
            } else {
                log("No params present");
            }

            // Handle notifications (no id)
            if (request.id == null) {
                log("Handling as notification (no ID)");
                handleNotification(request);
                return;
            }

            // Handle requests (with id)
            try {
                JsonElement result = handleRequest(request);
                sendSuccessResponse(request.id, request.method, result);
            } catch (IllegalArgumentException iae) {
                logError("Invalid parameters in method " + request.method, iae);
                sendErrorResponse(request.id, -32602, "Invalid params: " + iae.getMessage(), iae);
            } catch (SQLException sqle) {
                logError("SQL error in method " + request.method, sqle);
                sendErrorResponse(request.id, -32603, "Database error: " + sqle.getMessage(), sqle);
            } catch (Exception ex) {
                logError("Unexpected error in method " + request.method, ex);
                sendErrorResponse(request.id, -32603, "Internal error: " + ex.getMessage(), ex);
            }
        } catch (Exception ex) {
            logError("Critical exception in processLine (Request-ID: " + requestId + ")", ex);
            try {
                sendErrorResponse(requestId, -32603, "Critical error: " + ex.getMessage(), ex);
            } catch (Exception sendEx) {
                logError("Could not send error response after critical error", sendEx);
            }
        }
    }
    
    private void sendSuccessResponse(String id, String method, JsonElement result) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("jsonrpc", "2.0");
            response.addProperty("id", id);
            response.add("result", result);
            
            String jsonResponse = gson.toJson(response);
            int responseLength = jsonResponse.length();
            log("Method '" + method + "' successfully answered. Response size: " + responseLength + " chars");
            
            if (responseLength > MAX_RESPONSE_SIZE_WARNING) {
                log("WARNING: Response is very large (" + responseLength + " chars)");
                log("First 500 chars: " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));
            }
            
            writer.println(jsonResponse);
            boolean flushed = true;
            try {
                writer.flush();
            } catch (Exception flushEx) {
                flushed = false;
                logError("Error flushing response", flushEx);
            }
            
            if (flushed) {
                log("Response successfully written and flushed (ID: " + id + ")");
            } else {
                log("WARNING: Response written, but flush failed (ID: " + id + ")");
            }
        } catch (Exception ex) {
            logError("Error sending success response (ID: " + id + ")", ex);
            throw ex;
        }
    }
    
    private void sendErrorResponse(String id, int code, String message, Throwable throwable) {
        try {
            JsonObject response = new JsonObject();
            response.addProperty("jsonrpc", "2.0");
            if (id != null) {
                response.addProperty("id", id);
            } else {
                response.add("id", null);
            }
            
            JsonObject error = new JsonObject();
            error.addProperty("code", code);
            error.addProperty("message", message);
            
            // Add detailed error information for debugging
            if (throwable != null) {
                JsonObject errorData = new JsonObject();
                errorData.addProperty("exception", throwable.getClass().getName());
                String stackTrace = getStackTrace(throwable);
                errorData.addProperty("stackTrace", stackTrace);
                error.add("data", errorData);
            }
            
            response.add("error", error);
            
            String jsonResponse = gson.toJson(response);
            log("Sending error response (Code: " + code + ", ID: " + id + "): " + message);
            if (throwable != null) {
                log("Exception type: " + throwable.getClass().getName());
            }
            
            writer.println(jsonResponse);
            writer.flush();
            log("Error response successfully sent");
        } catch (Exception ex) {
            logError("CRITICAL: Could not send error response (ID: " + id + ")", ex);
            // Try to log at least a simple error message
            System.err.println("[CRITICAL ERROR] Failed to send error response: " + ex.getMessage());
        }
    }
    
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();
        // Limit stack trace to prevent excessive JSON size
        return stackTrace.length() > MAX_STACK_TRACE_LENGTH 
            ? stackTrace.substring(0, MAX_STACK_TRACE_LENGTH) + "..." 
            : stackTrace;
    }

    private void handleNotification(RpcRequest request) {
        if ("initialized".equals(request.method) || "notifications/initialized".equals(request.method)) {
            log("Notification 'initialized' received.");
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
        log("initialize called");
        if (params != null && params.size() > 0) {
            log("Initialize params: " + params.toString());
        }
        
        JsonObject result = new JsonObject();
        // Use the protocol version that the client supports (Cursor sends 2025-06-18)
        String clientProtocolVersion = params.has("protocolVersion") 
            ? params.get("protocolVersion").getAsString() 
            : "2024-11-05";
        result.addProperty("protocolVersion", clientProtocolVersion);
        result.addProperty("version", "0.2.3");
        result.addProperty("serverName", "encrypted-sqlite-mcp");

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "encrypted-sqlite-mcp");
        serverInfo.addProperty("version", "0.2.3");
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
        
        log("initialize successfully completed");
        return result;
    }

    private JsonElement handleToolsList() {
        log("tools/list called");
        JsonArray tools = new JsonArray();

        // list_tables
        JsonObject listTables = new JsonObject();
        listTables.addProperty("name", "list_tables");
        listTables.addProperty("description", "Lists all tables in the database. By default only table names, with include_columns=true also column details");
        JsonObject listTablesInput = new JsonObject();
        listTablesInput.addProperty("type", "object");
        JsonObject listTablesProps = new JsonObject();
        if (defaultConfig == null) {
            listTablesProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            listTablesProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        listTablesProps.add("include_columns", createSchemaProperty("boolean", "If true, column details are also returned (default: false)"));
        listTablesInput.add("properties", listTablesProps);
        if (defaultConfig == null) {
            JsonArray listTablesRequired = new JsonArray();
            listTablesRequired.add("db_path");
            listTablesRequired.add("passphrase");
            listTablesInput.add("required", listTablesRequired);
        }
        listTables.add("inputSchema", listTablesInput);
        tools.add(listTables);

        // get_table_data
        JsonObject getTableData = new JsonObject();
        getTableData.addProperty("name", "get_table_data");
        getTableData.addProperty("description", "Reads data from a table with optional filters, limit and offset");
        JsonObject getTableDataInput = new JsonObject();
        getTableDataInput.addProperty("type", "object");
        JsonObject getTableDataProps = new JsonObject();
        if (defaultConfig == null) {
            getTableDataProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            getTableDataProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        getTableDataProps.add("table", createSchemaProperty("string", "Table name"));
        getTableDataProps.add("columns", createSchemaProperty("array", "List of column names (optional)"));
        getTableDataProps.add("filters", createSchemaProperty("object", "Filters as key-value pairs (optional)"));
        getTableDataProps.add("limit", createSchemaProperty("number", "Maximum number of rows (default: 200)"));
        getTableDataProps.add("offset", createSchemaProperty("number", "Offset for pagination (default: 0)"));
        getTableDataInput.add("properties", getTableDataProps);
        JsonArray getTableDataRequired = new JsonArray();
        getTableDataRequired.add("table");
        if (defaultConfig == null) {
            getTableDataRequired.add("db_path");
            getTableDataRequired.add("passphrase");
        }
        getTableDataInput.add("required", getTableDataRequired);
        getTableData.add("inputSchema", getTableDataInput);
        tools.add(getTableData);

        // execute_sql
        JsonObject execQuery = new JsonObject();
        execQuery.addProperty("name", "execute_sql");
        execQuery.addProperty("description", "Executes an arbitrary SQL statement (SELECT, INSERT, UPDATE, DELETE, DDL)");
        JsonObject execQueryInput = new JsonObject();
        execQueryInput.addProperty("type", "object");
        JsonObject execQueryProps = new JsonObject();
        if (defaultConfig == null) {
            execQueryProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            execQueryProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        execQueryProps.add("sql", createSchemaProperty("string", "SQL statement"));
        execQueryInput.add("properties", execQueryProps);
        JsonArray execQueryRequired = new JsonArray();
        execQueryRequired.add("sql");
        if (defaultConfig == null) {
            execQueryRequired.add("db_path");
            execQueryRequired.add("passphrase");
        }
        execQueryInput.add("required", execQueryRequired);
        execQuery.add("inputSchema", execQueryInput);
        tools.add(execQuery);

        // insert_or_update
        JsonObject insertOrUpdate = new JsonObject();
        insertOrUpdate.addProperty("name", "insert_or_update");
        insertOrUpdate.addProperty("description", "Performs an UPSERT operation (INSERT or UPDATE on conflict)");
        JsonObject insertOrUpdateInput = new JsonObject();
        insertOrUpdateInput.addProperty("type", "object");
        JsonObject insertOrUpdateProps = new JsonObject();
        if (defaultConfig == null) {
            insertOrUpdateProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            insertOrUpdateProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        insertOrUpdateProps.add("table", createSchemaProperty("string", "Table name"));
        insertOrUpdateProps.add("primary_keys", createSchemaProperty("array", "List of primary key columns"));
        insertOrUpdateProps.add("rows", createSchemaProperty("array", "List of rows as objects"));
        insertOrUpdateInput.add("properties", insertOrUpdateProps);
        JsonArray insertOrUpdateRequired = new JsonArray();
        insertOrUpdateRequired.add("table");
        insertOrUpdateRequired.add("primary_keys");
        insertOrUpdateRequired.add("rows");
        if (defaultConfig == null) {
            insertOrUpdateRequired.add("db_path");
            insertOrUpdateRequired.add("passphrase");
        }
        insertOrUpdateInput.add("required", insertOrUpdateRequired);
        insertOrUpdate.add("inputSchema", insertOrUpdateInput);
        tools.add(insertOrUpdate);

        // delete_rows
        JsonObject deleteRows = new JsonObject();
        deleteRows.addProperty("name", "delete_rows");
        deleteRows.addProperty("description", "Deletes rows from a table based on filters");
        JsonObject deleteRowsInput = new JsonObject();
        deleteRowsInput.addProperty("type", "object");
        JsonObject deleteRowsProps = new JsonObject();
        if (defaultConfig == null) {
            deleteRowsProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            deleteRowsProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        deleteRowsProps.add("table", createSchemaProperty("string", "Table name"));
        deleteRowsProps.add("filters", createSchemaProperty("object", "Filters as key-value pairs"));
        deleteRowsInput.add("properties", deleteRowsProps);
        JsonArray deleteRowsRequired = new JsonArray();
        deleteRowsRequired.add("table");
        deleteRowsRequired.add("filters");
        if (defaultConfig == null) {
            deleteRowsRequired.add("db_path");
            deleteRowsRequired.add("passphrase");
        }
        deleteRowsInput.add("required", deleteRowsRequired);
        deleteRows.add("inputSchema", deleteRowsInput);
        tools.add(deleteRows);

        // get_table_schema
        JsonObject getTableSchema = new JsonObject();
        getTableSchema.addProperty("name", "get_table_schema");
        getTableSchema.addProperty("description", "Retrieves detailed schema information for a table (columns, indexes, foreign keys, constraints)");
        JsonObject getTableSchemaInput = new JsonObject();
        getTableSchemaInput.addProperty("type", "object");
        JsonObject getTableSchemaProps = new JsonObject();
        if (defaultConfig == null) {
            getTableSchemaProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            getTableSchemaProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        getTableSchemaProps.add("table", createSchemaProperty("string", "Table name"));
        getTableSchemaInput.add("properties", getTableSchemaProps);
        JsonArray getTableSchemaRequired = new JsonArray();
        getTableSchemaRequired.add("table");
        if (defaultConfig == null) {
            getTableSchemaRequired.add("db_path");
            getTableSchemaRequired.add("passphrase");
        }
        getTableSchemaInput.add("required", getTableSchemaRequired);
        getTableSchema.add("inputSchema", getTableSchemaInput);
        tools.add(getTableSchema);

        // list_indexes
        JsonObject listIndexes = new JsonObject();
        listIndexes.addProperty("name", "list_indexes");
        listIndexes.addProperty("description", "Lists all indexes of a table");
        JsonObject listIndexesInput = new JsonObject();
        listIndexesInput.addProperty("type", "object");
        JsonObject listIndexesProps = new JsonObject();
        if (defaultConfig == null) {
            listIndexesProps.add("db_path", createSchemaProperty("string", "Path to the database file"));
            listIndexesProps.add("passphrase", createSchemaProperty("string", "Passphrase for encryption"));
        }
        listIndexesProps.add("table", createSchemaProperty("string", "Table name"));
        listIndexesInput.add("properties", listIndexesProps);
        JsonArray listIndexesRequired = new JsonArray();
        listIndexesRequired.add("table");
        if (defaultConfig == null) {
            listIndexesRequired.add("db_path");
            listIndexesRequired.add("passphrase");
        }
        listIndexesInput.add("required", listIndexesRequired);
        listIndexes.add("inputSchema", listIndexesInput);
        tools.add(listIndexes);

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
        log("prompts/list called");
        JsonObject result = new JsonObject();
        result.add("prompts", new JsonArray());
        return result;
    }

    private JsonElement handleResourcesList() {
        log("resources/list called");
        JsonObject result = new JsonObject();
        result.add("resources", new JsonArray());
        return result;
    }

    private JsonElement handleToolsCall(JsonObject params) throws SQLException {
        log("handleToolsCall called");
        if (params == null) {
            throw new IllegalArgumentException("params must not be null");
        }
        
        String toolName = requiredString(params, "name");
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();
        log("tools/call for tool: " + toolName);
        log("Tool arguments present: " + (arguments != null && arguments.size() > 0));
        if (arguments != null && arguments.size() > 0) {
            log("Tool arguments: " + arguments.toString());
        }

        try {
            return switch (toolName) {
                case "list_tables" -> handleListTables(arguments);
                case "get_table_data" -> handleGetTableData(arguments);
                case "execute_sql" -> handleExecQuery(arguments);
                case "insert_or_update" -> handleInsertOrUpdate(arguments);
                case "delete_rows" -> handleDelete(arguments);
                case "get_table_schema" -> handleGetTableSchema(arguments);
                case "list_indexes" -> handleListIndexes(arguments);
                default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
            };
        } catch (Exception ex) {
            logError("Error executing tool '" + toolName + "'", ex);
            throw ex;
        }
    }

    private JsonElement handleListTables(JsonObject params) throws SQLException {
        log("handleListTables called");
        DatabaseConfig config = getDatabaseConfig(params);
        boolean includeColumns = (params.has("include_columns") && params.get("include_columns").getAsBoolean()) ||
                                 (params.has("includeColumns") && params.get("includeColumns").getAsBoolean()); // Backward compatibility
        log("include_columns: " + includeColumns);
        
        List<EncryptedSqliteClient.TableMetadata> tables = sqliteClient.withConnection(config, sqliteClient::listTables);
        log("Number of tables found: " + tables.size());
        
        JsonArray result = new JsonArray();
        for (EncryptedSqliteClient.TableMetadata table : tables) {
            JsonObject tableObj = new JsonObject();
            tableObj.addProperty("name", table.name());
            
            if (includeColumns) {
                JsonArray columns = new JsonArray();
                for (EncryptedSqliteClient.ColumnMetadata col : table.columns()) {
                    JsonObject colObj = new JsonObject();
                    colObj.addProperty("name", col.name());
                    colObj.addProperty("type", col.type());
                    colObj.addProperty("not_null", col.notNull());
                    colObj.addProperty("primary_key", col.primaryKey());
                    if (col.defaultValue() != null) {
                        colObj.addProperty("default_value", col.defaultValue());
                    }
                    columns.add(colObj);
                }
                tableObj.add("columns", columns);
            }
            
            result.add(tableObj);
        }
        
        // MCP tools/call response format according to specification:
        // The result should contain a "content" array with content items
        // Each content item has a "type" (e.g., "text") and "text" property
        JsonObject response = new JsonObject();
        JsonArray content = new JsonArray();
        JsonObject contentItem = new JsonObject();
        contentItem.addProperty("type", "text");
        
        // Create the data object with tables
        JsonObject dataObj = new JsonObject();
        dataObj.add("tables", result);
        
        // Convert to JSON string for text content
        contentItem.addProperty("text", gson.toJson(dataObj));
        content.add(contentItem);
        response.add("content", content);
        
        String responseJson = gson.toJson(response);
        log("handleListTables: Response size: " + responseJson.length() + " chars (includeColumns=" + includeColumns + ")");
        log("handleListTables: Response structure: {content: [{type: 'text', text: '{\"tables\": [...]}'}]} with " + result.size() + " tables");
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
        
        // Validate limit and offset to prevent negative values
        int limit = params.has("limit") ? params.get("limit").getAsInt() : 200;
        int offset = params.has("offset") ? params.get("offset").getAsInt() : 0;
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        // Cap limit to prevent excessive memory usage
        if (limit > 10000) {
            throw new IllegalArgumentException("limit cannot exceed 10000");
        }

        EncryptedSqliteClient.QueryResult result = sqliteClient.withConnection(config,
                conn -> sqliteClient.selectTable(conn, table, filters, columns, limit, offset));
        
        JsonObject response = new JsonObject();
        response.add("columns", gson.toJsonTree(result.columns()));
        response.add("rows", gson.toJsonTree(result.rows()));
        return response;
    }

    private JsonElement handleExecQuery(JsonObject params) throws SQLException {
        log("handleExecQuery called");
        DatabaseConfig config = getDatabaseConfig(params);
        String sql = requiredString(params, "sql");
        log("SQL query: " + sql.substring(0, Math.min(MAX_SQL_LOG_LENGTH, sql.length())));
        EncryptedSqliteClient.QueryResult result = sqliteClient.withConnection(config,
                conn -> sqliteClient.executeQuery(conn, sql));
        
        JsonObject response = new JsonObject();
        if (result.affectedRows() >= 0) {
            response.addProperty("affected_rows", result.affectedRows());
            log("handleExecQuery: affected_rows: " + result.affectedRows());
        } else {
            response.add("columns", gson.toJsonTree(result.columns()));
            response.add("rows", gson.toJsonTree(result.rows()));
            log("handleExecQuery: " + result.columns().size() + " columns, " + result.rows().size() + " rows");
            String responseJson = gson.toJson(response);
            log("handleExecQuery: Response size: " + responseJson.length() + " chars");
        }
        return response;
    }

    private JsonElement handleInsertOrUpdate(JsonObject params) throws SQLException {
        DatabaseConfig config = getDatabaseConfig(params);
        String table = requiredString(params, "table");
        // Support both snake_case and camelCase for backward compatibility
        if (!params.has("primary_keys") && !params.has("primaryKeys")) {
            throw new IllegalArgumentException("Missing required parameter: primary_keys");
        }
        ensureParam(params, "rows");
        JsonArray primaryKeysArray = params.has("primary_keys") 
            ? params.getAsJsonArray("primary_keys")
            : params.getAsJsonArray("primaryKeys");
        List<String> primaryKeys = gson.fromJson(primaryKeysArray, LIST_STRING_TYPE);
        List<Map<String, Object>> rows = gson.fromJson(params.getAsJsonArray("rows"), LIST_MAP_TYPE);
        Integer affected = sqliteClient.withConnection(config,
                conn -> sqliteClient.insertOrUpdate(conn, table, primaryKeys, rows));
        JsonObject response = new JsonObject();
        response.addProperty("affected_rows", affected);
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
        response.addProperty("affected_rows", affected);
        return response;
    }

    private JsonElement handleGetTableSchema(JsonObject params) throws SQLException {
        log("handleGetTableSchema called");
        DatabaseConfig config = getDatabaseConfig(params);
        String table = requiredString(params, "table");
        
        EncryptedSqliteClient.TableSchemaMetadata schema = sqliteClient.withConnection(config,
                conn -> sqliteClient.getTableSchema(conn, table));
        
        JsonObject response = new JsonObject();
        response.addProperty("table_name", schema.tableName());
        
        // Columns
        JsonArray columnsArray = new JsonArray();
        for (EncryptedSqliteClient.ColumnMetadata col : schema.columns()) {
            JsonObject colObj = new JsonObject();
            colObj.addProperty("cid", col.cid());
            colObj.addProperty("name", col.name());
            colObj.addProperty("type", col.type());
            colObj.addProperty("not_null", col.notNull());
            colObj.addProperty("primary_key", col.primaryKey());
            if (col.defaultValue() != null) {
                colObj.addProperty("default_value", col.defaultValue());
            }
            columnsArray.add(colObj);
        }
        response.add("columns", columnsArray);
        
        // Indexes
        JsonArray indexesArray = new JsonArray();
        for (EncryptedSqliteClient.IndexMetadata idx : schema.indexes()) {
            JsonObject idxObj = new JsonObject();
            idxObj.addProperty("name", idx.name());
            idxObj.addProperty("unique", idx.unique());
            idxObj.addProperty("origin", idx.origin());
            idxObj.add("columns", gson.toJsonTree(idx.columns()));
            indexesArray.add(idxObj);
        }
        response.add("indexes", indexesArray);
        
        // Foreign keys
        JsonArray foreignKeysArray = new JsonArray();
        for (EncryptedSqliteClient.ForeignKeyMetadata fk : schema.foreignKeys()) {
            JsonObject fkObj = new JsonObject();
            fkObj.addProperty("id", fk.id());
            fkObj.addProperty("seq", fk.seq());
            fkObj.addProperty("table", fk.table());
            fkObj.addProperty("from", fk.from());
            fkObj.addProperty("to", fk.to());
            if (fk.onUpdate() != null) fkObj.addProperty("on_update", fk.onUpdate());
            if (fk.onDelete() != null) fkObj.addProperty("on_delete", fk.onDelete());
            if (fk.match() != null) fkObj.addProperty("match", fk.match());
            foreignKeysArray.add(fkObj);
        }
        response.add("foreign_keys", foreignKeysArray);
        
        // CREATE SQL statement
        if (schema.createSql() != null) {
            response.addProperty("create_sql", schema.createSql());
        }
        
        log("handleGetTableSchema: Schema for table '" + table + "' successfully retrieved");
        return response;
    }

    private JsonElement handleListIndexes(JsonObject params) throws SQLException {
        log("handleListIndexes called");
        DatabaseConfig config = getDatabaseConfig(params);
        String table = requiredString(params, "table");
        
        List<EncryptedSqliteClient.IndexMetadata> indexes = sqliteClient.withConnection(config,
                conn -> sqliteClient.listIndexes(conn, table));
        
        JsonArray result = new JsonArray();
        for (EncryptedSqliteClient.IndexMetadata idx : indexes) {
            JsonObject idxObj = new JsonObject();
            idxObj.addProperty("name", idx.name());
            idxObj.addProperty("unique", idx.unique());
            idxObj.addProperty("origin", idx.origin());
            idxObj.add("columns", gson.toJsonTree(idx.columns()));
            result.add(idxObj);
        }
        
        JsonObject response = new JsonObject();
        response.add("indexes", result);
        log("handleListIndexes: " + indexes.size() + " indexes found for table '" + table + "'");
        return response;
    }

    private DatabaseConfig getDatabaseConfig(JsonObject params) {
        try {
            if (defaultConfig != null) {
                log("Using default config with optional overrides");
                // Use default config if provided, allow override
                // Support db_path, dbPath, and db_Path for backward compatibility
                Path dbPath;
                if (params.has("db_path")) {
                    dbPath = Path.of(params.get("db_path").getAsString());
                } else if (params.has("dbPath")) {
                    dbPath = Path.of(params.get("dbPath").getAsString());
                } else if (params.has("db_Path")) {
                    dbPath = Path.of(params.get("db_Path").getAsString());
                } else {
                    dbPath = defaultConfig.databasePath();
                }
                String passphrase = params.has("passphrase") ? params.get("passphrase").getAsString() : defaultConfig.passphrase();
                boolean passphraseOverridden = params.has("passphrase");
                boolean dbPathOverridden = params.has("db_path") || params.has("dbPath") || params.has("db_Path");
                log("DB Path: " + dbPath + (dbPathOverridden ? " (override)" : " (default)"));
                log("Passphrase present: " + (passphrase != null) + (passphraseOverridden ? " (override)" : " (default)"));
                if (passphrase != null && passphrase.startsWith("encrypted:")) {
                    log("Encrypted passphrase detected");
                }
                
                CipherProfile profile = defaultConfig.cipherProfile();
                if (params.has("cipherProfile")) {
                    log("CipherProfile being overridden");
                    profile = resolveCipherProfile(params.getAsJsonObject("cipherProfile"));
                }
                return DatabaseConfig.withDecryptedPassphrase(dbPath, passphrase, profile);
            } else {
                log("No default config, using parameters");
                // Must be provided in params
                return databaseConfig(params);
            }
        } catch (Exception ex) {
            logError("Error creating DatabaseConfig", ex);
            throw ex;
        }
    }

    private DatabaseConfig databaseConfig(JsonObject params) {
        // Support db_path, dbPath, and db_Path for backward compatibility
        String dbPathStr = null;
        if (params.has("db_path")) {
            dbPathStr = params.get("db_path").getAsString();
        } else if (params.has("dbPath")) {
            dbPathStr = params.get("dbPath").getAsString();
        } else if (params.has("db_Path")) {
            dbPathStr = params.get("db_Path").getAsString();
        }
        
        if (dbPathStr == null) {
            throw new IllegalArgumentException("Missing required parameter: db_path (or dbPath/db_Path)");
        }
        Path dbPath = Path.of(dbPathStr);
        String passphrase = requiredString(params, "passphrase");
        CipherProfile profile = resolveCipherProfile(params.has("cipherProfile") ? params.getAsJsonObject("cipherProfile") : null);
        return DatabaseConfig.withDecryptedPassphrase(dbPath, passphrase, profile);
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
        // Output debug logs to stderr (MCP protocol requires stdout for JSON-RPC only)
        // Only log if debug mode is enabled
        if (debugMode) {
            String timestamp = java.time.LocalDateTime.now().toString();
            String logMessage = "[" + timestamp + "] [DEBUG] " + message;
            System.err.println(logMessage);
        }
        LOGGER.fine(message);
    }

    private void logError(String message, Throwable throwable) {
        String timestamp = java.time.LocalDateTime.now().toString();
        String errorMessage = "[" + timestamp + "] [ERROR] " + message;
        System.err.println(errorMessage);
        if (throwable != null) {
            System.err.println("Exception: " + throwable.getClass().getName() + ": " + throwable.getMessage());
            // Print stack trace to stderr for debugging
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            throwable.printStackTrace(pw);
            System.err.println(sw.toString());
        }
        LOGGER.log(Level.SEVERE, "[MCP-SQLITE] " + message, throwable);
    }
}
