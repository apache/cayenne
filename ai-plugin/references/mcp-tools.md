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

**Availability: Cayenne 5.0+ only.** The MCP server first shipped with Cayenne 5.0. There is no MCP server for Cayenne 4.x or earlier — skills that depend on these tools (`cayenne-cgen`, `cayenne-modeler`, `cayenne-db-import`, `cayenne-upgrade`) cannot be used against pre-5.0 projects. The `upgrade_project` tool is available starting with the 6.0 MCP server.

**Setup: none.** This plugin comes with the server. It declares an MCP server named `cayenne`, started by `mcp/CayenneMcpLauncher.java` in the plugin. On the first start, the launcher takes the `org.apache.cayenne:cayenne-mcp-server` jar of the plugin's Cayenne version from the local Maven repository, or else downloads it from Maven Central (or the Apache snapshots repository) and caches it in the plugin's data directory, where the server also caches the CayenneModeler jar it downloads for `open_project`. The only requirement is Java 21 or newer on the `PATH`. CayenneModeler is only needed for `open_project`.

## Tool: `cgen_run`

Runs Cayenne's class generator for one DataMap. Uses the first `<cgen>` config block embedded in that DataMap if present; if the DataMap has no `<cgen>` block, the tool synthesizes a default config (all entities and embeddables, `makePairs=true`, destination derived from the standard Maven layout) and generates anyway — a missing block is **not** an error.

**Arguments:**

| Name | Type | Required | Description |
|---|---|---|---|
| `projectPath` | string | yes | Absolute path to the top-level Cayenne project descriptor (`cayenne-*.xml`), **not** a DataMap file. |
| `dataMap` | string | yes | Name of the target DataMap as it appears in the `<map name="...">` element of the project descriptor. Not a file path. |

**Returns:** JSON object with fields:

```json
{
  "status": "generated" | "up_to_date" | "validation_failed" | "error",
  "summary": { "filesConsidered": 15, "filesWritten": 3 },
  "files": [{ "path": "...", "kind": "entity_super", "sourceEntity": "Artist" }],
  "resolved": { "destDir": "..." },
  "warnings": [],
  "validation": { "projectFound": true, "dataMapFound": true, "cgenConfigPresent": true,
                  "destDirSpecified": true, "destDirWritable": true },
  "error": null
}
```

Surface the `summary` to the user verbatim. List the first few `files` paths; full list is informational.

When the DataMap had no `<cgen>` block, the tool generated from a synthesized default config. The default destination is derived from the Maven layout (`src/main/resources` → `src/main/java`); for non-Maven layouts it falls back to the DataMap's own directory, so check `resolved.destDir` and confirm with the user if it looks wrong. Offer to persist a `<cgen>` block (via `cayenne-modeling` or the Modeler) if they want to customize destination, templates, or filtering.

**Failure modes:**

- `projectPath` not readable or not a valid Cayenne project → validation error.
- `dataMap` doesn't match any `<map name="...">` → validation error.
- A `<cgen>` block without `<destDir>` is not an error: the classes go to the DataMap's own directory, reported in `resolved.destDir`. (`destdir_not_specified` is only returned for a DataMap without a file location, and a DataMap loaded from `projectPath` always has one.)

**Source:** `cayenne-mcp-server/src/main/java/org/apache/cayenne/mcp/tools/cgen/CgenRunTool.java`.

## Tool: `dbimport_run`

Runs Cayenne reverse-engineering (dbimport) for one DataMap. Reads the JDBC connection from the DBConnector that CayenneModeler stored in preferences when the user last ran the reverse-engineering dialog for this DataMap. If the DataMap has a `<dbImport>` block its filters are applied; otherwise the full database schema is imported. Rewrites the DataMap XML on disk with the merged schema.

**Arguments:**

| Name | Type | Required | Description |
|---|---|---|---|
| `projectPath` | string | yes | Absolute path to the top-level Cayenne project descriptor (`cayenne-*.xml`), **not** a DataMap file. |
| `dataMap` | string | yes | Name of the target DataMap as it appears in the `<map name="...">` element of the project descriptor. Not a file path. |

