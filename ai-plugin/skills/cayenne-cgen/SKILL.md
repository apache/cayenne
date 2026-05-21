---
name: cayenne-cgen
description: "Use this skill whenever the user wants to (re)generate Cayenne entity Java classes from a DataMap. Trigger on phrases like 'generate Java classes', 'regenerate entities', 'run cgen', 'create the entity classes', 'why is the Artist class missing fields', 'where did the `_Abstract*` classes come from', 'sync the entity classes with the model', or any request to materialize Java from the DataMap. Also trigger as a follow-up after modeling changes (someone added an entity, attribute, or relationship and now the Java side is stale). This skill exclusively uses the `mcp__cayenne__cgen_run` MCP tool — it does NOT use `mvn cayenne:cgen` or the Gradle cgen task."
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
# cayenne-cgen

Run Cayenne's class generator on a DataMap via the `mcp__cayenne__cgen_run` MCP tool. Reads the embedded `<cgen>` block in the DataMap to determine destination, mode, templates, etc.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/mcp-tools.md` — `cgen_run` tool reference (arguments, return shape, failure modes).
- `${CLAUDE_PLUGIN_ROOT}/references/cgen-config.md` — every `<cgen>` field; needed when the user has to add or tweak the config block.
- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate the project descriptor.

## Step 1 — Resolve project and DataMap

The MCP tool needs two arguments:

- `projectPath` — **absolute path** to the top-level project descriptor (`cayenne-*.xml`). **Not** a DataMap file.
- `dataMap` — the **name** as it appears in `<map name="...">` in the descriptor. **Not** a file path.

Locate the descriptor via `project-layout.md`. If multiple descriptors exist, ask which one. Open the descriptor to extract the DataMap names from `<map>` elements.

If the user named the DataMap directly (e.g. "regenerate classes for the customers DataMap"), use that. If they said "regenerate everything" and there are multiple DataMaps, run `cgen_run` once per DataMap in sequence (the tool generates per-DataMap).

## Step 2 — Verify `<cgen>` block exists

Before calling the tool, check the target DataMap for a `<cgen xmlns="http://cayenne.apache.org/schema/12/cgen">` block. If missing:

1. Don't call `cgen_run` blindly — it will return an error.
2. Either:
   - **Add a minimal block** by delegating to the `cayenne-modeling` skill. Use the starter config from `cgen-config.md`:
     ```xml
     <cgen xmlns="http://cayenne.apache.org/schema/12/cgen">
         <destDir>../java</destDir>
         <mode>entity</mode>
         <makePairs>true</makePairs>
         <usePkgPath>true</usePkgPath>
         <createPKProperties>true</createPKProperties>
     </cgen>
     ```
     Confirm `destDir` with the user — it's relative to the project XML directory and decides where generated `.java` files land.
   - Or, if the user prefers the GUI, suggest the `cayenne-modeler` skill and tell them to configure the DataMap's "Class Generation" tab.

## Step 3 — Call `cgen_run`

```
mcp__cayenne__cgen_run({
  "projectPath": "<absolute path to cayenne-*.xml>",
  "dataMap": "<map name from the descriptor>"
})
```

If the tool is not available (MCP server not registered), surface `cayenne-mcp-server/README.md` and stop. **Do not** suggest `mvn cayenne:cgen` or the Gradle cgen task.

## Step 4 — Surface the result

The tool returns structured JSON. Report:

- `summary.writtenCount`, `summary.skippedCount`, `summary.errorCount` verbatim — these are the headline.
- The first few entries in `writtenFiles` (relative paths). Full list is informational; offer to dump it if the user asks.
- Any entries in `errors` — these are blocking. Read the messages and explain in user terms (a missing entity class name, a bad template path, an invalid `<destDir>`, etc.).

If `writtenCount` is 0 and `skippedCount` covers everything, say so — it means everything is already up-to-date and no work was needed.

## Step 5 — Next steps

- If new `_<Entity>.java` superclass files were generated, gently remind the user not to edit those — they will be overwritten next run. User code goes in the matching `<Entity>.java` subclass.
- If the user just ran reverse engineering, this skill is the natural follow-up. Cross-link back to `cayenne-modeling` if they need to tweak names/types before regenerating.

## Anti-patterns

- **Do not** call `cgen_run` without verifying the `<cgen>` block exists — the tool returns an error and confuses the user.
- **Do not** suggest Maven (`mvn cayenne:cgen`) or Gradle (`cayenneCgen`) goals when MCP is unavailable. Those build plugins are out of scope. Point at MCP setup instead.
- **Do not** edit `_<Entity>.java` files. Generated superclasses. Edit `<Entity>.java` subclasses.
- **Do not** confuse `projectPath` and `dataMap` arguments. `projectPath` is a file system path to `cayenne-*.xml`. `dataMap` is a logical name (e.g. `mydb`), not a path to `mydb.map.xml`.
