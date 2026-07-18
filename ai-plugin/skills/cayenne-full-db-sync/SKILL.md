---
name: cayenne-full-db-sync
description: "Use this skill when the user wants to bring their WHOLE Cayenne project in line with the database in one shot — the mapping, the Object-layer names, and the generated Java classes together. This is the end-to-end 'sync with the DB' workflow, and it orchestrates three skills in order: `cayenne-db-import` (import schema metadata into the DataMap) → `cayenne-model-naming` (polish the just-imported names) → `cayenne-cgen` (regenerate Java classes). Trigger on holistic phrases like 'sync my project with the database', 'sync with the DB', 'my schema changed, update everything', 'update my entities/classes from the database', 'reverse engineer and regenerate the classes', 'import the new tables and rebuild the entities', 'full DB sync', 'bring the model and classes up to date with the DB'. The distinguishing signal is scope: the user wants the whole project (mapping + names + Java code), not just one stage. For the *model/mapping only* (no name cleanup, no class generation) use `cayenne-db-import`; to (re)generate classes alone use `cayenne-cgen`; to clean names alone use `cayenne-model-naming`. Uses the `mcp__cayenne__dbimport_run` and `mcp__cayenne__cgen_run` MCP tools via the sub-skills; does NOT use Maven or Gradle goals."
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
# cayenne-full-db-sync

Bring an entire Cayenne project in line with the database in one pass. This skill is a **thin
orchestrator** — it runs three existing skills in sequence and does not re-implement any of their
logic:

1. **`cayenne-db-import`** — import DB schema metadata into the DataMap (the mapping XML).
2. **`cayenne-model-naming`** — polish the Object-layer names of what the import just added.
3. **`cayenne-cgen`** — regenerate the Java entity classes so the code matches the model.

Each sub-skill still runs within its own scope and carries its own required reading, MCP tool usage,
failure handling, and anti-patterns. Your job here is to drive them in order, pass the shared
project/DataMap identity through, and short-circuit correctly when a stage has nothing to do.

## When to use this vs. a single skill

- **Whole project should reflect the DB** (mapping + names + Java) → this skill.
- **Model/mapping only**, no name cleanup or class generation → `cayenne-db-import`.
- **Java classes only**, model already correct → `cayenne-cgen`.
- **Names only**, no import or regeneration → `cayenne-model-naming`.

If the request is ambiguous ("update from the DB"), default to this full sync — it is the common
intent — but say what you're about to do (import → clean names → regenerate) before you start.

## Required reading

This skill delegates, so read each sub-skill and let it pull its own references:

- `${CLAUDE_PLUGIN_ROOT}/skills/cayenne-db-import/SKILL.md`
- `${CLAUDE_PLUGIN_ROOT}/skills/cayenne-model-naming/SKILL.md`
- `${CLAUDE_PLUGIN_ROOT}/skills/cayenne-cgen/SKILL.md`
- `${CLAUDE_PLUGIN_ROOT}/references/project-layout.md` — to resolve the project descriptor and DataMap once, up front.

## Step 0 — Resolve the shared inputs once

All three stages operate on the same project. Resolve these before starting so you can pass them
through:

- **`projectPath`** — absolute path to the top-level project descriptor (`cayenne-*.xml`), via
  `project-layout.md`. Not a DataMap file.
- **`dataMap`** — the DataMap name from `<map name="...">` in the descriptor. If the project has
  multiple DataMaps and the user wants "everything", run the whole pipeline once per DataMap. If
  ambiguous, ask which DataMap; cache the answer.

Tell the user the plan in one line: "I'll import from the DB, clean up the new names, then regenerate
the classes."

## Step 1 — Import (`cayenne-db-import`)

Follow the `cayenne-db-import` skill to run `dbimport_run` for the resolved project/DataMap. Because
you are orchestrating, **you** drive the follow-up here — do not act on that skill's own Step 5
hand-off prompts (it would suggest naming/cgen again); this skill sequences them instead.

Branch on the result status:

- **`imported` with a non-empty `summary`** (entities/relationships added/modified) → the model
  changed. Proceed to Step 2.
- **`up_to_date`** (or `imported` with an all-zero summary) → the mapping already matches the DB.
  **Skip Step 2** (nothing new to name). Go straight to Step 3 to guarantee the Java classes are in
  sync — `cgen_run` is idempotent and simply reports everything skipped if the code is already
  current. Tell the user the model was already up to date.
- **`validation_failed`** → follow `cayenne-db-import`'s remediation table for the `error.code`. The
  common first-run case is `dbconnector_not_configured`, which routes through the Modeler GUI to save
  the connection; once resolved, re-run the import and continue the pipeline. If it can't be resolved,
  stop and surface the problem — do **not** run naming or cgen on a failed import.
- **`error`** (partial/mid-run failure) → surface `error.message` and the partial summary, then stop.
  Don't proceed to naming or cgen.

## Step 2 — Clean up names (`cayenne-model-naming`)

Only when Step 1 imported real changes. Follow the `cayenne-model-naming` skill in its **changed-only**
mode — it scopes itself to the import's additions via `git diff`, so you don't tell it which entities
changed. Within this pipeline it runs **unattended**: apply the changed-scope renames and **report**
what changed; do not pause for per-rename confirmation. (Naming must run *before* cgen so the classes
are generated with the final names.)

Two carry-overs from that skill still hold: it only touches what the deterministic generator can't get
right (run-together names, `team1`-style collisions, a table prefix leaking into relationships), and
every rename updates its cross-references per `model-naming-rename-safety.md`.

## Step 3 — Regenerate classes (`cayenne-cgen`)

Follow the `cayenne-cgen` skill to run `cgen_run` for the same project/DataMap. This materializes the
Java entity classes from the now-final model. Report the `summary` counts and the destination as that
skill describes.

## Step 4 — Summarize the whole run

Give the user a single consolidated report of the three stages:

- **Import** — entities/relationships added/removed/modified (from the `dbimport_run` summary).
- **Names** — the renames applied (old → new), or "none needed".
- **Classes** — files written / skipped, and the destination directory.

Then the usual follow-ups: remind them not to edit the `_<Entity>.java` superclasses (regenerated),
and to save the project in the Modeler if it's open.

## Anti-patterns

- **Don't use this for a single stage.** Model-only import → `cayenne-db-import`; classes-only →
  `cayenne-cgen`; names-only → `cayenne-model-naming`. This skill is for the whole pipeline.
- **Don't re-implement the sub-steps.** Delegate to each skill so its MCP tool usage, failure modes,
  and anti-patterns all apply. This file only sequences and short-circuits.
- **Don't run naming or cgen after a failed or empty import.** A `validation_failed`/`error` stops the
  pipeline; `up_to_date` skips naming (but cgen may still run as an idempotent guarantee).
- **Don't reorder the stages.** Names must be finalized before cgen, or classes are generated with the
  names you're about to change.
- **Don't rewrite the whole model during a sync.** Naming stays in changed-only scope (import
  additions), never an entire-model pass, unless the user explicitly asks for one.
- **Don't suggest `mvn cayenne:cdbimport` / `cayenne:cgen` or the Gradle tasks.** The sub-skills use
  the MCP tools exclusively; if the server isn't connected, they point at `cayenne-mcp-server/README.md`
  and stop.
