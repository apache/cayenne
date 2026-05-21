---
name: cayenne-query
description: "Use this skill whenever the user wants to write or modify a Cayenne query — fetching entities by criteria, joining, prefetching to avoid N+1, ordering, paginating, aggregating, or running raw SQL through Cayenne. Trigger on phrases like 'query for X', 'fetch all artists where ...', 'write an ObjectSelect', 'use SQLSelect', 'use SelectById', 'add a prefetch', 'get distinct values', 'count rows', 'find by ID', 'load by primary key', 'build a Cayenne expression', 'why am I getting N+1', 'how do I paginate', 'select a single column', 'select columns into a DTO', 'named query in the DataMap'. Do NOT trigger for modeling changes (use cayenne-modeling) or runtime bootstrap (use cayenne-runtime)."
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
# cayenne-query

Write idiomatic Cayenne 5.0 queries — `ObjectSelect`, `SelectById`, `SQLSelect`, expressions, prefetch, pagination, aggregates.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/query-api.md` — every query mechanism with examples (ObjectSelect, SQLSelect/SQLExec, Expression, ColumnSelect, SelectById).

## Step 1 — Identify the query shape

| Intent | Use |
|---|---|
| Fetch one or more entities matching criteria | `ObjectSelect.query(Cls.class).where(...).select(ctx)` |
| Fetch by primary key | `SelectById.query(Cls.class, pk).selectOne(ctx)` |
| Aggregate (count, sum) | `ObjectSelect.query(Cls.class).selectCount(ctx)` or `ColumnSelect` with aggregate functions |
| One or a few columns only (DTO-style) | `ObjectSelect.columnQuery(Cls.class, Cls.NAME, Cls.AGE).select(ctx)` |
| Raw SQL with parameter binding | `SQLSelect.query(Cls.class, "SELECT ...").params(...).select(ctx)` |
| Insert/update/delete bulk | `SQLExec.query("UPDATE ...").update(ctx)` |
| Reused named query stored in DataMap | XML `<query>` (see `${CLAUDE_PLUGIN_ROOT}/references/datamap-schema.md`) loaded via `NamedQuery` |

### Primary-key lookups: prefer `SelectById`

When the user is fetching a single entity by its PK, use `SelectById` rather than `ObjectSelect.where(PK.eq(...))`:

```java
Artist a = SelectById.query(Artist.class, 42).selectOne(ctx);
```

`SelectById` can hit the `ObjectContext`'s session cache without firing a SQL query when the row is already loaded. `ObjectSelect.where(...)` always goes to the database. For composite PKs, pass a `Map<String, Object>` of PK column → value.

## Step 2 — Filter with `Property` constants

cgen generates `Property<T>` constants on each entity superclass. Use them — they're type-checked at compile time:

```java
ObjectSelect.query(Artist.class)
    .where(Artist.ARTIST_NAME.likeIgnoreCase("p%"))
    .and(Artist.DATE_OF_BIRTH.between(d1, d2))
    .select(ctx);
```

Only fall back to `ExpressionFactory.matchExp("artistName", ...)` or `Expression.fromString(...)` when:

- The filter is dynamic (field name comes from runtime input), or
- The user explicitly wants string-based expressions.

`query-api.md` lists all common predicate methods on `Property`.

## Step 3 — Handle relationships (prefetch)

When the query traverses a relationship in a loop, **always** add a prefetch to avoid N+1:

```java
// Bad — fires one query per artist when paintings are accessed
for (Artist a : artists) { a.getPaintings().size(); }

// Good
List<Artist> artists = ObjectSelect.query(Artist.class)
    .prefetch(Artist.PAINTINGS.disjoint())
    .select(ctx);
```

Pick:

- `.joint()` — to-one relationships, or to-many with small fan-out. Single SQL join.
- `.disjoint()` — to-many, modest size. Two queries.
- `.disjointById()` — to-many, very large parent set. Two queries, keyed by PKs.

## Step 4 — Apply ordering, paging, and caching as needed

```java
ObjectSelect.query(Artist.class)
    .orderBy(Artist.ARTIST_NAME.asc())
    .pageSize(50)             // server-side pagination
    .limit(1000)
    .localCache()             // per-context cache, or .sharedCache("group-name")
    .select(ctx);
```

Use `sharedCache` for reference data (rarely changes, read often). Use `pageSize` to avoid loading entire result sets.

## Step 5 — Raw SQL when needed

```java
List<Artist> hits = SQLSelect.query(Artist.class,
        "SELECT * FROM ARTIST WHERE ARTIST_NAME LIKE #bind($pattern)")
    .params(Map.of("pattern", userInput + "%"))
    .select(ctx);
```

**Always** use `#bind($name)` placeholders. Never concatenate user input into SQL. Cayenne's SQLTemplate is Velocity-based — see `query-api.md` for `#bind`, `#bindEqual`, `#chain`, and adapter-specific SQL with `<sql adapter-class="...">`.

## Anti-patterns

- **Parameter String concatenation in raw SQL.** Use `#bind($name)`. Concatenation is a SQL injection vector and may also result in invalid syntax.
- **Using `selectOne` when multiple may match.** It throws. Use `selectFirst` if "any one" is okay.
- **Loading large result sets without pagination.** Use `.pageSize(n)` or `iterator()` and process incrementally.
- **N+1 from missing prefetch.** If iterating entities and accessing relationships per-entity, add `.prefetch(...)`.
- **Using `Expression.fromString(...)` or `ExpressionFactory.matchExp("fieldName", ...)` for static queries.** Prefer typed `Property` constants (`Artist.ARTIST_NAME.eq(...)`) — they catch typos at compile time and survive model refactors.
- **Mutating fetched objects without committing.** `ObjectContext.commitChanges()` is required for changes to persist.
