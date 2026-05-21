---
name: cayenne-reverse-engineer
description: "Use this skill whenever the user wants to import database schema metadata into a Cayenne DataMap — full-schema sync from a live DB. Trigger on phrases like 'reverse engineer the database', 'import the schema', 'generate a DataMap from my DB', 'sync the model with the database', 'add the new tables from the DB', 'import the customer table', 'pick up the latest schema changes', 'create entities from these tables', or any request that involves reading database metadata to populate or update a DataMap. This is for *full schema* or *bulk table* import; one-off a-la-carte entity additions belong in the cayenne-modeling skill. The skill drives this through CayenneModeler's reverse-engineering wizard via the `mcp__cayenne__open_project` MCP tool — it does NOT use Maven `cdbimport` or any Gradle equivalent."
---

# cayenne-reverse-engineer

Import a database schema into a Cayenne DataMap by driving the CayenneModeler's reverse-engineering wizard through MCP. Cayenne 5.0 does not yet expose reverse engineering as a direct MCP tool, so the workflow launches the GUI and walks the user through the wizard.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate or create the project descriptor.
- `${CLAUDE_PLUGIN_ROOT}/references/dbimport-config.md` — the field semantics behind every wizard screen, so you can explain options in user terms.
- `${CLAUDE_PLUGIN_ROOT}/references/mcp-tools.md` — `open_project` tool reference and behavior when MCP is not connected.

## Step 1 — Confirm scope

Ask the user (one question, only if not obvious from the request):

- Are they reverse-engineering into a **new DataMap**, or **adding/updating** entities in an **existing DataMap**?

## Step 2 — Locate or create the project

Follow `project-layout.md` to find the existing `cayenne-*.xml`. If none exists, the user is starting from scratch:

- Either generate a minimal descriptor first (use `cayenne-modeling`'s patterns — namespace `http://cayenne.apache.org/schema/12/domain`, `project-version="12"`, one empty `<map name="..."/>` plus a sibling empty `*.map.xml`), or
- Tell the user to use **File → New Project** inside the Modeler once it's open.

The descriptor path needs to be **absolute** when passed to `open_project`.

## Step 3 — Launch the Modeler

Call the MCP tool:

```
mcp__cayenne__open_project({ "projectPath": "<absolute path to cayenne-*.xml>" })
```

If the tool is not available (server not registered), surface `cayenne-mcp-server/README.md` and stop. **Do not** suggest `mvn cayenne:cdbimport` or any Gradle equivalent — those build plugins are out of scope.

If `open_project` returns a non-`ok` status, surface the error code and message; common ones are `modeler_not_found`, `project_not_found`, `handshake_timeout`. Don't retry blindly — diagnose first.

## Step 4 — Walk the user through the wizard

The user now has the Modeler open. Give them the exact GUI sequence:

1. **DataMap → DB Import tab**
2. **Data Source** dialog:
   - **JDBC Driver** — e.g., `org.postgresql.Driver`
   - **DB URL** — full JDBC URL
   - **User Name / Password** — credentials
   - **Cayenne Adapter** (optional, autodetected for known DBs) — e.g., `org.apache.cayenne.dba.postgres.PostgresAdapter`
   - Click **Test Connection**, then **Continue**.
3. **Configure** screen — set table/column/procedure filters:
   - Include/exclude regex patterns under the relevant catalog/schema.
   - **Table Types** — usually leave at `TABLE`; add `VIEW` if the user wants views imported.
4. **Naming** screen:
   - **Naming Strategy** — usually the default (`DefaultObjectNameGenerator`).
   - **Strip from Table Names** — regex to strip a prefix like `^TBL_`.
   - **Default Package** — Java package for generated ObjEntity classes.
   - **Meaningful PK Tables** — regex (or `*`) for tables whose PK columns should be exposed as ObjAttributes. Leave empty unless the user needs PK visibility.
5. **Other Options** — toggles for `skipPrimaryKeyLoading`, `skipRelationshipsLoading`, `forceDataMapCatalog`, `forceDataMapSchema`, `useJava7Types`. See `dbimport-config.md` for what each does; defaults are usually right.
6. Click **Save** then **Run Import**. The Modeler runs the import and reports a diff (added/changed/removed entities).

Explain options as the user asks — `dbimport-config.md` has the semantics. When unsure, recommend the default.

## Step 5 — Confirm and follow up

Once the user reports the import is done and they've saved the project:

- Tell them to **save the project** in the Modeler (File → Save). The Modeler persists wizard settings as a `<dbImport>` block inside the DataMap for repeat runs.
- Hand off to `cayenne-cgen` to regenerate Java classes for the new/changed entities. Quote the DataMap name so the cgen skill can pass it to `cgen_run`.
- If the DB has columns that don't follow the user's preferred naming, recommend tweaking the naming strategy and re-running.

## Anti-patterns

- **Do not** suggest `mvn cayenne:cdbimport`, the Gradle `cdbimport` task, or hand-running the `cayenne-dbsync` Java APIs. The Modeler GUI is the only supported execution path here.
- **Do not** try to hand-write a DataMap from a DB schema description as a substitute for the wizard — the wizard handles JDBC types, PK detection, FK relationships, and naming consistently. Hand-rolling produces subtle mistakes.
- **Do not** enable `forceDataMapCatalog` / `forceDataMapSchema` defensively. They suppress legitimate DB metadata and cause hard-to-debug issues in multi-catalog setups.
- **Do not** offer reverse engineering without MCP — if the server isn't connected, point at `cayenne-mcp-server/README.md` and stop.
