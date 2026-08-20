---
name: cayenne-modeling
description: "Use this skill whenever the user wants to edit, inspect, or extend the Cayenne ORM model in a project — adding or modifying entities, attributes, relationships, embeddables, named queries, stored procedures, or DataNodes. Trigger on phrases like 'add an ObjEntity', 'add a DbEntity', 'add a relationship', 'expose this column as an attribute', 'create a new DataMap', 'add a named query', 'create an embeddable', 'add a stored procedure', 'change the attribute type', 'mark this column as nullable', 'rename this entity', or any mention of a Cayenne `*.map.xml` or `cayenne-*.xml` file. Also trigger when the user references modeling concepts (ObjEntity, DbEntity, ObjAttribute, DbAttribute, ObjRelationship, DbRelationship, Embeddable, dbEntityName, deleteRule, db-attribute-path, db-relationship-path, defaultPackage) in the context of a Cayenne-using app. This is the *primary* skill for a-la-carte ORM model manipulation — direct XML edits, not the Modeler GUI."
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

Edit Cayenne DataMap (`*.map.xml`) and project descriptor (`cayenne-*.xml`) files directly. This is the **default path** for granular model changes — adding one entity, one relationship, one query. Use the Modeler GUI (`cayenne-modeler` skill) only for inherently visual work like reverse engineering.

## Required reading

Read these before making any edit — they describe the XML formats this skill operates on:

- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate the project descriptor and DataMaps in the user's repo.
- `${CLAUDE_PLUGIN_ROOT}/references/datamap-schema.md` — every element shape for `*.map.xml`.
- `${CLAUDE_PLUGIN_ROOT}/references/project-descriptor-schema.md` — `cayenne-*.xml` shape.

## Step 1 — Locate the right file

Follow `project-layout.md`. If the user has multiple Cayenne projects and the request is ambiguous, ask which one. Cache the answer for the rest of the session.

Decide whether the change belongs in a DataMap or the project descriptor (table at the end of `project-layout.md`):

- Entity/relationship/query/embeddable/procedure → DataMap (`*.map.xml`).
- DataMap list, DataNode (DB connection) → project descriptor (`cayenne-*.xml`).

## Step 2 — Make the edit

Apply the change following the schema in `datamap-schema.md` or `project-descriptor-schema.md`.

### Critical rules

1. **Element order matters.** The DataMap schema requires this order inside `<data-map>`: `<property>`, `<procedure>`, `<embeddable>`, `<db-entity>`, `<obj-entity>`, `<db-relationship>`, `<obj-relationship>`, `<query>`, `<cgen>`, `<dbImport>`. Insert at the right place — don't append blindly.

2. **Cross-link consistently.**
   - An `<obj-entity>` references a `<db-entity>` by `dbEntityName="..."`. Make sure that DbEntity exists.
   - An `<obj-attribute>` references a DbAttribute by `db-attribute-path="COLUMN_NAME"`. Make sure that column exists on the DbEntity (or on a related one if using a dotted path through a relationship).
   - An `<obj-relationship>` requires a backing `<db-relationship>` (or chain of them) via `db-relationship-path`. Never add an ObjRelationship without the DB-layer counterpart.
   - Most FKs need **two** `<db-relationship>` entries, one per direction. They are not auto-derived.

3. **Use the right `type` on attributes.**
   - `db-attribute type` — JDBC type names: `VARCHAR`, `INTEGER`, `BIGINT`, `DATE`, `TIMESTAMP`, `BOOLEAN`, `NUMERIC`, `BLOB`, `CLOB`, `VARBINARY`, etc. VARCHAR/CHAR/VARBINARY also need `length`. NUMERIC/DECIMAL also need `scale`.
   - `obj-attribute type` — Java FQN: `java.lang.String`, `java.lang.Integer`, `java.util.Date`, `java.math.BigDecimal`, `byte[]`, etc. Use wrapper types (`Integer`, not `int`) for nullable columns.

4. **PK handling.** PK columns get `isPrimaryKey="true" isMandatory="true"` on the DbAttribute. They are normally **not** mirrored as ObjAttributes — Cayenne handles them implicitly. Map a PK as an ObjAttribute only if the user wants a "meaningful PK" (visible on the Java side).

5. **Preserve formatting.** Match the indentation and quote style of the file (the standard Cayenne style uses tabs and double quotes, but follow what's actually in the file you're editing).

6. **Changing `defaultPackage` on a DataMap.** If the user asks to change the DataMap-level `defaultPackage` property, ask whether entity-level packages should be updated too. Otherwise, every existing `obj-entity` will keep its old package. Ask: "Do you also want me to update the `className` values on individual `<obj-entity>` elements that currently use the old package?" Update them only if the user confirms.

## Step 3 — Validate the edit conceptually

After writing the XML, mentally walk through:

- Does every `obj-entity.dbEntityName` resolve to an existing `<db-entity>`?
- Does every `obj-attribute.db-attribute-path` resolve to a column on the right DbEntity?
- Does every `obj-relationship.db-relationship-path` resolve to a chain of existing `<db-relationship>` entries?
- Are PK and FK columns marked `isMandatory="true"` where the DB enforces NOT NULL?

If anything fails, fix it before reporting done.

## Step 4 — Offer the right next step

- **If you modified entities and the DataMap has a `<cgen>` block:** suggest invoking the `cayenne-cgen` skill to regenerate Java classes. Mention which entities are affected.
- **If the user added a new entity and there's no Java class yet:** same — recommend `cayenne-cgen`.
- **If the user is asking about a full DB sync** (importing many tables, syncing with a changed schema): hand off to `cayenne-db-import`. Do not try to script this via XML edits.
- **If the request is about making names descriptive/consistent** (cleaning up reverse-engineered names, fixing `Gametype`/`team1`-style names, normalizing entity/attribute/relationship names across the model): hand off to `cayenne-model-naming` — it knows the naming conventions and the rename cross-reference checklist.
- **If the change is structurally messy** (bulk renaming relationships, visual graph rework): suggest the `cayenne-modeler` skill. Otherwise do not.

## Modeler coexistence

CayenneModeler **watches the project XML files** and automatically detects on-disk changes. When you edit a `*.map.xml` file directly, the Modeler will prompt the user to reload the project the next time they interact with it. There is no risk of the Modeler silently overwriting your edits. Do **not** warn the user that the Modeler may overwrite changes or tell them to close the Modeler before editing — that is incorrect and unnecessary.

## Anti-patterns

- **Don't hand-edit `_<Entity>.java` superclass files.** They are regenerated by cgen and your changes will be overwritten. Edit the user `<Entity>.java` subclass instead.
- **Don't add an `obj-relationship` without a `db-relationship`.** Cayenne will validate-fail at runtime.
- **Don't use primitive Java types (`int`, `long`, `boolean`) as `obj-attribute type` for nullable columns.** Primitives can't represent NULL. Use wrappers.
- **Don't reorder existing elements.** The schema requires the order documented above; reordering existing elements may also create noisy diffs.
- **Don't run cgen yourself.** That's `cayenne-cgen`'s job — invoke that skill instead of calling the MCP tool directly here.
- **Don't suggest `mvn cayenne:cdbimport` or any Maven/Gradle plugin goal.** Those are explicitly out of scope for this plugin.
