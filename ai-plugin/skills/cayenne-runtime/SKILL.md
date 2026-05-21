---
name: cayenne-runtime
description: "Use this skill whenever the user wants to bootstrap Cayenne in a Java application — constructing a CayenneRuntime, wiring a DataSource, configuring connection pools, creating ObjectContexts, or integrating Cayenne with a DI container (Bootique, Spring, plain Java). Trigger on phrases like 'set up Cayenne in my app', 'configure CayenneRuntime', 'wire Cayenne into Bootique/Spring', 'create an ObjectContext', 'configure a DataSource for Cayenne', 'connect Cayenne to Postgres/MySQL/etc.', 'where do I call newContext', 'how do I shut down Cayenne', or any question about the runtime lifecycle. Do NOT trigger for query writing (use cayenne-query) or model editing (use cayenne-modeling)."
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
# cayenne-runtime

Bootstrap `CayenneRuntime` and use `ObjectContext` for CRUD in a Java application.

## Required reading

- `${CLAUDE_PLUGIN_ROOT}/references/runtime-api.md` — `CayenneRuntimeBuilder` methods, `ObjectContext` API, transaction patterns, lifecycle rules.

## Step 1 — Find or create the bootstrap site

Where does the user's app construct `CayenneRuntime`? Common patterns:

- **Plain Java / CLI** — a `main()` method or a singleton holder.
- **Bootique app** — typically a `@Provides` method in a module (`provideCayenneRuntime(@Inject DataSource ds)`). If editing a Bootique app, mention that the `java-dev:bootique-config` skill (if available) can help with the surrounding config wiring.
- **Spring app** — a `@Bean CayenneRuntime cayenneRuntime(DataSource ds)` method in a configuration class.
- **Cayenne-only embedded** — a static field or DI singleton initialized lazily.

If none exists yet, create one. Apply `runtime-api.md` patterns:

```java
CayenneRuntime runtime = CayenneRuntime.builder()
        .addConfig("cayenne-mydb.xml")
        .dataSource(externalDataSource)   // or .url(...).jdbcDriver(...).user(...).password(...)
        .build();
```

## Step 2 — Decide on the DataSource path

Two paths from `CayenneRuntimeBuilder`:

| When | Use |
|---|---|
| Production app with Spring/Bootique/Hikari already managing the pool | `.dataSource(ds)` — Cayenne reuses the external pool. |
| Standalone tool or test fixture with no external DI | `.url(...).jdbcDriver(...).user(...).password(...)` — Cayenne's built-in pool. |

The `.dataSource()` path is preferred when possible. Never put JDBC credentials in `cayenne-*.xml` for production code — keep them in the application's normal config mechanism.

## Step 3 — Use `ObjectContext`

```java
ObjectContext ctx = runtime.newContext();   // one per request/thread
Artist a = ctx.newObject(Artist.class);
a.setArtistName("Picasso");
ctx.commitChanges();
```

Rules:

- `CayenneRuntime` is a heavy singleton — **create once, share for app lifetime**, call `shutdown()` on exit.
- `ObjectContext` is **not thread-safe** — new one per request/thread.
- Don't mix objects across contexts. Use `ctx.localObject(other)` to bring an object into the current context.

## Step 4 — Transactions

`commitChanges()` is auto-transactional. For multi-step transactions:

```java
runtime.performInTransaction(() -> {
    // multiple commits in one transaction
    return null;
});
```

Any throw rolls back the whole block.

## Step 5 — Verify the integration

Tell the user how to smoke-test:

1. Build and run the app.
2. From a request handler: `ObjectContext ctx = runtime.newContext(); long count = ObjectSelect.query(SomeEntity.class).selectCount(ctx);` — should return a number without throwing.

If they hit `CayenneRuntimeException: No DataNode...`, the descriptor's `<map>` reference doesn't have a node — confirm the DataSource wiring went through `CayenneRuntimeBuilder.dataSource(...)` (Cayenne synthesizes a node from this).

## Anti-patterns

- **Creating `CayenneRuntime` per request.** It's a heavyweight singleton. Cache it for the application lifetime.
- **Sharing `ObjectContext` across threads.** Not safe. Create one per thread.
- **Putting DB credentials in `cayenne-*.xml`** for production. Use the application's secret management; pass a `DataSource` to the builder.
- **Forgetting `commitChanges()`.** Mutations stay in memory until commit.
- **Forgetting `shutdown()`** on app exit. Hangs the JVM on some pool implementations.
