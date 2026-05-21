# Runtime API reference

How to bootstrap Cayenne in a Java application and use `ObjectContext` for CRUD.

**Availability: Cayenne 5.0+ only.** `CayenneRuntime` and `CayenneRuntimeBuilder` are 5.0 names — they were called `ServerRuntime` and `ServerRuntimeBuilder` (in package `org.apache.cayenne.configuration.server`) in Cayenne 4.x. If the user's project is on 4.x, the patterns below won't compile — this plugin is targeted at 5.0.

## CayenneRuntime — top-level container

`org.apache.cayenne.runtime.CayenneRuntime` is the entry point. One per application lifetime (or per data domain in multi-DB setups).

Two creation paths:

### 1. With a built-in connection pool

```java
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.CayenneRuntimeBuilder;

CayenneRuntime runtime = CayenneRuntime.builder()
        .addConfig("cayenne-mydb.xml")
        .url("jdbc:postgresql://localhost:5432/mydb")
        .jdbcDriver("org.postgresql.Driver")
        .user("cayenne")
        .password("secret")
        .minConnections(1)
        .maxConnections(10)
        .build();
```

### 2. With an external DataSource (Spring, HikariCP, Bootique, etc.)

```java
import javax.sql.DataSource;

DataSource ds = ...; // created elsewhere in your app

CayenneRuntime runtime = CayenneRuntime.builder()
        .addConfig("cayenne-mydb.xml")
        .dataSource(ds)
        .build();
```

This path is preferred in production — your existing DI framework manages the pool, and Cayenne uses it.

### CayenneRuntimeBuilder methods

From `cayenne/src/main/java/org/apache/cayenne/runtime/CayenneRuntimeBuilder.java`:

| Method | Purpose |
|---|---|
| `addConfig(String)` | Add a Cayenne project descriptor (`cayenne-*.xml`) location. Resolved via classpath. |
| `addConfigs(String...)` | Add multiple descriptors. |
| `dataSource(DataSource)` | Use an existing DataSource. Overrides anything in the XML. |
| `url(String)` | Built-in pool: JDBC URL. |
| `jdbcDriver(String)` | Built-in pool: driver class name. |
| `user(String)` / `password(String)` | Built-in pool: credentials. |
| `minConnections(int)` / `maxConnections(int)` | Built-in pool: bounds. |
| `validationQuery(String)` | Built-in pool: pool validation query (e.g. `SELECT 1`). |
| `maxQueueWaitTime(long)` | Built-in pool: max ms to wait for a connection. |
| `addModule(Module)` | Add a Cayenne DI module to override defaults. |
| `disableModulesAutoLoading()` | Skip the `META-INF/services/CayenneRuntimeModuleProvider` auto-load — for tightly-controlled DI. |
| `build()` | Construct the `CayenneRuntime`. |

## Lifecycle

`CayenneRuntime` is **expensive** to create — it parses XML, validates the model, sets up the connection pool. Create it **once**, share it for the application lifetime, and call `shutdown()` on app exit:

```java
runtime.shutdown();
```

Never create a new `CayenneRuntime` per request.

## ObjectContext — the unit of work

`org.apache.cayenne.ObjectContext` is the per-request handle for CRUD. Get one from the runtime:

```java
ObjectContext ctx = runtime.newContext();
```

Each `ObjectContext`:

- Has its own session-level cache of loaded objects.
- Tracks modifications until you call `commitChanges()`.
- While ObjectContext is thread-safe, objects within in are **not thread-safe**. So read-only contexts can be used across threads, whereas read/write ones (those that change object state) would require a new context per request/thread.

### Core methods

```java
// Insert
Artist a = ctx.newObject(Artist.class);
a.setArtistName("Picasso");
ctx.commitChanges();

// Read by ID
Artist a = SelectById.query(Artist.class, 42).selectOne(ctx);

// Update
a.setArtistName("Pablo Picasso");
ctx.commitChanges();

// Delete
ctx.deleteObject(a);
ctx.commitChanges();

// Rollback uncommitted changes
ctx.rollbackChanges();
```

### Transactions

`commitChanges()` runs inside an implicit transaction by default. For multi-step transactions, use `Cayenne.transactional`:

```java
import org.apache.cayenne.tx.BaseTransaction;
import org.apache.cayenne.tx.TransactionalOperation;

runtime.performInTransaction(() -> {
    Artist a = ctx.newObject(Artist.class);
    a.setArtistName("Picasso");
    ctx.commitChanges();

    Painting p = ctx.newObject(Painting.class);
    p.setArtist(a);
    p.setTitle("Guernica");
    ctx.commitChanges();
    return null;
});
```

If any step throws, the whole transaction rolls back.

## DI: integrating with external containers

Cayenne ships its own lightweight DI (`cayenne-di`). It does not require Spring, etc. To use Cayenne inside an application DI container (Spring, Bootique), bind `CayenneRuntime` as a singleton and decide how you want to create `ObjectContext` (request-scoped, method scoped, etc.)

## Common mistakes

- **Creating `CayenneRuntime` per request.** It's a heavyweight singleton. Cache it.
- **Sharing `ObjectContext` that change object state across threads.** Not safe. New context per thread.
- **Mixing objects across contexts.** A `Painting` loaded in context A cannot be assigned to an `Artist` in context B. Use `ctx.localObject(otherObject)` to copy across.
- **Forgetting `commitChanges()`.** Mutations to persistent objects stay in memory until commit.
