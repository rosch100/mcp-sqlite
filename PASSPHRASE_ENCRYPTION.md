# Passphrase-Verschlüsselung - Schnellanleitung

Diese Anleitung zeigt Ihnen, wie Sie Ihre Passphrase für den lokalen MCP SQLite Server verschlüsseln können.

## macOS Keychain (Empfohlen für macOS)

Auf macOS können Sie den Verschlüsselungsschlüssel sicher in der Keychain speichern:

### Schritt 1: Projekt bauen
```bash
./gradlew build
```

### Schritt 2: Schlüssel generieren und in Keychain speichern
```bash
./store-key-in-keychain.sh --generate
```

Das war's! Der Schlüssel wird automatisch aus der Keychain geladen, wenn keine Umgebungsvariable gesetzt ist.

### Schritt 3: Passphrase verschlüsseln
```bash
./encrypt-passphrase.sh "ihre-plain-passphrase"
```

**Vorteile der Keychain:**
- ✅ Schlüssel wird sicher verschlüsselt gespeichert
- ✅ Keine Umgebungsvariablen nötig
- ✅ Automatische Entsperrung mit macOS-Benutzerpasswort
- ✅ Funktioniert systemweit für alle Anwendungen

---

## Umgebungsvariable (Alternative/Cross-Platform)

## Schritt 1: Projekt bauen

Stellen Sie sicher, dass das Projekt gebaut wurde:

```bash
./gradlew build
```

## Schritt 2: Verschlüsselungsschlüssel generieren

Generieren Sie einen neuen Verschlüsselungsschlüssel:

```bash
./generate-key.sh
```

Das Skript gibt Ihnen einen Schlüssel aus. Kopieren Sie diesen.

## Schritt 3: Schlüssel als Umgebungsvariable setzen

Setzen Sie den Schlüssel als Umgebungsvariable:

```bash
export MCP_SQLITE_ENCRYPTION_KEY="<ihr-generierter-schlüssel>"
```

**Wichtig:** Für dauerhafte Verwendung fügen Sie diese Zeile zu Ihrer Shell-Konfiguration hinzu:
- Bash: `~/.bashrc` oder `~/.bash_profile`
- Zsh: `~/.zshrc`
- Fish: `~/.config/fish/config.fish`

```bash
echo 'export MCP_SQLITE_ENCRYPTION_KEY="<ihr-schlüssel>"' >> ~/.zshrc
```

## Schritt 4: Passphrase verschlüsseln

Verschlüsseln Sie Ihre Passphrase:

```bash
./encrypt-passphrase.sh "ihre-plain-passphrase"
```

Das Skript gibt Ihnen die verschlüsselte Passphrase aus (beginnt mit `encrypted:`).

## Schritt 5: In Konfiguration verwenden

Verwenden Sie die verschlüsselte Passphrase in Ihrer MCP-Konfiguration (z.B. `~/.cursor/mcp.json`):

```json
{
  "mcpServers": {
    "encrypted-sqlite": {
      "command": "/path/to/mcp-sqlite/build/install/mcp-sqlite/bin/mcp-sqlite",
      "args": [
        "--args",
        "{\"dbPath\":\"/path/to/your/database.sqlite\",\"passphrase\":\"encrypted:<verschlüsselte-passphrase>\"}"
      ],
      "env": {
        "MCP_SQLITE_ENCRYPTION_KEY": "<ihr-verschlüsselungsschlüssel>"
      }
    }
  }
}
```

## Alternative: Manuelle Verwendung

Falls Sie die Skripte nicht verwenden möchten:

### Schlüssel generieren:
```bash
java -cp build/libs/mcp-sqlite-0.1.0.jar com.example.mcp.sqlite.util.GenerateKey
```

### Passphrase verschlüsseln:
```bash
export MCP_SQLITE_ENCRYPTION_KEY="<ihr-schlüssel>"
java -cp build/libs/mcp-sqlite-0.1.0.jar com.example.mcp.sqlite.util.EncryptPassphrase "ihre-passphrase"
```

## Sicherheitshinweise

- ⚠️ **WICHTIG:** Der Verschlüsselungsschlüssel (`MCP_SQLITE_ENCRYPTION_KEY`) **MUSS** gesetzt sein, sonst funktioniert die Entschlüsselung nicht
- 🔒 Bewahren Sie den Schlüssel sicher auf und committen Sie ihn niemals in Version Control
- 🔑 Verwenden Sie verschiedene Schlüssel für verschiedene Umgebungen (Entwicklung, Produktion)
- 🔄 Rotieren Sie den Schlüssel regelmäßig und verschlüsseln Sie alle Passphrasen neu

## Beispiel-Workflow

```bash
# 1. Projekt bauen
./gradlew build

# 2. Schlüssel generieren und setzen
KEY=$(./generate-key.sh | grep -A 1 "Verschlüsselungsschlüssel:" | tail -1)
export MCP_SQLITE_ENCRYPTION_KEY="$KEY"

# 3. Passphrase verschlüsseln
./encrypt-passphrase.sh "meine-geheime-passphrase"

# 4. Ausgabe kopieren und in mcp.json verwenden
```

## Troubleshooting

### Fehler: "MCP_SQLITE_ENCRYPTION_KEY ist nicht gesetzt"
- Stellen Sie sicher, dass die Umgebungsvariable gesetzt ist: `echo $MCP_SQLITE_ENCRYPTION_KEY`
- Setzen Sie sie mit: `export MCP_SQLITE_ENCRYPTION_KEY="<schlüssel>"`

### Fehler: "Der Schlüssel ist zu schwach"
- Verwenden Sie immer `generate-key.sh` oder `GenerateKey` Tool zum Generieren
- Verwenden Sie niemals vorhersagbare Schlüssel

### Fehler: "Ungültiges Base64-Format"
- Stellen Sie sicher, dass der Schlüssel korrekt kopiert wurde (keine Leerzeichen, vollständig)

