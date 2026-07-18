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
# Rename safety — cross-reference checklist

Reference for the `cayenne-model-naming` skill. A model name is referenced from several other places
in the same DataMap (and from user Java code). Renaming an element without updating its references
leaves a dangling reference that fails Cayenne validation at load time, or silently changes behavior.
For every rename, walk the checklist for that element type. Element shapes are in `datamap-schema.md`.

## Golden rules

- **Rename the element, don't repoint it.** You change a `name`; you never touch the `db-attribute-path`
  or `db-relationship-path` *targets* to point somewhere else. Those paths only change when the thing
  they point *to* was itself renamed (DbAttribute/DbRelationship), and then only the matching segment.
- **Names are unique within their scope.** Within an ObjEntity, attributes and relationships share one
  namespace; within a DbEntity, likewise; ObjEntity names are unique within the DataMap; DbRelationship
  names are unique within their source DbEntity. Never introduce a collision — collisions are exactly
  what produced the numbered names (`team1`) you're removing.
- **Batch, then re-validate.** Apply all renames, then re-walk every checklist below to confirm no
  reference dangles.
- **Name before you generate.** Renames must happen *before* `cayenne-cgen`, so classes are generated
  with the final names. Renaming after generation orphans the old `.java` files.

## Rename an ObjEntity `name` (`X` → `Y`)

| Update | Where |
|---|---|
| `className` simple name | Same `<obj-entity>`. Keep it equal to the new `name` → the generated class is `Y`. Old `X.java` / `_X.java` become orphaned; regenerate and delete the stale pair. |
| `source="X"` / `target="X"` | Every `<obj-relationship>` in the DataMap — on **any** entity, not just this one. |
| `root-name="X"` | Every `<query>` with `root="obj-entity"`. |
| entity name in EJBQL | `<ejbql>` bodies (`select a from X a` → `... from Y a`). |
| `result-entity="X"` | `<query type="ProcedureQuery">`. |
| Java | `ObjectSelect.query(X.class)` tracks `className`, so updating `className` + regenerating covers it. Also fix string-based entity lookups (`context.newObject("X")`, `objectSelect("X")`) and EJBQL strings in code. |

The `dbEntityName` does **not** change — the DbEntity keeps its DB-derived name.

## Rename an ObjAttribute `name`

| Update | Where |
|---|---|
| query qualifiers / orderings | `<qualifier>` and `<ordering>` bodies that reference the old property name. |
| EJBQL | `<ejbql>` paths (`a.oldName` → `a.newName`). |
| Java | Generated getter/setter changes; fix `Expression`/`Property` paths and `ObjectSelect` column refs in user code. |

`db-attribute-path` is **unaffected** — it names the DB column, which didn't change. Uniqueness is
within the owning ObjEntity (attrs + rels).

## Rename an ObjRelationship `name`

| Update | Where |
|---|---|
| prefetches | `<prefetch>` bodies and any dotted path that traverses this relationship. |
| qualifiers / orderings / EJBQL | Expression paths that step through the old relationship name. |
| Java | Generated getter/setter changes; fix prefetch/expression paths in user code. |

`db-relationship-path` is **unaffected** — it names DbRelationships, not this ObjRelationship's own
name. Uniqueness is within the owning ObjEntity.

## Rename a DbRelationship `name` (`a` → `b`)

| Update | Where |
|---|---|
| `db-relationship-path` **segments** | Every `<obj-relationship>` whose `db-relationship-path` contains `a` — **including inside dotted flattened chains**. E.g. path `artistGroupArray.toArtist`, renaming `toArtist` → `b` gives `artistGroupArray.b`. These references can live on entities other than the DbRelationship's source. |

No Java impact — DbRelationships are a DB-layer concept. Uniqueness is within the source DbEntity
(attrs + rels). If the DbRelationship backs an ObjRelationship, keep the name mirrored with it per
direction; if it is **standalone** (no ObjRelationship built on it — see `model-naming-conventions.md`),
there is nothing to mirror, but the `db-relationship-path` update above still applies: a standalone
DbRelationship can appear as a segment in another entity's flattened path.

## Paired renames

A single FK is usually four coordinated names: two DbRelationships (one per direction) and two
ObjRelationships built on them. When you fix a numbered-collision pair, rename all four so both ends
read as opposite roles and each ObjRelationship's name still mirrors its DbRelationship:

```
DbRelationship  ARTIST_ID  (Game→Team, to-one)   homeTeam
DbRelationship  ARTIST_ID  (Team→Game, to-many)  homeGames
ObjRelationship (Game→Team)                       homeTeam   (mirrors db-rel homeTeam)
ObjRelationship (Team→Game)                       homeGames  (mirrors db-rel homeGames)
```

After renaming a DbRelationship, remember its name appears in the *other* direction's ObjRelationship
`db-relationship-path` only when that path traverses it (flattened case) — a simple one-hop
ObjRelationship's `db-relationship-path` is just its own backing DbRelationship's new name.
