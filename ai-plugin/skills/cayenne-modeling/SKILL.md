---
name: cayenne-modeling
description: "Use this skill whenever the user wants to edit, inspect, or extend the Cayenne ORM model in a project — adding or modifying entities, attributes, relationships, embeddables, named queries, stored procedures, or DataNodes. Trigger on phrases like 'add an ObjEntity', 'add a DbEntity', 'add a relationship', 'expose this column as an attribute', 'create a new DataMap', 'add a named query', 'create an embeddable', 'add a stored procedure', 'change the attribute type', 'mark this column as nullable', 'rename this entity', or any mention of a Cayenne `*.map.xml` or `cayenne-*.xml` file. Also trigger when the user references modeling concepts (ObjEntity, DbEntity, ObjAttribute, DbAttribute, ObjRelationship, DbRelationship, Embeddable, dbEntityName, deleteRule, dbAttributePath, dbRelationshipPath, defaultPackage) in the context of a Cayenne-using app. This is the *primary* skill for a-la-carte ORM model manipulation — direct XML edits, not the Modeler GUI. IMPORTANT — DB layer ownership: the DB layer (DbEntity, DbAttribute, DbRelationship) is owned by DB import and is NOT hand-edited by this skill, unless the user has explicitly said they do not use DB import for this project. Otherwise a DB-layer request, even for a single table or column, is routed to `cayenne-db-import`; this skill hand-edits the Object layer, queries, embeddables and config blocks."
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
# cayenne-modeling

Edit Cayenne DataMap (`*.map.xml`) and project descriptor (`cayenne-*.xml`) files directly. This is the **default path** for granular model changes — adding one entity, one relationship, one query. Use the Modeler GUI (`cayenne-modeler` skill) only for inherently visual work like bulk relationship layout.

## Who owns what — check this before any edit

A DataMap has two halves with different owners. The owner is decided by the **layer** being changed, not by the size of the change.

| Part of the model | Owner | How to change it |
|---|---|---|
| **DB layer** — `<dbEntity>`, `<dbAttribute>` (type, length, scale, `mandatory`, PK flags), `<dbRelationship>` and its joins | The database, via DB import | Change the DB schema, then run `cayenne-db-import`. **Do not hand-edit.** One settings-based carve-out for `<dbRelationship>` — see "DbRelationships follow the import settings". |
| **Object layer** — `<objEntity>`, `<objAttribute>`, `<objRelationship>`, inheritance, callbacks | The user | Hand-edit here. |
| Everything else — queries, `<embeddable>`, `<procedure>`, `<cgen>`, `<dbImport>` filters, the project descriptor | The user | Hand-edit here. |

**The only exception:** hand-edit the DB layer when the user has **explicitly** said they do not use DB import for this project (e.g. "we don't reverse engineer", "the model is the source of truth and the schema is generated from it"). Then this skill owns both layers.

When a request needs a DB-layer change, settle whether the project uses DB import, in this order:

1. **The user already said** — in the request, earlier in the session, in project instructions, or in your persistent memory for this project. Follow that.
2. **The target DataMap has a `<dbImport>` block** — the project uses DB import. Don't ask.
3. **Otherwise it is unclear — ask once**: "Do you use DB import (reverse engineering) to keep this DataMap in sync with the database, or do you maintain the DB layer by hand?" Then save the answer to your persistent memory for this project, if you have one (per DataMap, if the project has several and the answers differ), so the question is not repeated in later sessions. Without persistent memory, remember it for the rest of the session.

