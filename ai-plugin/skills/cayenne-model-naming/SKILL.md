---
name: cayenne-model-naming
description: "Use this skill to clean up Object-layer names in a Cayenne DataMap — ObjEntity, ObjAttribute, and ObjRelationship names, plus DbRelationship names (the first-class unit of relationship cleanup — every FK has one whether or not an ObjRelationship was generated; the ObjRelationship name is synced to it when one exists) — so they read as descriptive, consistent Java. Trigger on phrases like 'clean up the model names', 'fix the entity names', 'these names look ugly', 'make the names descriptive', 'normalize the ObjEntity/attribute/relationship names', 'why is this relationship called team1', 'rename entities to be consistent', 'the import produced Gametype instead of GameType'. Invoke it on an explicit user request, or as a manual follow-up after a `cayenne-db-import` to polish the just-imported additions — it is never triggered automatically. IMPORTANT: this is a LIGHT polish pass — CayenneModeler's reverse-engineering already produces good names for the common case; only improve the specific things its deterministic algorithm cannot (run-together names with no separators like `gametype`, meaningless numbered names like `team1` from multiple relationships between two tables, and a common entity prefix that leaks into relationship names like `aaOrders`). Do NOT rewrite names that are already correct. This is Obj-layer naming polish; for structural model edits use `cayenne-modeling`, and for regenerating classes afterward use `cayenne-cgen`."
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
# cayenne-model-naming

Polish the Object-layer names in a Cayenne DataMap so they look like idiomatic Java. This runs on top
of the names CayenneModeler's reverse engineering already produced — which are **good for the common
case**. Your job is a surgical touch-up of the few names its deterministic algorithm can't get right,
**not** a rewrite. If a name is already a clean camelCase/PascalCase transliteration of its DB element
with the right cardinality, leave it exactly as it is.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/model-naming-conventions.md` — the deterministic baseline (what's
  already correct and must be left alone) and the specific gaps where your judgment adds value.
- `${CLAUDE_PLUGIN_ROOT}/references/model-naming-rename-safety.md` — the cross-reference checklist for
  every rename, so a rename never leaves a dangling reference.
- `${CLAUDE_PLUGIN_ROOT}/references/datamap-schema.md` — element shapes for `*.map.xml`.
- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — locate the project descriptor and DataMap.

## Step 1 — Determine scope: changed elements vs the entire model

There are two modes, and picking the right one is the most important decision in this skill:

- **Changed-only** (the default, e.g. when the user runs this after a `cayenne-db-import`) — only
  inspect the names on newly-added or just-modified elements. Everything else in the model was
  presumably reviewed already; don't churn it.
- **Entire model** (rare) — inspect every Obj element. Use this **only** when the user explicitly asks
  for it ("clean up the whole model", "normalize all the names", "go over every entity").

`dbimport_run` does **not** report which entities changed (it reports change counts though, so use it as an extra
hint), so detect the changed set with git. Run these from the repo containing the DataMap file:

```bash
# Is the DataMap under version control at all?
git -C <repo-dir> rev-parse --is-inside-work-tree

