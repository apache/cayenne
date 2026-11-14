---
paths:
  - "**/src/test/**"
description: generating the classes of test DataMaps with the locally built Cayenne MCP server
---

## Generating Test Entity Classes

Generate the Java classes of a test DataMap (e.g. `cayenne/src/test/resources/*.map.xml` → `org.apache.cayenne.testdo.*`)
with the `cgen_run` tool of the MCP server built from this repo. Don't hand-write them, don't use the Maven or Gradle
cgen goals, and don't use a registered `cayenne` MCP server, as it may be of an older Cayenne version.

```bash
# build the server, if cayenne-mcp-server/target has no jar
mvn clean package -pl cayenne-mcp-server -am -DskipTests
```

Call the tool over stdio with newline-delimited JSON-RPC. `projectPath` is the absolute path to the project descriptor
(`cayenne-*.xml`), `dataMap` is the map name in it:

```bash
cat > /tmp/cgen.jsonl <<'EOF'
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"cli","version":"1"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"cgen_run","arguments":{"projectPath":"<abs path>/cayenne-<name>.xml","dataMap":"<name>"}}}
EOF
(cat /tmp/cgen.jsonl; sleep 15) | java -jar cayenne-mcp-server/target/cayenne-mcp-server-*-SNAPSHOT.jar 2>/dev/null | tail -1
```

The result lists the files written. cgen only rewrites the files whose contents changed, and doesn't overwrite the
existing subclasses. Add the ASF license header to the new subclasses (the generated `auto/_*.java` superclasses need
none).

A new test DataMap also needs its project descriptor (`cayenne-<name>.xml`), a `<map>` entry in `cayenne-ALL.xml` (the
schema of all the test maps), and a constant in `CayenneProjects`.
