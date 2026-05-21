<!--
	Licensed to the Apache Software Foundation (ASF) under one
	or more contributor license agreements.  See the NOTICE file
	distributed with this work for additional information
	regarding copyright ownership.  The ASF licenses this file
	to you under the Apache License, Version 2.0 (the
	"License"); you may not use this file except in compliance
	with the License.  You may obtain a copy of the License at
	
	https://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an
	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, either express or implied.  See the License for the
	specific language governing permissions and limitations
	under the License.   
-->
# Cayenne MCP tools

The Cayenne MCP server (`cayenne-mcp-server` module) exposes Cayenne operations to AI agents over stdio.

**Availability: Cayenne 5.0+ only.** The MCP server is a new component shipped alongside CayenneModeler starting with the 5.0 release. There is no MCP server for Cayenne 4.x or earlier — skills that depend on these tools (`cayenne-cgen`, `cayenne-modeler`, `cayenne-reverse-engineer`) cannot be used against pre-5.0 projects.

Setup is documented in `cayenne-mcp-server/README.md` at the repo root. Quick form for Claude Code:

```bash
claude mcp add cayenne --scope user -- java -jar /path/to/cayenne-mcp-server-<VERSION>.jar
```

Verify the server is registered with `claude mcp list` — you should see an entry named `cayenne` (the MCP server alias from the `claude mcp add cayenne` command above) showing as connected. This is unrelated to the plugin name `apache-cayenne`.

## Tool: `cgen_run`

Runs Cayenne's class generator for one DataMap, using the `<cgen>` config block embedded in that DataMap.

**Arguments:**

| Name | Type | Required | Description |
|---|---|---|---|
| `projectPath` | string | yes | Absolute path to the top-level Cayenne project descriptor (`cayenne-*.xml`), **not** a DataMap file. |
| `dataMap` | string | yes | Name of the target DataMap as it appears in the `<map name="...">` element of the project descriptor. Not a file path. |

**Returns:** JSON object with fields:

```json
{
  "status": "ok" | "error",
  "summary": { "writtenCount": 3, "skippedCount": 12, "errorCount": 0 },
  "resolvedConfig": { "destDir": "...", "mode": "entity", ... },
  "writtenFiles": [{ "path": "...", "size": 1234 }],
  "skippedFiles": [{ "path": "...", "reason": "up-to-date" }],
  "errors": []
}
```

Surface the `summary` to the user verbatim. List the first few `writtenFiles` paths; full list is informational. If `errors` is non-empty, those are blocking — show them.

**Failure modes:**

- DataMap has no `<cgen>` block → error; user must add one (see `cgen-config.md` for the XML to insert).
- `projectPath` not readable or not a valid Cayenne project → validation error.
- `dataMap` doesn't match any `<map name="...">` → validation error.

**Source:** `cayenne-mcp-server/src/main/java/org/apache/cayenne/mcp/tools/cgen/CgenRunTool.java`.

## Tool: `open_project`

Launches CayenneModeler with a project file pre-loaded.

**Arguments:**

| Name | Type | Required | Description |
|---|---|---|---|
| `projectPath` | string | yes | Absolute path to the top-level Cayenne project descriptor (`cayenne-*.xml`). |

**Behavior:**

1. Locates the bundled CayenneModeler installation alongside the running MCP jar.
2. Spawns it with `--mcp-handshake <nonce>`.
3. Waits up to ~15 seconds for the Modeler to write a `java.util.prefs.Preferences` handshake confirming successful project load.
4. Returns once the handshake fires or times out.

**Returns:** JSON object with `status` (`ok` / `error`), and on error a `code` indicating the failure (e.g., `modeler_not_found`, `project_not_found`, `handshake_timeout`).

**When to call:** for reverse-engineering workflows (drives the user through the Modeler's import wizard), bulk visual editing, or when the user explicitly asks to open the Modeler.

**Don't call it** as a fallback for simple XML edits — direct edits via the `cayenne-modeling` skill are faster and don't require the user to switch context.

**Source:** `cayenne-mcp-server/src/main/java/org/apache/cayenne/mcp/tools/openproject/OpenProjectTool.java`.

## Detecting whether the server is connected

The MCP tools surface in this session under names `mcp__cayenne__cgen_run` and `mcp__cayenne__open_project`. If they are not in the available tools, the server is not registered.

When unavailable:

- Do **not** fall back to `mvn cayenne:cgen` or any Gradle equivalent — those build plugins are practically deprecated and are intentionally out of scope for this Claude Code plugin.
- Point the user at `cayenne-mcp-server/README.md` for setup and stop.