# What did the import change (unstaged), and did anything get staged already?
git -C <repo-dir> diff -- <path/to/datamap>.map.xml
git -C <repo-dir> diff --cached -- <path/to/datamap>.map.xml
```

From the diff, collect the added/modified `<obj-entity>`, `<obj-attribute>`, `<obj-relationship>`, and
`<db-relationship>` lines — that added/changed set is your scope. Notes:

- If `git diff` is empty but the file is tracked, there's nothing new to clean — say so and stop
  (unless the user asked for an entire-model pass).
- If the file is **untracked / brand-new** (no prior version in `HEAD`), every element is effectively
  "new" — treat it as an entire-model pass, but tell the user that's why.
- **Fallback:** if it's not a git repository, or git isn't available, you can't scope by diff — fall
  back to the entire model and tell the user you couldn't narrow it down.

## Step 2 — Find the names worth changing (and only those)

For each in-scope element, compare against `model-naming-conventions.md`. **Most names will already be
correct — leave them.** Flag only the cases the deterministic generator can't handle:

1. **Run-together names with no separators** — `gametype` → `GameType`, `dateofbirth` → `dateOfBirth`.
   Split on a real word boundary you're confident about; never invent one.
2. **Numbered collision names** (`projects` / `projects1`, `people` / `people1`) from more than one
   relationship between the same two tables. The generator resolves the common cases itself: to-one
   names are FK-based even without an `_ID` suffix (`HOME_TEAM_ID` → `homeTeam`, `BIRTH_COUNTRY` →
   `birthCountry`), and to-many names pick up a FK role qualifier when the FK embeds the source
   entity name, even one sans a common table prefix (`HOME_TEAM_ID` → `homeGames`; `home_team_id`
   referencing `aa_team` → `homeGames` too). What still collides: to-many collections whose FK role is
   unrelated to the entity name (`MANAGER_ID` / `AUDITOR_ID` → `projects` / `projects1` — rename by
   role: `managedProjects` / `auditedProjects`), and — in models imported by older Cayenne versions —
   to-one ends whose FK columns lack an `_ID` suffix and collapsed to `employee` / `employee1`.
3. **A common entity prefix leaking into relationship names** — when every entity shares a prefix
   that was **kept** on the class names (`AaCustomer`, `AaOrder`), the prefix can leak into
   relationship names. The generator already keeps it out of to-many names (the leading `_`-tokens
   shared by the source and target table names are dropped → `orders`, not `aaOrders`); what leaks is
   to-one names built from prefixed FK columns (`AA_CUSTOMER_ID` → `aaCustomer`) and to-many names in
   models imported by older Cayenne versions (`aaOrders`). Strip the prefix from those relationship
   names (`customer`, `orders`); a relationship is a role/property, and the prefix is noise there.
   **Leave the entity names alone** — the prefix on classes is the user's choice, and renaming
   entities regenerates classes.
4. **Other clear, defensible improvements** — illegal identifiers (digit-leading names; Java
   keywords are fine — cgen escapes them — and properties clashing with base-class getters like
   `class` / `objectId` are auto-qualified with the entity name by the generator, though older
   imports may carry them raw), lost acronym casing, obvious cryptic abbreviations applied
   consistently, plural-table-to-singular-entity. Conservative by default; when unsure, leave the
   baseline name and ask.

Relationship cleanup is anchored on the **DbRelationship** — it's the first-class citizen, since
every FK has one whether or not an ObjRelationship was generated on top. The rules above (run-together,
numbered collisions, prefix leak) apply to DbRelationship names directly, derived from their own DB
metadata (FK column for to-one, pluralized target DbEntity for to-many). When an ObjRelationship *is*
built on a DbRelationship, keep the two names **in sync** — rename both together, per direction.
DbRelationships with no ObjRelationship (an ungenerated reverse direction, a skipped FK, a hop inside
a flattened many-to-many) are cleaned the same way — don't skip them. Any rename must update every
`db-relationship-path` segment that names it (see Step 4).

## Step 3 — Present the rename plan

Show the user a table of `old → new` grouped by entity, plus the mirrored DbRelationship renames, and
state plainly what you're **leaving alone** so it's clear this isn't a blanket rewrite.

- **Entire-model mode, or any rename of a name already referenced by user Java/queries** → confirm
  before applying. Renaming an ObjEntity regenerates its class and can break existing code.
- **Changed-only, right after an import** (new elements not referenced anywhere yet) → low risk; apply
  and report, but still list every rename you made.

If there are many domain abbreviations to expand, ask the user for a glossary rather than guessing.

## Step 4 — Apply the renames safely

Edit the `*.map.xml`. For **every** rename, walk the matching checklist in
`model-naming-rename-safety.md` and update all references in the same edit:

- ObjEntity → `className`, every `obj-relationship` `source`/`target`, query `root-name`, EJBQL,
  `result-entity`.
- ObjRelationship → prefetch/expression/EJBQL paths (its `db-relationship-path` is unaffected).
- ObjAttribute → qualifier/ordering/EJBQL paths (its `db-attribute-path` is unaffected).
- DbRelationship → every `db-relationship-path` segment that names it, including inside dotted
  flattened chains. This holds whether or not the DbRelationship backs an ObjRelationship — a
  standalone DbRelationship can still be named as a segment in another entity's flattened path.

Keep every name unique within its scope, and preserve the file's existing formatting and element
order.

## Step 5 — Validate and hand off

- Re-walk the cross-reference checklist: does every `source`/`target`, `root-name`, `db-relationship-path`
  segment, and `className` still resolve?
- Hand off to `cayenne-cgen` to (re)generate the Java classes with the final names. **Naming must
  happen before cgen** — if classes were already generated with the old names, point out the now-stale
  `.java` files so the user can regenerate/delete them.

## Anti-patterns

- **Don't rewrite names the generator already got right.** A camelCase attribute, a PascalCase entity,
  a plural to-many, an FK-derived to-one — all fine. This is polish, not a redo.
- **Don't clean the entire model when only an import changed things.** Scope with `git diff`; default
  to changed-only.
- **Don't invent word splits.** Split `gametype` → `GameType` only when the boundary is real; don't
  break single words (`status`, `metadata`).
- **Don't guess abbreviation expansions.** Ask for a glossary when a model is full of them.
- **Don't rename an ObjEntity without updating `className`, every reference, and regenerating** — a
  bare rename orphans the old class and dangles relationship `source`/`target` and query `root-name`.
- **Don't rename a DbRelationship without fixing every `db-relationship-path` segment**, including
  flattened chains on other entities.
- **Don't skip a DbRelationship just because it has no ObjRelationship.** Standalone DbRelationships
  are in scope — clean them on their own merits from their DB metadata, not by mirroring.
- **Don't repoint `db-attribute-path` / `db-relationship-path` targets.** You rename the element; you
  never change what a path points to.
- **Don't run cgen yourself.** Hand off to `cayenne-cgen`.
