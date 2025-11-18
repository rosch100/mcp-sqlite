# MCP Server Verzeichnisse und Registries

Übersicht über verschiedene Verzeichnisse und Registries, in denen MCP-Server veröffentlicht und gefunden werden können.

## Offizielle Registries

### 1. MCP Registry (Offiziell)
- **URL**: https://registry.modelcontextprotocol.io
- **Typ**: Offizielle Registry des Model Context Protocol
- **Veröffentlichung**: Über `mcp-publisher` CLI Tool
- **Format**: `server.json` Schema
- **Authentifizierung**: GitHub OAuth
- **Befehl**: `mcp-publisher publish`

## Community-Verzeichnisse

### 2. MCPList.ai
- **URL**: https://www.mcplist.ai/
- **Typ**: Community-Verzeichnis
- **Veröffentlichung**: Über Submission-Formular
- **Zweck**: Liste und Entdeckung von MCP-Servern

### 3. MCP Index
- **URL**: https://mcpindex.net/
- **Typ**: Community-Verzeichnis
- **Veröffentlichung**: Über Submission-Prozess
- **Features**: Detaillierte Server-Informationen und Installationsanleitungen

### 4. MCPServ.club
- **URL**: https://www.mcpserv.club/
- **Typ**: Community-Verzeichnis
- **Veröffentlichung**: Über Submission-Guidelines (siehe `/docs`)
- **Zweck**: Sammlung von MCP-Servern

### 5. MCPHub
- **URL**: https://mcphub.com/
- **Typ**: Spezialisiertes Verzeichnis für MCP-Server
- **Zweck**: Zentrale Plattform für Veröffentlichung und Entdeckung

### 6. Directory MCP
- **URL**: https://directorymcp.com/
- **Typ**: Verzeichnis für MCP-Komponenten
- **Inhalt**: Server-Bibliotheken, Dokumentationen, Integrationsrichtlinien

## Container-Registries

### 7. GitHub Container Registry (ghcr.io)
- **URL**: https://github.com/orgs/USERNAME/packages/container/package-name
- **Typ**: Container Registry (OCI-kompatibel)
- **Verwendung**: Docker/Podman Images
- **Format**: `ghcr.io/username/repo:tag`
- **Vorteil**: Integriert mit GitHub, kostenlos für öffentliche Repositories

### 8. Docker Hub
- **URL**: https://hub.docker.com/
- **Typ**: Container Registry
- **Format**: `username/repo:tag`
- **Vorteil**: Weit verbreitet, große Community
- **Nachteil**: Rate Limits für kostenlose Accounts

### 9. Quay.io
- **URL**: https://quay.io/
- **Typ**: Container Registry (Red Hat)
- **Format**: `quay.io/username/repo:tag`
- **Features**: Security Scanning, Build Automation

## Package-Registries

### 10. NPM (Node Package Manager)
- **URL**: https://www.npmjs.com/
- **Typ**: Package Registry
- **Verwendung**: Für JavaScript/TypeScript MCP-Server
- **Format**: `@scope/package-name` oder `package-name`
- **Installation**: `npm install @scope/mcp-server-name`

### 11. PyPI (Python Package Index)
- **URL**: https://pypi.org/
- **Typ**: Package Registry
- **Verwendung**: Für Python-basierte MCP-Server
- **Format**: `package-name`
- **Installation**: `pip install package-name`

### 12. Maven Central / Maven Repository
- **URL**: https://mvnrepository.com/
- **Typ**: Package Registry
- **Verwendung**: Für Java-basierte MCP-Server
- **Format**: `groupId:artifactId:version`
- **Installation**: Über Gradle/Maven

## Hersteller-spezifische Verzeichnisse

### 13. Anthropic Connectors Directory
- **URL**: https://support.anthropic.com/en/articles/11596036-anthropic-connectors-directory-faq
- **Typ**: Offizielles Verzeichnis von Anthropic
- **Zweck**: Für Claude Desktop und andere Anthropic-Produkte
- **Anforderungen**: Spezifische Richtlinien und Standards

## Vergleich

| Registry | Typ | Format | Kosten | Beste für |
|----------|-----|--------|--------|-----------|
| MCP Registry | Offiziell | server.json | Kostenlos | Standard-Veröffentlichung |
| ghcr.io | Container | OCI | Kostenlos (öffentlich) | Docker Images |
| Docker Hub | Container | OCI | Kostenlos (begrenzt) | Weite Verbreitung |
| NPM | Package | npm | Kostenlos | JavaScript/TypeScript |
| PyPI | Package | pip | Kostenlos | Python |
| Maven | Package | Maven/Gradle | Kostenlos | Java |

## Empfehlungen

### Für diesen MCP-Server (Java-basiert):

1. **Primär**: MCP Registry (offiziell) - für Standard-Veröffentlichung
2. **Container**: ghcr.io - bereits konfiguriert, integriert mit GitHub
3. **Alternativ**: Docker Hub - für größere Reichweite
4. **Community**: MCPList.ai, MCP Index, MCPServ.club - für zusätzliche Sichtbarkeit

### Veröffentlichungs-Strategie:

1. ✅ **MCP Registry** (bereits vorbereitet mit `server.json`)
2. ✅ **ghcr.io** (bereits konfiguriert mit Dockerfile)
3. ⏳ **Community-Verzeichnisse** (manuelle Submission)
4. ⏳ **Docker Hub** (optional, falls gewünscht)

## Links

- [MCP Registry](https://registry.modelcontextprotocol.io)
- [MCPList.ai](https://www.mcplist.ai/)
- [MCP Index](https://mcpindex.net/)
- [MCPServ.club](https://www.mcpserv.club/)
- [MCPHub](https://mcphub.com/)
- [Directory MCP](https://directorymcp.com/)
- [Anthropic Connectors Directory](https://support.anthropic.com/en/articles/11596036-anthropic-connectors-directory-faq)

