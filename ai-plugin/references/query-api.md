# Query API reference

How to fetch and filter persistent objects with Cayenne 5.0.

## Three query mechanisms

| API | When to use |
|---|---|
| `ObjectSelect` | Default for selecting Java entities. Fluent, type-safe. |
| `SQLSelect` / `SQLExec` | Raw SQL with Cayenne parameter binding. |
| Named queries (XML) | When the same query is reused many places; lives in the DataMap. |

## ObjectSelect — primary fluent query API

`org.apache.cayenne.query.ObjectSelect`. Factory methods:

```java
import org.apache.cayenne.query.ObjectSelect;

// Fetch all
List<Artist> all = ObjectSelect.query(Artist.class).select(ctx);

// Single by criteria
Artist picasso = ObjectSelect.query(Artist.class)
        .where(Artist.ARTIST_NAME.eq("Picasso"))
        .selectOne(ctx);

// First matching, null if none
Artist maybe = ObjectSelect.query(Artist.class)
        .where(Artist.ARTIST_NAME.eq("Picasso"))
        .selectFirst(ctx);

// Iterate large result without loading all
try (ResultIterator<Artist> it = ObjectSelect.query(Artist.class).iterator(ctx)) {
    while (it.hasNextRow()) {
        Artist a = it.nextRow();
        ...
    }
}
```

### Filtering with Property constants

cgen generates `Property<T>` constants on each entity superclass: `Artist.ARTIST_NAME`, `Artist.DATE_OF_BIRTH`, `Artist.PAINTINGS`. These are the recommended way to express filters because they are type-checked.

```java
ObjectSelect.query(Artist.class)
        .where(Artist.ARTIST_NAME.likeIgnoreCase("p%"))
        .and(Artist.DATE_OF_BIRTH.between(d1, d2))
        .or(Artist.ARTIST_NAME.eq("Anonymous"))
        .select(ctx);
```

Common predicate methods on `Property<T>`:

- `.eq(value)`, `.ne(value)`
- `.lt(value)`, `.lte(value)`, `.gt(value)`, `.gte(value)`
- `.like(pattern)`, `.likeIgnoreCase(pattern)`, `.contains(s)`, `.startsWith(s)`, `.endsWith(s)`
- `.in(values...)`, `.nin(values...)`
- `.isNull()`, `.isNotNull()`
- `.between(min, max)`

### Ordering

```java
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.SortOrder;

ObjectSelect.query(Artist.class)
        .orderBy(Artist.ARTIST_NAME.asc())
        .orderBy(Artist.DATE_OF_BIRTH.desc())
        .select(ctx);
```

### Pagination

```java
ObjectSelect.query(Artist.class)
        .pageSize(50)              // server-side pagination
        .limit(1000)
        .offset(0)
        .select(ctx);
```

`pageSize` enables on-demand loading: the result list contains all PKs but only the requested page's full objects are materialized when accessed.

### Prefetching (avoiding N+1)

```java
// Joint: same query, single SQL
ObjectSelect.query(Artist.class)
        .prefetch(Artist.PAINTINGS.joint())
        .select(ctx);

// Disjoint: separate query per relationship
ObjectSelect.query(Artist.class)
        .prefetch(Artist.PAINTINGS.disjoint())
        .select(ctx);

// Disjoint-by-id: separate query keyed by source PKs
ObjectSelect.query(Artist.class)
        .prefetch(Artist.PAINTINGS.disjointById())
        .select(ctx);
```

Rule of thumb: `joint()` for to-one, `disjoint()` for to-many that fans out modestly, `disjointById()` for large to-many.

### Caching

```java
ObjectSelect.query(Artist.class)
        .localCache()                    // per-context cache
        .select(ctx);

ObjectSelect.query(Artist.class)
        .sharedCache("artistGroup")      // cross-context, named cache group
        .select(ctx);
```

Use `sharedCache` for reference data. Invalidate by group via `cayenne-cache-invalidation` if installed.

### Aggregates and column-only queries