JDBC URL, driver, and credentials are **not** arguments — they come from CayenneModeler preferences. Schema filters come from the DataMap's `<dbImport>` block if present.

**Returns:** JSON object with fields:

```json
{
  "status": "imported | up_to_date | validation_failed | error",
  "summary": {
    "tokensConsidered": 5,
    "tokensApplied": 5,
    "entitiesAdded": 3,
    "entitiesRemoved": 0,
    "entitiesModified": 2,
    "relationshipsAdded": 4
  },
  "resolved": {
    "dataMapFile": "/path/to/datamap.map.xml",
    "jdbcUrl": "jdbc:postgresql://localhost/mydb",
    "jdbcDriver": "org.postgresql.Driver",
    "dbAdapter": "org.apache.cayenne.dba.postgres.PostgresAdapter"
  },
  "warnings": [],
  "validation": {
    "projectFound": true,
    "dataMapFound": true,
    "dbConnectorPresent": true,
    "jdbcDriverLoadable": true,
    "jdbcConnectionOpened": true
  },
  "error": null
}
```

Surface the `summary` to the user. If `status` is `imported`, report entity/relationship counts. If `up_to_date`, nothing changed. Show `warnings` if non-empty; they are `WARN`-level log lines captured from the dbsync engine.

**Failure modes:**

| Error code | Meaning | Remediation |
|---|---|---|
| `project_not_found` | `projectPath` not readable | Verify path. |
| `project_parse_failed` | Not a valid Cayenne project descriptor | Confirm the file is a `cayenne-*.xml`. |
| `datamap_not_found` | No matching DataMap name; error message lists available names | Correct the `dataMap` argument. |
| `dbconnector_not_configured` | No DBConnector stored in preferences for this DataMap | Open the Modeler via `open_project`, run the Modeler's reverse-engineering dialog once to save the connection, then re-run `dbimport_run`. |
| `jdbc_driver_not_loadable` | Driver class not on CayenneModeler's classpath | Open CayenneModeler → Preferences → Classpath, add the JDBC driver jar, save, then re-run. |
| `jdbc_connection_failed` | Connection could not be opened | Check JDBC URL, credentials, and network access. |
| `dbimport_runtime_error` | Import started but threw mid-run | Check the error message; `summary` shows counts captured before the failure. |

**Source:** `cayenne-mcp-server/src/main/java/org/apache/cayenne/mcp/tools/dbimport/DbImportRunTool.java`.

## Tool: `open_project`

Launches CayenneModeler with a project file pre-loaded.

**Arguments:**

| Name | Type | Required | Description |
|---|---|---|---|
| `projectPath` | string | yes | Absolute path to the top-level Cayenne project descriptor (`cayenne-*.xml`). |
**Behavior:**

1. Picks the CayenneModeler of the server's own version: the Modeler the MCP jar is bundled with (when the server was set up from a Modeler download), or else the runnable `org.apache.cayenne:cayenne-modeler` jar from the local Maven repository or downloaded from Maven Central (Apache snapshots for a snapshot version) and cached. The first download takes a few seconds. So a project is never opened, and possibly saved, by a Modeler of another version.
2. Spawns it with `--mcp-handshake <nonce>`.
3. Waits up to ~15 seconds for the Modeler to write a `java.util.prefs.Preferences` handshake confirming successful project load.
4. Returns once the handshake fires or times out.

**Returns:** JSON object with `status` (`launched` / `validation_failed` / `error`), `resolved` (the Modeler used: `modelerPath`, and `source` — `bundled` or `downloaded`), and on failure an `error` with a `code` and a `message`.

**Failure modes:**

| Error code | Meaning | Remediation |
|---|---|---|
| `modeler_download_failed` | The Modeler jar couldn't be downloaded | Relay the message: usually no network, or a proxy blocking `repo1.maven.org`. Once fixed, call again. |
| `modeler_not_found` | The server isn't bundled with a Modeler, and is a development build whose Modeler can't be downloaded | Relay the message. |
| `project_not_found` | `projectPath` not readable | Verify path. |
| `launch_failed` / `launch_exited_early` | The Modeler process failed to start or exited early | Relay the message. |
| `launch_not_confirmed` | No handshake within the timeout | The Modeler may still be loading; tell the user to check the GUI. Don't retry. |

