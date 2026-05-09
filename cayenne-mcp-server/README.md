# Cayenne MCP Server

A self-contained MCP (Model Context Protocol) server that exposes CayenneModeler
operations to AI coding agents over stdio.

## Requirements

Java 21 or later must be available on the system PATH.

## Finding the JAR

The JAR location depends on how CayenneModeler was installed.
Downloads are available at https://cayenne.apache.org/download/.

### macOS (DMG)

```
<install-dir>/CayenneModeler.app/Contents/Resources/mcp/cayenne-mcp-server-<VERSION>.jar

# But typically, in:
/Applications/CayenneModeler.app/Contents/Resources/mcp/cayenne-mcp-server-<VERSION>.jar

```

### Windows (ZIP)

```
<install-dir>\bin\cayenne-mcp-server-<VERSION>.jar
```

### Linux / cross-platform (tar.gz)

```
<install-dir>/bin/cayenne-mcp-server-<VERSION>.jar
```

### Development build (own Cayenne source)

Build the module first:

```bash
mvn clean package -pl cayenne-mcp-server -am -DskipTests
```

The JAR is then at:

```
cayenne-mcp-server/target/cayenne-mcp-server-<VERSION>.jar
```

## Configuring AI clients

The server communicates over **stdio** and is launched on demand by the client.
Replace `/path/to/cayenne-mcp-server.jar` with the actual path from the section above.

### Claude Code

```bash
claude mcp add cayenne -- java -jar /path/to/cayenne-mcp-server-<VERSION>.jar
```

### Cursor

Edit `~/.cursor/mcp.json` (global) or `.cursor/mcp.json` in your project:

```json
{
  "mcpServers": {
    "cayenne": {
      "command": "java",
      "args": ["-jar", "/path/to/cayenne-mcp-server-<VERSION>.jar"]
    }
  }
}
```

### VS Code (GitHub Copilot)

Add to `.vscode/mcp.json` in your project:

```json
{
  "servers": {
    "cayenne": {
      "type": "stdio",
      "command": "java",
      "args": ["-jar", "/path/to/cayenne-mcp-server-<VERSION>.jar"]
    }
  }
}
```

## Verifying the setup

Once configured, ask your AI agent to call the `hello` tool. A response of
`"hello world"` confirms the server is running and reachable.
