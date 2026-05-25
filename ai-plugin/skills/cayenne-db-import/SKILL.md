---
name: cayenne-db-import
description: "Use this skill whenever the user wants to import database schema metadata into a Cayenne DataMap — full-schema sync from a live DB. Trigger on phrases like 'reverse engineer the database', 'import the schema', 'generate a DataMap from my DB', 'sync the model with the database', 'add the new tables from the DB', 'import the customer table', 'pick up the latest schema changes', 'create entities from these tables', or any request that involves reading database metadata to populate or update a DataMap. This is for *full schema* or *bulk table* import; one-off a-la-carte entity additions belong in the cayenne-modeling skill. The skill runs reverse engineering directly via the `mcp__cayenne__dbimport_run` MCP tool when a DBConnector is already configured; otherwise it opens the CayenneModeler GUI via `mcp__cayenne__open_project` to configure the connection first."
---

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
# cayenne-db-import

Import a database schema into a Cayenne DataMap using the `mcp__cayenne__dbimport_run` MCP tool. If a DBConnector is already stored in preferences for the DataMap the import runs directly without any GUI interaction. When the connection has not been configured yet, the workflow launches CayenneModeler and walks the user through the dialog once to save the connection, then re-runs `dbimport_run`.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate or create the project descriptor.
- `${CLAUDE_PLUGIN_ROOT}/references/dbimport-config.md` — field semantics for every dialog screen, so you can explain options in user terms.
- `${CLAUDE_PLUGIN_ROOT}/references/mcp-tools.md` — `dbimport_run` and `open_project` tool references and failure modes.

## Step 1 — Confirm scope

Ask the user (one question, only if not obvious from the request):

- Are they reverse-engineering into a **new DataMap**, or **adding/updating** entities in an **existing DataMap**?

## Step 2 — Locate or create the project

Follow `project-layout.md` to find the existing `cayenne-*.xml`. If none exists, the user is starting from scratch:

- Either generate a minimal descriptor first (use `cayenne-modeling`'s patterns — namespace `http://cayenne.apache.org/schema/12/domain`, `project-version="12"`, one empty `<map name="..."/>` plus a sibling empty `*.map.xml`), or
- Tell the user to use **File → New Project** inside the Modeler once it's open.

The descriptor path needs to be **absolute** when passed to the MCP tools.

## Step 3 — Run `dbimport_run`

Call the MCP tool:

```
mcp__cayenne__dbimport_run({ "projectPath": "<absolute path to cayenne-*.xml>", "dataMap": "<DataMap name>" })
```

If the tool is not available (server not registered), surface `cayenne-mcp-server/README.md` and stop. **Do not** suggest `mvn cayenne:cdbimport` or any Gradle equivalent — those build plugins are out of scope.

### Interpreting the result

- **`imported`** — success. Surface the `summary` counters (entities added/removed/modified, relationships added) to the user.
- **`up_to_date`** — the DataMap already matches the database. Tell the user nothing changed.
- **`error`** — import started but failed mid-run. Show the `error.message` and the partial `summary`.
- **`validation_failed`** — a pre-flight check failed. Use the `error.code` to guide next steps (see below).

### Validation failures and their remediation

| Code | What failed | What to do |
|---|---|---|
| `project_not_found` | Project path not readable | Verify path. |
| `project_parse_failed` | File is not a valid Cayenne descriptor | Confirm the file is a `cayenne-*.xml`. |
| `datamap_not_found` | DataMap name not in project | The error message lists available names; use one. |
| `dbconnector_not_configured` | No connection saved for this DataMap | → Go to **Step 4** to configure via the Modeler dialog. |
| `jdbc_driver_not_loadable` | Driver jar not on the Modeler classpath | Open CayenneModeler → Preferences → Classpath, add the driver jar, save, then re-run. |
| `jdbc_connection_failed` | Connection could not be opened | Check JDBC URL, credentials, and network access. |

## Step 4 — Configure the connection (first-time only)

This step is only needed when `dbimport_run` returns `dbconnector_not_configured`.

### 4a — Launch the Modeler

```
mcp__cayenne__open_project({ "projectPath": "<absolute path to cayenne-*.xml>" })
```

If `open_project` returns a non-`ok` status, surface the error code; common ones are `modeler_not_found`, `project_not_found`, `handshake_timeout`. Don't retry blindly — diagnose first.

### 4b — Walk the user through the dialog

The user now has the Modeler open. Give them the exact GUI sequence:

1. **DataMap → DB Import tab**
2. **Data Source** dialog:
   - **JDBC Driver** — e.g., `org.postgresql.Driver`
   - **DB URL** — full JDBC URL
   - **User Name / Password** — credentials
   - **Cayenne Adapter** (optional, autodetected for known DBs)
   - Click **Test Connection**, then **Continue**.
3. **Configure** screen — set table/column/procedure filters:
   - Include/exclude regex patterns under the relevant catalog/schema.
   - **Table Types** — usually `TABLE`; add `VIEW` if needed.
4. **Naming** screen:
   - **Naming Strategy**, **Strip from Table Names**, **Default Package**, **Meaningful PK Tables**.
   - See `dbimport-config.md` for what each does.
5. **Other Options** — toggles for `skipPrimaryKeyLoading`, `skipRelationshipsLoading`, etc.
6. Click **Save** (not **Run Import** — the goal here is to save the connection, not run the import through the GUI).

After the user saves, the DBConnector is written to CayenneModeler preferences.

### 4c — Re-run `dbimport_run`

Go back to **Step 3** and call `dbimport_run` again. This time it should find the saved connection and proceed.

## Step 5 — Confirm and follow up

Once the import succeeds:

- Tell the user to **save the project** in the Modeler if it is open (File → Save). The dialog settings persist as a `<dbImport>` block inside the DataMap for repeat runs.
- Hand off to `cayenne-cgen` to regenerate Java classes for the new/changed entities. Quote the DataMap name so the cgen skill can pass it to `cgen_run`.
- If the DB has columns that don't follow the user's preferred naming, recommend tweaking the naming strategy and re-running.

## Anti-patterns

- **Do not** suggest `mvn cayenne:cdbimport`, the Gradle `cdbimport` task, or hand-running the `cayenne-dbsync` Java APIs. `dbimport_run` is the only supported execution path.
- **Do not** try to hand-write a DataMap from a DB schema description as a substitute — the import handles JDBC types, PK detection, FK relationships, and naming consistently. Hand-rolling produces subtle mistakes.
- **Do not** enable `forceDataMapCatalog` / `forceDataMapSchema` defensively. They suppress legitimate DB metadata and cause hard-to-debug issues in multi-catalog setups.
- **Do not** offer reverse engineering without MCP — if the server isn't connected, point at `cayenne-mcp-server/README.md` and stop.
- **Do not** run the import through the Modeler GUI dialog when `dbimport_run` is available — the GUI path is only for the first-time connection setup.