```java
import org.apache.cayenne.query.ColumnSelect;
import org.apache.cayenne.exp.property.PropertyFactory;

// Single column
List<String> names = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME).select(ctx);

// Multiple columns
List<Object[]> rows = ObjectSelect.columnQuery(Artist.class,
        Artist.ARTIST_NAME, Artist.DATE_OF_BIRTH).select(ctx);

// Aggregate
long count = ObjectSelect.query(Artist.class).selectCount(ctx);
```

### Selecting by primary key

```java
import org.apache.cayenne.query.SelectById;

Artist a = SelectById.query(Artist.class, 42).selectOne(ctx);
```

## SQLSelect / SQLExec — raw SQL

```java
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.query.SQLExec;

// Select into entities
List<Artist> artists = SQLSelect.query(Artist.class,
        "SELECT * FROM ARTIST WHERE ARTIST_NAME LIKE #bind($name)")
        .params(Map.of("name", "P%"))
        .select(ctx);

// Select into raw rows
List<Object[]> rows = SQLSelect.scalarQuery(Object[].class,
        "SELECT ARTIST_ID, ARTIST_NAME FROM ARTIST")
        .select(ctx);

// Execute (insert/update/delete)
int affected = SQLExec.query(
        "UPDATE ARTIST SET ARTIST_NAME = UPPER(ARTIST_NAME)")
        .update(ctx);
```

Parameter binding uses Velocity-like syntax: `#bind($name)`, `#bindEqual($name)`, `#bindNotEqual($name)`, `#chain('AND' 'condition' "$param")`. Never concatenate user input into SQL strings — use bindings.

## Expression — programmatic predicates

`Expression` is Cayenne's predicate AST. Most queries don't construct one directly — they let `Property` constants do it.

### Preferred: build expressions from `Property` constants

The `Property<T>` constants on each entity superclass (`Artist.ARTIST_NAME`, `Artist.DATE_OF_BIRTH`, `Artist.PAINTINGS`, etc., declared on the generated `_Artist` superclass and inherited by the user `Artist` subclass) return `Expression` objects from every predicate method. Compose them with `andExp` / `orExp`:

```java
import org.apache.cayenne.exp.Expression;

Expression e = Artist.ARTIST_NAME.eq("Picasso")
        .andExp(Artist.DATE_OF_BIRTH.gt(someDate));

ObjectSelect.query(Artist.class).where(e).select(ctx);
```

This is the canonical pattern — see Cayenne's own test suites for many real-world examples (`Artist.ARTIST_NAME.eq(...)`, `Painting.TO_ARTIST.eq(artist)`, etc.). It's type-checked, refactor-safe, and survives column renames in the model.

For relationship traversal, chain `Property` constants with `.dot(...)`:

```java
Expression hasGuernica = Artist.PAINTINGS.dot(Painting.PAINTING_TITLE).eq("Guernica");
```

### Fallback: `ExpressionFactory` (dynamic field names)

Only when field selection is dynamic at runtime (e.g., the user picks a column from a UI dropdown):

```java
import org.apache.cayenne.exp.ExpressionFactory;

String runtimeField = pickField();   // not known at compile time
Expression e = ExpressionFactory.matchExp(runtimeField, value);
```

Avoid `ExpressionFactory.*` for static queries — `Property` constants are strictly better.

### Fallback: `Expression.fromString(...)` (parsed string form)

```java
Expression e = Expression.fromString("artistName = 'Picasso' and dateOfBirth > $cutoff");
e = e.params(Map.of("cutoff", someDate));
```

Supports `=`, `!=`, `<`, `>`, `<=`, `>=`, `like`, `likeIgnoreCase`, `in`, `between`, `and`, `or`, `not`, `null`. Dotted paths traverse relationships: `paintings.gallery.galleryName = 'MoMA'`. Useful when expressions are stored as strings (e.g. in config); otherwise prefer Property constants.

## Anti-patterns

- **String concatenation in raw SQL.** Always use `#bind($name)`. Concatenation is a SQL injection vector.
- **Using `selectOne` when multiple may match.** It throws. Use `selectFirst` if "any one" is okay.
- **Loading large result sets without `pageSize` or `iterator()`.** Both methods avoid materializing the full list.
- **N+1 from missing prefetch.** If you iterate `artists` and access `artist.getPaintings()` per artist, you'll fire one query per artist. Use `.prefetch(...)`.
