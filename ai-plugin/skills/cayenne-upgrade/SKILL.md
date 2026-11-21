---
name: cayenne-upgrade
description: "Use this skill when the user wants to upgrade a Cayenne project saved by an older Cayenne version to the current format, or to upgrade their application to a newer Cayenne release. Trigger on phrases like 'upgrade the project', 'upgrade my Cayenne mapping', 'migrate the model to Cayenne 6', 'upgrade to Cayenne 6', 'bump the Cayenne version', 'the project was created with an older Cayenne', or when another skill finds a project descriptor or DataMap in an older format. The skill upgrades the project XML via the `upgrade_project` MCP tool (no CayenneModeler needed), then makes the code changes the upgrade requires, guided by the changes the tool reports and by UPGRADE.md. Do NOT upgrade the XML by hand."
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
# cayenne-upgrade

Upgrade a Cayenne project to the current format with the `upgrade_project` MCP tool, and bring the
application code in line with it.

An upgrade can be **destructive**: some parts of an old project have no equivalent in the current format, and the
upgrade removes or alters them (e.g. DataNodes, EJBQL queries, callbacks mapped on entities). The tool reports each
such change with `"affectsRuntime": true` and the information the upgraded project no longer has in `details`.
Recreating those parts in code is this skill's job — an upgrade isn't done until every such change is addressed.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/mcp-tools.md` — the `upgrade_project` tool reference and failure modes.
- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate the project descriptor.
- `${CLAUDE_PLUGIN_ROOT}/references/runtime-api.md` — for recreating DataNodes in code.
- `${CLAUDE_PLUGIN_ROOT}/references/query-api.md` — for replacing removed queries.

## Step 1 — Resolve the project path

The tool needs an **absolute path** to the top-level project descriptor (`cayenne-*.xml`). Use `project-layout.md`
to find it. If there are several descriptors, upgrade each, one at a time.

If the `upgrade_project` tool is not available, follow "When the MCP server is unavailable" in `mcp-tools.md` and
stop. Do not upgrade the XML by hand.

## Step 2 — Make the upgrade reversible

The upgrade rewrites the project and DataMap files in place and deletes the files the project no longer needs. Git
is the undo.

- Run `git status` on the project files. If they have uncommitted changes, ask the user to commit or stash them first,
  so the upgrade is a clean, reviewable diff that can be reverted. If the project is not under version control, ask
  the user to back it up first.
- If the project is open in CayenneModeler, ask the user to close it without saving, or the Modeler may later
  overwrite the upgraded files.

## Step 3 — Upgrade

```
upgrade_project({ "projectPath": "<absolute path>" })
```

| `status` / `error.code` | Action |
|---|---|
| `upgraded` | Continue to Step 4. |
| `up_to_date` | The XML is current. Skip to Step 6 if the user is upgrading the Cayenne version. |
| `intermediate_upgrade_needed` | The project is too old. Relay the message, which names the older Cayenne release whose Modeler must upgrade it first, and stop. |
| `downgrade_needed` | The project was saved by a newer Cayenne than the MCP server. The user needs a newer CayenneModeler / MCP server. Stop. |
| `project_not_found`, `project_parse_failed`, `upgrade_failed` | Relay the message and stop. |

Keep the returned `changes` — they are the work list for Step 5, and the upgraded files no longer have the
information in `details`.

## Step 4 — Show the user what changed

- `fromVersion` → `toVersion`.
- **Every** change with `affectsRuntime: true`, with its `message` and `details`. These are the parts that were
  removed or altered and need code changes.
- The number of other changes (informational, no action needed).

If any change affects the runtime, ask the user whether to proceed with the code changes, or to revert the upgrade
(`git restore` / `git checkout` of the project files, which also brings back the deleted ones).

## Step 5 — Address the changes that affect the runtime

Go through every change with `affectsRuntime: true`. The `message` says what happened and what to do; the `details`
hold the facts. Find the affected application code and fix it. Common cases:

- **DataNode removed** — DataNodes are configured in code. Find where the app builds its `CayenneRuntime` and add the
  node with `defaultDataNode(..)` (one node) or `addDataNode(.., "map1", ...)` (several nodes, linked to the DataMaps
  from the `DataMap:` details lines), per `runtime-api.md`. Take the JDBC driver, URL, user name, pool sizes and
  adapter from `details`. A password is never reported: take it from the app's existing configuration, or ask the
  user — never hardcode a secret. A JNDI `DataSource factory` means the DataSource comes from JNDI under the name in
  `DataSource factory parameters`. If the app already configures the DataSource in code (e.g. via Bootique or
  Spring), only confirm the DataMap linkage.
- **Query removed** (EJBQL, no root entity, unsupported type) — `details` hold the query definition. Search the code
  for the query name (e.g. `MappedSelect.query("name")`) and replace each use with an equivalent `ObjectSelect` or
  `SQLQuery`, per `query-api.md`, or recreate it as a named query in the DataMap via the `cayenne-modeling` skill.
- **SQL query fetch limit / offset removed** — make it a part of the query SQL in the DataMap (`cayenne-modeling`),
  or set it on the query at the call site.
- **Callbacks removed** — `details` list `event: method` pairs, e.g. `postAdd: onPostAdd`. In the entity's Persistent
  class, implement the matching interface from `org.apache.cayenne.lifecycle` (`PostAddAware`, `PrePersistAware`,
  `PostPersistAware`, `PreUpdateAware`, `PostUpdateAware`, `PreRemoveAware`, `PostRemoveAware`, `PostLoadAware`),
  calling the existing method from the interface method (e.g. `onPostAdd()`), or renaming it.
- **Anything else** — follow the `message`. When unsure what the replacement is, look it up in `UPGRADE.md` (Step 6).

If a change can't be addressed (e.g. the code that used a removed part is not in this repository), tell the user
explicitly. Never drop a runtime-affecting change silently.

## Step 6 — Code changes beyond the project XML

A Cayenne release changes API as well. When the user is upgrading the Cayenne version, read the upgrade guide for
the sections between the old and the new version:

- https://github.com/apache/cayenne/blob/master/UPGRADE.md (6.0 and 5.0)
- https://github.com/apache/cayenne/blob/master/UPGRADE-4.2-and-older.md (4.2 and older)

Apply the notes that are relevant to the code in this repository (and update the Cayenne version in the build).

## Step 7 — Regenerate and build

1. Regenerate the entity classes via the `cayenne-cgen` skill — the templates and the model may have changed.
2. Compile the project (`mvn compile` / `gradle compileJava`) and fix what's broken, using `UPGRADE.md`.
3. Report to the user: the version upgrade, each runtime-affecting change and how it was addressed, anything left
   for them to do.

## Anti-patterns

- **Do not** edit the project XML to upgrade it — no bumping versions, rewriting namespaces or converting elements by
  hand. The tool runs the same upgrade handlers as CayenneModeler.
- **Do not** upgrade when the project files have uncommitted changes — the upgrade can't be cleanly undone
  otherwise.
- **Do not** treat the upgrade as done after Step 3. The runtime-affecting changes are lost from the project; if they
  are not recreated in code, the app breaks or silently behaves differently.
- **Do not** open CayenneModeler to upgrade — the MCP tool does the same without leaving the agent.
