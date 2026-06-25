# Cayenne MCP Tools Protocol

Each tool returns its result as a single `TextContent` payload whose body is a JSON object matching the shapes below. Enum values are lowercase strings and serialize verbatim.

A result always carries:
- `status` — high-level outcome (per-tool values listed below)
- `validation` — per-step boolean checks; slots not reached are `null`
- `error` — `null` on success, otherwise `{ "code": <enum>, "message": <string> }`

## `cgen_run`

Runs Cayenne class generation for a named DataMap. If the DataMap has an embedded `<cgen>` block it is used; otherwise the tool synthesizes a default config (all entities and embeddables, `makePairs=true`, destination derived from the Maven layout) and generates anyway. A missing `<cgen>` block is not an error.

### Input

```json
{
  "projectPath": "string (absolute path to cayenne-*.xml)",
  "dataMap":     "string (DataMap name as it appears in the project descriptor)"
}
```

Both fields are required.

### Output

```json
{
  "status": "generated | up_to_date | validation_failed | error",
  "summary": {
    "filesConsidered": 0,
    "filesWritten":    0
  },
  "files": [
    {
      "path":         "string (absolute path to written file)",
      "kind":         "entity_super | entity_sub | embeddable_super | embeddable_sub | datamap",
      "sourceEntity": "string (ObjEntity or Embeddable name, may be null)"
    }
  ],
  "resolved": {
    "destDir": "string (absolutized cgen destDir; null when validation_failed)"
  },
  "warnings": ["string (captured log lines from org.apache.cayenne.gen)"],
  "validation": {
    "projectFound":      true,
    "dataMapFound":      true,
    "cgenConfigPresent": true,
    "destDirSpecified":  true,
    "destDirWritable":   true
  },
  "error": {
    "code":    "project_not_found | project_parse_failed | datamap_not_found | destdir_not_specified | destdir_not_writable | cgen_runtime_error",
    "message": "string"
  }
}
```

`status` semantics:
- `generated` — at least one file was written; `files` is non-empty
- `up_to_date` — all candidate files were already current; `files` is empty
- `validation_failed` — a pre-flight check failed; the failing slot in `validation` is `false`, later slots are `null`
- `error` — cgen started but threw mid-run; `files` lists what was written before the failure

## `dbimport_run`

Runs Cayenne reverse-engineering (dbimport) for a named DataMap. Reads JDBC connection info from the DBConnector that CayenneModeler stored in preferences for this DataMap. If the DataMap has a `<reverse-engineering>` block its filters are applied; otherwise the full database schema is imported. Rewrites the DataMap XML on disk with the merged schema.

### Input

```json
{
  "projectPath": "string (absolute path to cayenne-*.xml)",
  "dataMap":     "string (DataMap name as it appears in the project descriptor)"
}
```

Both fields are required. JDBC URL, driver, and credentials are not inputs — they come from CayenneModeler preferences. Schema filters come from the DataMap's `<reverse-engineering>` block if present.

### Output

```json
{
  "status": "imported | up_to_date | validation_failed | error",
  "summary": {
    "tokensConsidered":  0,
    "tokensApplied":     0,
    "entitiesAdded":     0,
    "entitiesRemoved":   0,
    "entitiesModified":  0,
    "relationshipsAdded": 0
  },
  "resolved": {
    "dataMapFile": "string (absolute path to the DataMap XML file)",
    "jdbcUrl":     "string",
    "jdbcDriver":  "string",
    "dbAdapter":   "string"
  },
  "warnings": ["string (captured WARN-level log lines from org.apache.cayenne.dbsync)"],
  "validation": {
    "projectFound":       true,
    "dataMapFound":       true,
    "dbConnectorPresent": true,
    "jdbcDriverLoadable": true,
    "jdbcConnectionOpened": true
  },
  "error": {
    "code":    "project_not_found | project_parse_failed | datamap_not_found | dbconnector_not_configured | jdbc_driver_not_loadable | jdbc_connection_failed | dbimport_runtime_error",
    "message": "string"
  }
}
```

`status` semantics:
- `imported` — dbimport ran and applied at least one merger token; DataMap XML was rewritten on disk
- `up_to_date` — dbimport ran but the DataMap was already in sync with the database; no file change
- `validation_failed` — a pre-flight check failed; the failing slot in `validation` is `false`, later slots are `null`
- `error` — validation passed but dbimport threw mid-run; `summary` reflects counters captured before the failure

`resolved` notes:
- Populated as soon as the connector is resolved (validation step 4), so it is present even on `jdbc_driver_not_loadable` and `jdbc_connection_failed` failures.
- `null` only when validation fails before the connector is read (steps 1–3).
- Never contains `userName` or `password`.

`summary` notes:
- `tokensConsidered` — merger tokens computed from the DB/DataMap diff.
- `tokensApplied` — tokens actually applied; equals `tokensConsidered` on success.
- Entity/relationship counters are rolled up from the applied token stream; `Read` the DataMap XML before and after for the full diff.

`validation` notes:
- `dbConnectorPresent` — the DataMap's preferences node holds a stored DBConnector (set by CayenneModeler when the user last ran "Reengineer Database Schema" for this DataMap).
- `jdbcDriverLoadable` — the driver class was loadable from the URLClassLoader built from CayenneModeler's *Preferences → Classpath* entries; if `false`, add the driver jar there and re-run.

## `open_project`

Launches CayenneModeler with a project file pre-loaded and blocks until the Modeler reports a startup handshake (15 s timeout).

### Input

```json
{
  "projectPath": "string (absolute path to cayenne-*.xml)"
}
```

Required.

### Output

```json
{
  "status": "launched | validation_failed | error",
  "resolved": {
    "distribution": "mac | windows | generic | source_tree",
    "modelerPath":  "string (path to the launcher binary)",
    "command":      ["string", "..."]
  },
  "validation": {
    "projectFound":   true,
    "mcpJarLocated":  true,
    "modelerFound":   true
  },
  "handshake": {
    "nonce":               "string (hex)",
    "modelerPid":          0,
    "startedAt":           "string (ISO-8601 instant reported by Modeler)",
    "resolvedProjectPath": "string (path the Modeler actually opened)",
    "waitMs":              0
  },
  "error": {
    "code":    "project_not_found | mcp_jar_location_unresolved | modeler_not_found | modeler_not_built | launch_failed | launch_exited_early | launch_not_confirmed",
    "message": "string"
  }
}
```

`status` semantics:
- `launched` — Modeler process started and confirmed the project load; `handshake` is populated
- `validation_failed` — a pre-flight check failed (project missing, jar location unresolved, no Modeler installation discovered); `resolved` and `handshake` are `null`
- `error` — Modeler was spawned but failed to confirm the load: it exited early, the handshake timed out, or `ProcessBuilder` itself failed; `resolved` is populated when available, `handshake` is `null`
