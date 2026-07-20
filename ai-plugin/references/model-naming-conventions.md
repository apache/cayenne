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
# Obj-layer naming conventions — what to fix, what to leave alone

Reference for the `cayenne-model-naming` skill. The governing rule: **the Modeler already
generates good names for the common case. This is a polish pass, not a rewrite.** Read the
"deterministic baseline" first so you can recognize a name that is already correct and skip it.

## The deterministic baseline (leave these names alone)

Reverse engineering (`dbimport_run`, the Modeler's DB Import) generates the Obj-layer names before
you ever see the model, then de-duplicates any collisions. It already applies these principles — and
you must preserve them:

1. **Obj names stay as close to the DB names as possible** — the object name is a transliteration
   of the table/column, not a re-invention.
2. **Java identifier / class conventions** — classes PascalCase, properties camelCase.
3. **snake_case → camelCase / PascalCase** — split on `_`, drop the underscores, camel-join.
4. **Relationship names from entity name + cardinality** — see below.

Concretely, the generator produces:

| DB element | Rule | Result |
|---|---|---|
| `db-entity` name | stem, split on `_`, capitalize each token | `ARTIST_GROUP` → `ArtistGroup` |
| `db-attribute` name | split on `_`, camelCase | `FIRST_NAME` → `firstName` |
| to-one relationship | FK column, minus its trailing `_ID`/`ID` when present; target entity name for a compound (multi-column) FK or when there are no joins | `MANAGER_ID` → `manager`; `BIRTH_COUNTRY` → `birthCountry` |
| to-many relationship | English plural of the target entity name (minus the leading `_`-tokens shared with the source table name), prefixed with the FK role qualifier when the FK column embeds the source entity name | `PAINTING` → `paintings`; `HOME_TEAM_ID` → `homeGames`; `AA_TEAM` referencing `AA_GAME` → `games` |
| name collision within an entity | append a numeric suffix | `team`, `team1`, `team2` … |

Generation also collapses all-upper tokens to lowercase and preserves already-mixed
case. So `GameType`, `gameType`, `firstName`, `ArtistGroup`, `paintings`, `manager` are **all
already correct** — do not re-case, re-spell, re-pluralize, or "prettify" them. If a name is a
clean camelCase/PascalCase transliteration of its DB element with the right cardinality, it is
done. Touch nothing.

## Where the deterministic algorithm falls short — the AI job

The cases below are the ones we've identified where the generator can't do better and your judgment
adds real value. They are **illustrative, not exhaustive** — the generator is a deterministic
transliteration, so any place where a *human* reading the DB name would produce a clearly better
Java name than a mechanical `_`-split is fair game (§5). Focus your attention on these gaps; don't
touch names the baseline already got right.

### 1. Run-together DB names with no separators

Word-splitting happens **only on `_`**. A single-token, uniform-case DB name has no boundary to
split on, so a multi-word concept collapses into one lowercased chunk. This is the classic case:

| DB name | Generator output | Correct |
|---|---|---|
| `gametype` / `GAMETYPE` | `Gametype` | `GameType` |
| `dateofbirth` | `Dateofbirth` | `dateOfBirth` |
| `ordernumber` | `Ordernumber` | `orderNumber` |
| `custaddr` | `Custaddr` | `CustomerAddress` (with expansion — see §3) |

Split on the **real** word boundary, using domain knowledge, and re-apply the Java convention
(PascalCase for entities, camelCase for attributes and relationships). Only split where you are confident a boundary
exists — never invent one (`status` is not `sta` + `tus`; `metadata` is one word). Names that are
**already** mixed-case (`GameType`, `gameType`) were handled by the generator — leave them.

### 2. More than one relationship between the same two tables

When two FKs point at the same target table (or two relationships otherwise share a target), and a
role can't be derived from the FK column, generation disambiguates with numbers. **The two
directions are not equally affected** — the to-one and to-many sides are named by different rules:

- **to-many side collides when the FK doesn't embed the source entity name.** When the FK column is
  `<ROLE>_<SOURCE_ENTITY>[_ID]`, the generator prepends the role qualifier to the pluralized target
  (`HOME_TEAM_ID` / `AWAY_TEAM_ID` on FKs to `TEAM` → `homeGames` / `awayGames`) — those are already
  correct, leave them. The FK column may drop a common table prefix: `home_team_id` referencing
  `aa_team` still yields the `home` qualifier. It still collides when the FK carries a role
  unrelated to the source entity name:
  `MANAGER_ID` / `AUDITOR_ID` FKs to `EMPLOYEE` both produce `projects` / `projects1` on
  `Employee`, because the role can't be mechanically tied to the entity. Name each collection by its
  role yourself: `managedProjects` / `auditedProjects`.

- **to-one side does NOT collide.** To-one naming is FK-column-based — the name is the FK column
  with a trailing `_ID`/`ID` stripped when present (`HOME_TEAM_ID` → `homeTeam`, `BIRTH_COUNTRY` →
  `birthCountry`), so distinct FK columns yield distinct, good names. Leave those alone. The
  generator falls back to the target entity name only for a compound (multi-column) FK — whose
  column names describe PK components, not a role — or when the relationship has no joins at all.
  Models imported by **older Cayenne versions** used that fallback whenever the FK column lacked an
  `ID`/`_ID` suffix, producing target-entity names and numbered collisions (`employee` /
  `employee1`); when you see those, derive the role from the FK column yourself (`MANAGER` →
  `manager`, `SUPERVISOR` → `supervisor`).

So in the typical "two `<ROLE>_<ENTITY>_ID` FKs" model **both directions are already fine**
(`homeTeam` ↔ `homeGames`, `awayTeam` ↔ `awayGames`) — nothing to rename. When you do rename, give
the two ends matching opposite-role names so the pair is legible from either side
(`manager` ↔ `managedProjects`).

### 3. Genuinely cryptic abbreviations (secondary, be conservative)

Expand an abbreviation only when the win is clear **and** you apply it consistently across the whole
model: `qty` → `quantity`, `amt` → `amount`, `dob` → `dateOfBirth`. An unfamiliar or ambiguous
abbreviation stays as-is (case-normalized) rather than becoming a wrong guess. If the model is full
of domain-specific abbreviations, ask the user for a glossary instead of guessing element by element.

### 4. A common entity prefix leaking into relationship names

Many schemas tag every table with the same prefix (`AA_CUSTOMER`, `AA_ORDER`, or `os_t1`, `os_t2`).
The reverse-engineering **"Strip from Table Names"** (`stripFromTableNames`) setting handles this
cleanly *when it's used*: entity names come through stripped (`AA_CUSTOMER` → `Customer`) and there's
no problem — leave that model alone.

The case that needs you is when the prefix is **kept on the entity names** (stripping was not
configured — often intentional, treating the prefix as a class-name namespace). The generator keeps
the prefix out of **to-many** names on its own: the leading `_`-tokens the source and target table
names share are dropped from the pluralized target (`aa_customer` referencing `aa_order` →
`orders`) — those are already clean. What still **leaks**:

- to-one names built from a prefixed FK column (`AA_CUSTOMER_ID`) carry it → `aaCustomer`.
- models imported by **older Cayenne versions** kept the prefix in to-many names too → `aaOrders`.

A relationship name is a **role/property** on a class (`order.getAaCustomer()`), and the shared
prefix is pure noise there. **Strip the common prefix from the relationship names** — `aaOrders` →
`orders`, `aaCustomer` → `customer` — and mirror the change onto the paired DbRelationship.

**Leave the ObjEntity names as they are.** The prefix on the class names is the user's choice (if
they'd wanted it gone from entities they would have set `stripFromTableNames`); renaming entities is
a bigger, class-regenerating change. This case cleans relationship names only.

### 5. Other cases — use judgment

The three cases above don't exhaust the ways a purely mechanical transliteration can miss. Whenever
you spot a name where a human reading the underlying DB name would obviously do better, and the fix
is defensible (not a guess), apply the same conservative treatment. Some more examples:

- **Genuinely illegal identifiers** the generator passed through — a name starting with a digit, or
  literally `class`, whose getter `getClass()` collides with the final `Object.getClass()`. Java
  *keywords* are **not** a problem — `default`, `package`, `return` and the like compile fine, since
  class generation prefixes the field/parameter name with `_` and embeds the capitalized name in the
  accessors (`getDefault()` / `setDefault()`). Leave keyword-named properties alone.
- **Lost acronym casing** — `HTTPURL` → `Gametype`-style collapse loses the acronym; `httpUrl` /
  `url` may read better than `httpurl`.
- **Plural table → singular entity** — a `CUSTOMERS` table yields `Customers`; an entity is a single
  row, so `Customer` is usually the intent (be careful: only when clearly a pluralized table name,
  and check for a resulting collision).
- **Redundant entity-name prefix on an attribute** — `Artist.artistName` → `name` — only when it's
  clearly noise and doesn't collide.

The bar is the same throughout: a clear, defensible improvement over the mechanical output, applied
consistently. When in doubt, leave the baseline name and surface the question to the user rather than
guessing.

## Target forms (for the names you actually change)

- **ObjEntity `name`** — PascalCase, singular preferred; keep it equal to the `className` simple name.
- **ObjAttribute `name`** — camelCase; strip type/Hungarian prefixes (`strName` → `name`, `n_count`
  → `count`) only when unambiguous.
- **ObjRelationship, to-one** — singular camelCase role.
- **ObjRelationship, to-many** — plural camelCase.
- **DbRelationship `name`** — mirror the paired ObjRelationship name (see below).

## DbRelationship names — the first-class unit of relationship cleanup

A DbRelationship `name` is **arbitrary** (not derived from a real table/column like a DbEntity/
DbAttribute), but it is **not** exempt from cleanup — treat it as the first-class citizen here, since
every FK produces a DbRelationship whether or not an ObjRelationship was generated on it. Reverse
engineering names it with the **same generator** as ObjRelationships (to-one = FK column minus a
trailing `_ID`/`ID` when present; to-many = English plural of the target entity with a
FK role qualifier when the FK embeds the source entity name, all `underscoredToJava`-cased), so it
inherits the same gaps — run-together names,
numbered collisions (`toArtist` / `toArtist1`), a leaked common prefix. Apply §1–§5 to DbRelationship
names directly, deriving the fix from the DbRelationship's own DB metadata (its FK column for to-one,
its target DbEntity pluralized for to-many).

**Sync the ObjRelationship when one exists.** An ObjRelationship built on a DbRelationship is linked
by its `db-relationship-path` naming that DbRelationship. Keep the two names matched **per direction**:
rename both together (`homeTeam` ↔ `homeTeam`, `homeGames` ↔ `homeGames`); if one is already good,
both are. A DbRelationship with **no** ObjRelationship — an ungenerated reverse direction, a FK where
`Create ObjRelationships` was unchecked, or a hop inside a flattened many-to-many
(`db-relationship-path="artistGroupArray.toArtist"`) — is cleaned exactly the same way; there's just
no ObjRelationship to sync.

Its name stays unique within its source DbEntity. See `model-naming-rename-safety.md` for what to
update on any rename.
