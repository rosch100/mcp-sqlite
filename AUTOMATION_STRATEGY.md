# Automatisierungs-Strategie für MCP Server Verzeichnisse

Strategie für automatische Submission und Aktualisierung in verschiedenen MCP-Verzeichnissen.

## Übersicht

### Automatisierbare Verzeichnisse (mit API/CLI)

1. **MCP Registry (Offiziell)** ✅
   - **Tool**: `mcp-publisher` CLI
   - **Methode**: GitHub Actions mit GitHub OIDC
   - **Status**: Vollständig automatisierbar

2. **GitHub Container Registry (ghcr.io)** ✅
   - **Tool**: Docker Build & Push
   - **Methode**: GitHub Actions (bereits implementiert)
   - **Status**: Bereits automatisiert

### Teilweise automatisierbare Verzeichnisse (Webhook/API möglich)

3. **MCPList.ai** ⚠️
   - **Methode**: GitHub Webhook → API (falls verfügbar)
   - **Fallback**: Manuelle Submission
   - **Status**: Prüfung erforderlich

4. **MCP Index** ⚠️
   - **Methode**: GitHub Webhook → API (falls verfügbar)
   - **Fallback**: Manuelle Submission
   - **Status**: Prüfung erforderlich

### Manuelle Verzeichnisse (nur Web-Formular)

5. **MCPServ.club** ❌
6. **Directory MCP** ❌
7. **MCPHub** ❌

**Strategie**: Submission-Templates für manuelle Aktualisierung bei Releases

---

## Implementierungs-Strategie

### Stufe 1: Vollautomatisch (MCP Registry)

**GitHub Actions Workflow** für automatische Veröffentlichung bei Releases.

**Vorteile:**
- ✅ Keine manuelle Intervention nötig
- ✅ Konsistente Veröffentlichung
- ✅ Automatische Aktualisierung bei jedem Release

**Implementierung:**
- `.github/workflows/publish-mcp-registry.yml`

### Stufe 2: Webhook-basiert (Community-Verzeichnisse)

**GitHub Webhook** → Verzeichnis-API (falls verfügbar)

**Vorteile:**
- ✅ Automatische Benachrichtigung bei Releases
- ⚠️ Abhängig von API-Verfügbarkeit

**Implementierung:**
- Webhook-Konfiguration in GitHub
- API-Endpunkte der Verzeichnisse prüfen

### Stufe 3: Template-basiert (Manuelle Verzeichnisse)

**Release-basierte Erinnerung** mit vorbereiteten Templates

**Vorteile:**
- ✅ Einfach zu implementieren
- ✅ Templates sind vorbereitet
- ⚠️ Erfordert manuelle Aktualisierung

**Implementierung:**
- GitHub Actions Issue/Comment bei Release
- Erinnerung mit Links zu Submission-Templates

---

## GitHub Actions Workflow: MCP Registry

### Automatische Veröffentlichung

**Trigger:**
- Bei neuen Releases (Tags)
- Bei Änderungen an `server.json`

**Schritte:**
1. Installiere `mcp-publisher`
2. Authentifiziere mit GitHub OIDC
3. Validiere `server.json`
4. Publiziere zur MCP Registry
5. Verifiziere Veröffentlichung

---

## GitHub Actions Workflow: Release Notification

### Benachrichtigung für manuelle Submission

**Trigger:**
- Bei neuen Releases

**Schritte:**
1. Erstelle GitHub Issue/Comment
2. Liste alle Verzeichnisse auf
3. Zeige Links zu Submission-Templates
4. Checkliste für manuelle Submission

---

## Versionierungs-Strategie

### Automatische Version-Erkennung

- Version aus `build.gradle` lesen
- Version in `server.json` aktualisieren
- Version in `McpServer.java` aktualisieren
- CHANGELOG.md aktualisieren

### Release-Prozess

1. **Version bump** (manuell oder automatisch)
2. **Commit & Push**
3. **Tag erstellen** (`v0.2.5`)
4. **GitHub Release erstellen**
5. **Automatische Workflows**:
   - Docker Build & Push ✅
   - MCP Registry Publish ✅
   - Release Notification ✅

---

## Empfohlene Implementierung

### Phase 1: Sofort (Vollautomatisch)

1. ✅ MCP Registry Auto-Publish
2. ✅ Docker Image Auto-Build (bereits vorhanden)

### Phase 2: Kurzfristig (Webhook-Prüfung)

1. Prüfe API-Verfügbarkeit für Community-Verzeichnisse
2. Implementiere Webhook-basierte Updates (falls möglich)

### Phase 3: Langfristig (Manuelle Unterstützung)

1. Release-basierte Issue-Erstellung
2. Automatische Checkliste für manuelle Submission
3. Template-Generierung bei Releases

---

## Monitoring & Verifizierung

### Automatische Verifizierung

- Prüfe MCP Registry nach Veröffentlichung
- Validiere `server.json` vor Submission
- Teste Docker Image nach Build

### Manuelle Verifizierung

- Prüfe Community-Verzeichnisse nach Submission
- Teste Links und Konfigurationen
- Aktualisiere bei Bedarf

---

## Best Practices

1. **Versionierung**: Semantische Versionierung (SemVer)
2. **Tagging**: Immer `v` Präfix (`v0.2.4`)
3. **Changelog**: Aktualisiere CHANGELOG.md bei jedem Release
4. **server.json**: Halte Metadaten aktuell
5. **Dokumentation**: Aktualisiere README bei Änderungen

---

## Troubleshooting

### MCP Registry Publish fehlgeschlagen

- Prüfe GitHub OIDC Token
- Validiere `server.json` Schema
- Prüfe Logs in GitHub Actions

### Docker Build fehlgeschlagen

- Prüfe Dockerfile
- Validiere Multi-Platform Support
- Prüfe ghcr.io Permissions

### Manuelle Submission fehlgeschlagen

- Verwende Submission-Templates
- Prüfe alle Informationen
- Kontaktiere Verzeichnis-Administratoren