- Do **not** infer the exception. A missing `<dbImport>` block is "unclear", not an opt-out — the block is optional and import works without one. The same goes for a missing DBConnector, an MCP server that isn't connected, or a table that doesn't exist in the DB yet.
- If the project uses DB import, do not make the DB-layer change. Say the DB layer comes from DB import, and hand off to `cayenne-db-import` (after the user has applied the schema change to the DB, if it isn't there yet). This holds for a single table, column, nullability flag or FK just as for a bulk import.
- If the user later says otherwise (they started or stopped using DB import), follow the new answer and update the saved memory.
- Why: `dbimport_run` merges the DB into the model and applies every difference without prompting. Hand-written DB-layer elements that don't match the DB are overwritten or dropped on the next import, and hand-written types, PK flags and joins tend to be subtly wrong.
- A request that spans both layers ("add a `Customer` entity for the new `CUSTOMER` table") is split: import brings in the DB layer, usually along with a default ObjEntity; then adjust the Object layer here.

### DbRelationships follow the import settings

Whether DB import owns `<dbRelationship>` elements depends on `<skipRelationshipsLoading>` in the DataMap's `<dbImport>` block. Read it before adding, removing or changing a DbRelationship in a project that uses DB import:

| `<skipRelationshipsLoading>` | Who owns DbRelationships | What to do |
|---|---|---|
| `true` | The user. Import neither reads FKs nor removes relationships from the model. | Hand-edit `<dbRelationship>` elements here, both directions. `<dbEntity>` / `<dbAttribute>` still come from import. |
| `false`, absent, or no `<dbImport>` block | DB import. Every FK becomes a relationship; a relationship with no matching FK is **dropped** by the next import. | Don't hand-edit. A relationship backed by an FK comes from `cayenne-db-import`. For one with no FK in the DB, see below. |

When the user wants a relationship that has no FK in the DB and the setting is not `true`, don't write it and don't change the setting on your own. Explain that the next import would drop it, and offer the two ways out:

1. Add the FK constraint in the DB, then run `cayenne-db-import`.
2. Set `<skipRelationshipsLoading>true</skipRelationshipsLoading>` in `<dbImport>` (creating the block if needed) and add the relationship by hand. Spell out the cost: from then on import stops bringing in **any** relationships, so FKs added to the DB later must also be mapped by hand.

Make the change only after the user picks one.

- DbRelationship **names** are not DB metadata. Import matches relationships by their joins, not by name. Renaming them (and keeping the ObjRelationship name in sync) is allowed and belongs to `cayenne-model-naming`.

## Required reading

Read these before making any edit — they describe the XML formats this skill operates on:

- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate the project descriptor and DataMaps in the user's repo.
- `${CLAUDE_PLUGIN_ROOT}/references/datamap-schema.md` — every element shape for `*.map.xml`.
- `${CLAUDE_PLUGIN_ROOT}/references/project-descriptor-schema.md` — `cayenne-*.xml` shape.

## Step 1 — Locate the right file

Follow `project-layout.md`. If the user has multiple Cayenne projects and the request is ambiguous, ask which one. Cache the answer for the rest of the session.

Check the `project-version` attribute in the descriptor before editing. If it is lower than the version in `project-descriptor-schema.md`, the project was saved by an older Cayenne version: stop and hand off to the `cayenne-modeler` skill so the user can upgrade it in CayenneModeler. Don't edit an old-format file and don't migrate the format by hand — the references here describe the current format only.

Decide whether the change belongs in a DataMap or the project descriptor (table at the end of `project-layout.md`):

- Entity/relationship/query/embeddable/procedure → DataMap (`*.map.xml`).
- DataMap list, DataNode (DB connection) → project descriptor (`cayenne-*.xml`).

## Step 2 — Make the edit

Apply the change following the schema in `datamap-schema.md` or `project-descriptor-schema.md`.

### Critical rules

The parts of these rules about writing `<dbEntity>`, `<dbAttribute>` and `<dbRelationship>` elements apply only when the user has opted out of DB import — or, for `<dbRelationship>` alone, when `<skipRelationshipsLoading>` is `true` (see "Who owns what"). Otherwise treat the DB layer as read-only input that the Object layer must reference correctly.

1. **Element order matters.** The DataMap schema requires this order inside `<dataMap>`: `<procedure>`, `<embeddable>`, `<dbEntity>`, `<objEntity>`, `<dbRelationship>`, `<objRelationship>`, the queries (`<objectQuery>`, `<sqlQuery>`, `<procedureQuery>`), `<cgen>`, `<dbImport>`. Insert at the right place — don't append blindly.

2. **Cross-link consistently.**
   - An `<objEntity>` references a `<dbEntity>` by `dbEntityName="..."`. Make sure that DbEntity exists.
   - An `<objAttribute>` references a DbAttribute by `dbAttributePath="COLUMN_NAME"`. Make sure that column exists on the DbEntity (or on a related one if using a dotted path through a relationship).
   - An `<objRelationship>` requires a backing `<dbRelationship>` (or chain of them) via `dbRelationshipPath`. Never add an ObjRelationship without the DB-layer counterpart. If the DbRelationship is missing and DB import owns relationships (`<skipRelationshipsLoading>` is not `true`), it has to come from an FK in the DB via `cayenne-db-import` — don't write it by hand.
   - When hand-editing the DB layer: most FKs need **two** `<dbRelationship>` entries, one per direction. They are not auto-derived.

3. **Use the right `type` on attributes.**
   - `dbAttribute type` — JDBC type names: `VARCHAR`, `INTEGER`, `BIGINT`, `DATE`, `TIMESTAMP`, `BOOLEAN`, `NUMERIC`, `BLOB`, `CLOB`, `VARBINARY`, etc. VARCHAR/CHAR/VARBINARY also need `length`. NUMERIC/DECIMAL also need `scale`.
   - `objAttribute type` — Java FQN: `java.lang.String`, `java.lang.Integer`, `java.util.Date`, `java.math.BigDecimal`, `byte[]`, etc. Use wrapper types (`Integer`, not `int`) for nullable columns.

4. **PK handling.** PK columns get `primaryKey="true" mandatory="true"` on the DbAttribute. They are normally **not** mirrored as ObjAttributes — Cayenne handles them implicitly. Map a PK as an ObjAttribute only if the user wants a "meaningful PK" (visible on the Java side).

5. **Preserve formatting.** Match the indentation and quote style of the file (the standard Cayenne style uses tabs and double quotes, but follow what's actually in the file you're editing).

6. **Changing `defaultPackage` on a DataMap.** If the user asks to change the DataMap-level `defaultPackage` attribute, ask whether entity-level packages should be updated too. Otherwise, every existing `objEntity` will keep its old package. Ask: "Do you also want me to update the `className` values on individual `<objEntity>` elements that currently use the old package?" Update them only if the user confirms.

## Step 3 — Validate the edit conceptually

After writing the XML, mentally walk through:

- Does every `objEntity.dbEntityName` resolve to an existing `<dbEntity>`?
- Does every `objAttribute.dbAttributePath` resolve to a column on the right DbEntity?
- Does every `objRelationship.dbRelationshipPath` resolve to a chain of existing `<dbRelationship>` entries?
- If you hand-edited the DB layer (user opted out of DB import): are PK and FK columns marked `mandatory="true"` where the DB enforces NOT NULL?
- If the user uses DB import: did the edit leave every `<dbEntity>` and `<dbAttribute>` untouched — and every `<dbRelationship>` too, unless `<skipRelationshipsLoading>` is `true`?

If anything fails, fix it before reporting done.

## Step 4 — Offer the right next step

- **If you modified entities and the DataMap has a `<cgen>` block:** suggest invoking the `cayenne-cgen` skill to regenerate Java classes. Mention which entities are affected.
- **If the user added a new entity and there's no Java class yet:** same — recommend `cayenne-cgen`.
- **If the request needs a DB-layer change of any size** (a new table, a new or changed column, an FK, syncing with a changed schema) and the user has not opted out of DB import: hand off to `cayenne-db-import`, or to `cayenne-full-db-sync` when they want names and classes updated too. Do not script it via XML edits.
- **If the request is about making names descriptive/consistent** (cleaning up reverse-engineered names, fixing `Gametype`/`team1`-style names, normalizing entity/attribute/relationship names across the model): hand off to `cayenne-model-naming` — it knows the naming conventions and the rename cross-reference checklist.
- **If the project is in an older format** (`project-version` below the current one): hand off to `cayenne-modeler` to upgrade it before any edit.
- **If the change is structurally messy** (bulk renaming relationships, visual graph rework): suggest the `cayenne-modeler` skill. Otherwise do not.

## Modeler coexistence

CayenneModeler **watches the project XML files** and automatically detects on-disk changes. When you edit a `*.map.xml` file directly, the Modeler will prompt the user to reload the project the next time they interact with it. There is no risk of the Modeler silently overwriting your edits. Do **not** warn the user that the Modeler may overwrite changes or tell them to close the Modeler before editing — that is incorrect and unnecessary.

## Anti-patterns

- **Don't upgrade an older project by hand** — no bumping `project-version` or rewriting namespaces and elements yourself. The Modeler runs the real upgrade handlers; use `cayenne-modeler`.
- **Don't hand-edit the DB layer unless the user explicitly said they don't use DB import.** No `<dbEntity>`, `<dbAttribute>` or `<dbRelationship>` additions, removals or attribute changes — not even "just one column". The single carve-out is `<dbRelationship>` when `<skipRelationshipsLoading>` is `true`.
- **Don't flip `<skipRelationshipsLoading>` to `true` on your own** to make room for a hand-written relationship. It turns off relationship import for the whole DataMap — the user decides. Don't guess the opt-out from the state of the project — when there is no `<dbImport>` block and no earlier answer, ask once and save the answer. See "Who owns what".
- **Don't hand-edit `_<Entity>.java` superclass files.** They are regenerated by cgen and your changes will be overwritten. Edit the user `<Entity>.java` subclass instead.
- **Don't add an `objRelationship` without a `dbRelationship`.** Cayenne will validate-fail at runtime.
- **Don't use primitive Java types (`int`, `long`, `boolean`) as `objAttribute type` for nullable columns.** Primitives can't represent NULL. Use wrappers.
- **Don't reorder existing elements.** The schema requires the order documented above; reordering existing elements may also create noisy diffs.
- **Don't run cgen yourself.** That's `cayenne-cgen`'s job — invoke that skill instead of calling the MCP tool directly here.
- **Don't suggest `mvn cayenne:cdbimport` or any Maven/Gradle plugin goal.** Those are explicitly out of scope for this plugin.
