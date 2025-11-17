#!/usr/bin/env python3
"""
Interaktives Test-Skript für den MCP-SQLITE Server.
Simuliert die Kommunikation mit dem Server über STDIO.
"""

import json
import subprocess
import sys
import os
from typing import Optional

class McpClient:
    def __init__(self, db_path: str, passphrase: str, server_binary: str = "./build/install/mcp-sqlite/bin/mcp-sqlite"):
        self.db_path = db_path
        self.passphrase = passphrase
        self.server_binary = server_binary
        self.request_id = 0
        
    def _next_id(self) -> int:
        self.request_id += 1
        return self.request_id
    
    def _send_request(self, method: str, params: dict, capture_stderr: bool = True) -> dict:
        """Sendet einen JSON-RPC Request an den Server"""
        request = {
            "jsonrpc": "2.0",
            "id": self._next_id(),
            "method": method,
            "params": params
        }
        
        json_request = json.dumps(request) + "\n"
        
        # Starte Server-Prozess
        args = [
            self.server_binary,
            "--args",
            json.dumps({
                "dbPath": self.db_path,
                "passphrase": self.passphrase
            })
        ]
        
        try:
            process = subprocess.Popen(
                args,
                stdin=subprocess.PIPE,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE if capture_stderr else subprocess.DEVNULL,
                text=True,
                bufsize=1
            )
            
            # Sende Request
            stdout, stderr = process.communicate(input=json_request, timeout=10)
            
            # Parse Response
            if stdout:
                for line in stdout.strip().split('\n'):
                    if line.strip():
                        try:
                            return json.loads(line)
                        except json.JSONDecodeError:
                            continue
            
            # Wenn keine JSON-Response, gib Fehler zurück
            return {
                "error": {
                    "code": -32603,
                    "message": "No valid JSON response received",
                    "data": {
                        "stdout": stdout[:500] if stdout else "",
                        "stderr": stderr[:500] if stderr else ""
                    }
                }
            }
            
        except subprocess.TimeoutExpired:
            process.kill()
            return {"error": {"code": -32603, "message": "Request timeout"}}
        except Exception as e:
            return {"error": {"code": -32603, "message": str(e)}}
    
    def initialize(self) -> dict:
        """Initialisiert die Verbindung zum Server"""
        return self._send_request("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {
                "name": "test-client",
                "version": "1.0.0"
            }
        })
    
    def list_tools(self) -> dict:
        """Listet alle verfügbaren Tools"""
        return self._send_request("tools/list", {})
    
    def list_tables(self, include_columns: bool = False) -> dict:
        """Listet alle Tabellen in der Datenbank"""
        arguments = {}
        if include_columns:
            arguments["includeColumns"] = True
        
        return self._send_request("tools/call", {
            "name": "listTables",
            "arguments": arguments
        })
    
    def get_table_data(self, table: str, columns: Optional[list] = None, 
                       filters: Optional[dict] = None, limit: int = 200, 
                       offset: int = 0) -> dict:
        """Liest Daten aus einer Tabelle"""
        arguments = {"table": table, "limit": limit, "offset": offset}
        if columns:
            arguments["columns"] = columns
        if filters:
            arguments["filters"] = filters
        
        return self._send_request("tools/call", {
            "name": "getTableData",
            "arguments": arguments
        })

def main():
    if len(sys.argv) < 3:
        print("Usage: python3 test-mcp-interactive.py <db_path> <passphrase>")
        print("\nExample:")
        print("  python3 test-mcp-interactive.py /path/to/db.sqlite mypassphrase")
        sys.exit(1)
    
    db_path = sys.argv[1]
    passphrase = sys.argv[2]
    
    if not os.path.exists(db_path):
        print(f"Error: Database file not found: {db_path}")
        sys.exit(1)
    
    server_binary = "./build/install/mcp-sqlite/bin/mcp-sqlite"
    if not os.path.exists(server_binary):
        print(f"Error: Server binary not found: {server_binary}")
        print("Please build the server first: ./gradlew installDist")
        sys.exit(1)
    
    client = McpClient(db_path, passphrase, server_binary)
    
    print("=== MCP SQLite Server Test ===\n")
    
    # 1. Initialize
    print("1. Initializing connection...")
    init_response = client.initialize()
    if "error" in init_response:
        print(f"   Error: {init_response['error']}")
        sys.exit(1)
    print(f"   ✓ Connected to server: {init_response.get('result', {}).get('serverInfo', {}).get('name', 'unknown')}")
    
    # 2. List tools
    print("\n2. Listing available tools...")
    tools_response = client.list_tools()
    if "error" in tools_response:
        print(f"   Error: {tools_response['error']}")
    else:
        tools = tools_response.get("result", {}).get("tools", [])
        print(f"   ✓ Found {len(tools)} tools:")
        for tool in tools:
            print(f"     - {tool.get('name')}: {tool.get('description', '')[:60]}...")
    
    # 3. List tables
    print("\n3. Listing tables...")
    tables_response = client.list_tables(include_columns=True)
    if "error" in tables_response:
        print(f"   Error: {tables_response['error']}")
        if "data" in tables_response.get("error", {}):
            print(f"   Details: {tables_response['error']['data']}")
        sys.exit(1)
    
    result = tables_response.get("result", {})
    tables = result.get("tables", [])
    
    print(f"   ✓ Found {len(tables)} tables:\n")
    for table in tables:
        table_name = table.get("name", "unknown")
        columns = table.get("columns", [])
        print(f"   Table: {table_name}")
        if columns:
            print(f"     Columns ({len(columns)}):")
            for col in columns[:5]:  # Zeige nur erste 5 Spalten
                col_type = col.get("type", "?")
                pk = " [PK]" if col.get("primaryKey") else ""
                not_null = " [NOT NULL]" if col.get("notNull") else ""
                print(f"       - {col.get('name')} ({col_type}){pk}{not_null}")
            if len(columns) > 5:
                print(f"       ... and {len(columns) - 5} more columns")
        else:
            print("     (No column information available)")
        print()
    
    print("=== Test completed successfully ===")

if __name__ == "__main__":
    main()