**When to call:** for reverse-engineering workflows (drives the user through the Modeler's import dialog), bulk visual editing, or when the user explicitly asks to open the Modeler. Not for upgrading old projects — that's `upgrade_project`.

**Don't call it** as a fallback for simple XML edits — direct edits via the `cayenne-modeling` skill are faster and don't require the user to switch context.

**Source:** `cayenne-mcp-server/src/main/java/org/apache/cayenne/mcp/tools/openproject/OpenProjectTool.java`.

## Tool: `upgrade_project`

Upgrades a project saved by an older Cayenne version to the current format: rewrites the project descriptor and every DataMap XML in place, and deletes the files the project no longer needs (e.g. old diagram layouts). Runs the same upgrade as CayenneModeler does when it opens an old project, without the GUI.

**Arguments:**

| Name | Type | Required | Description |
|---|---|---|---|
| `projectPath` | string | yes | Absolute path to the top-level Cayenne project descriptor (`cayenne-*.xml`), **not** a DataMap file. |

**Returns:** JSON object with fields:

```json
{
  "status": "upgraded | up_to_date | error",
  "fromVersion": "12",
  "toVersion": "15",
  "changes": [{
    "message": "DataNode 'main' was removed from the project along with its connection parameters. Starting with project version 13, DataNodes are configured in code via CayenneRuntimeBuilder.addDataNode(..)",
    "details": ["adapter: org.apache.cayenne.dba.postgres.PostgresAdapter", "DataMap: main", "JDBC driver: org.postgresql.Driver", "JDBC URL: jdbc:postgresql://localhost/app", "user name: app"],
    "affectsRuntime": true
  }],
  "error": null
}
```

- `changes` lists every change the upgrade made. A change with `affectsRuntime: true` removed or altered a part of the project that has no automatic replacement — the application code must change to keep working. Its `details` hold the facts the upgraded project no longer has (connection settings, query text, callback methods, ...), one `label: value` line each. Passwords are never included.

**Failure modes** (`status: "error"`):

| Error code | Meaning | Remediation |
|---|---|---|
| `project_not_found` | `projectPath` not readable | Verify path. |
| `project_parse_failed` | The project version can't be read | Confirm the file is a `cayenne-*.xml`. |
| `intermediate_upgrade_needed` | The project is too old to upgrade directly | The message names the older Cayenne release whose Modeler must upgrade it first. |
| `downgrade_needed` | The project was saved by a newer Cayenne | Use a newer CayenneModeler / MCP server. |
| `upgrade_failed` | The upgrade threw | Relay the message. |

**Source:** `cayenne-mcp-server/src/main/java/org/apache/cayenne/mcp/tools/upgrade/UpgradeProjectTool.java`.

## Detecting whether the server is connected

The tools come from this plugin's `cayenne` server. Claude Code prefixes their names with the server and the plugin
name, e.g. `mcp__plugin_apache-cayenne-6_cayenne__cgen_run`. The prefix depends on the name the plugin was installed
under, so look the tools up by the tool name (`cgen_run`, `dbimport_run`, `open_project`, `upgrade_project`).

A Cayenne MCP server registered by hand (`claude mcp add cayenne ...`, tools named like `mcp__cayenne__cgen_run`) is a
different server, often of another Cayenne version (e.g. the 5.0 one, used by the `apache-cayenne-5` plugin). Don't
use its tools for this plugin's workflows.

## When the MCP server is unavailable

If this plugin's tools are not available, the server failed to start. Tell the user to check it with `/mcp`, which
shows the error. The usual causes:

- **No Java 21 or newer on the `PATH`.** Install it, then reconnect the server in `/mcp`.
- **No network on the first start.** The server jar is downloaded once, from `repo1.maven.org` (or
  `repository.apache.org` for snapshots). Behind a proxy, the system proxy settings are used. Once the network is
  fixed, reconnect the server in `/mcp`.

Then:

- Do **not** fall back to `mvn cayenne:cgen` or any Gradle equivalent — those build plugins are practically deprecated and are intentionally out of scope for this Claude Code plugin.
- Do **not** use the tools of a hand-registered `cayenne` server instead.
- Stop.
