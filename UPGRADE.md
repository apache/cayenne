# Apache Cayenne Upgrade Guide

> For upgrade notes for Cayenne 4.2 and older, see [UPGRADE-4.2-and-older.md](UPGRADE-4.2-and-older.md).

## What's New in 5.0

This is a high-level overview of 5.0 changes. Check the next section for milestone-by-milestone upgrade instructions.

### New Dev Versioning Scheme

Snapshot versions are now a constant value — the dev version of 5.0 will always be `5.0-SNAPSHOT`,
so you can stay at the bleeding edge of development if needed:

```xml
<dependency>
    <groupId>org.apache.cayenne</groupId>
    <artifactId>cayenne</artifactId>
    <version>5.0-SNAPSHOT</version>
</dependency>
```

### New Class Generation UI

The new Class Generation UI in CayenneModeler simplifies configuration, allows multiple `cgen` setups
per project, and includes a template editor. Custom templates are now part of the project XML
configuration and don't require separate setup in either Modeler or Maven/Gradle plugins.

### Improved `(not)exists` Queries

`(not)exists` is now directly supported by the Expression API (including `Expression`, the expression
parser, and the Property API) — no need to construct a subquery manually. The feature can handle any
expression and spawn several sub-queries per expression if needed:

```java
long count = ObjectSelect.query(Artist.class)
        .where(Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE).like("painting%").exists())
        .selectCount(context);
```

### Improved SQL Support

`ANY` and `ALL` subqueries are now supported, as well as `case-when` expressions:

```java
import static org.apache.cayenne.exp.ExpressionFactory.*;
// ...
Expression caseWhenExp = caseWhen(
        List.of(betweenExp("estimatedPrice", 0, 9),
                betweenExp("estimatedPrice", 10, 20)),
        List.of(wrapScalarValue("low"),
                wrapScalarValue("high")),
        wrapScalarValue("error"));
```


## Upgrading to 5.0-M3


* Per [CAY-2912](https://issues.apache.org/jira/browse/CAY-2912) SQL logging was redesigned to be compact and
  single-line. The `JdbcEventLogger` interface and its `Slf4jJdbcEventLogger` / `FormattedSlf4jJdbcEventLogger`
  implementations were removed and replaced by `org.apache.cayenne.log.SqlLogger` (default implementation
  `Slf4jSqlLogger`). Log output now goes to a logger named `cayenne-sql` (previously `org.apache.cayenne.log.JdbcEventLogger`) —
  update your logging configuration accordingly. Each statement is logged as one line combining the SQL, its bindings
  and the result count (e.g. `... bind:[user_id:15] selected:1`); transaction boundaries moved to `DEBUG`. If you bound
  a custom `JdbcEventLogger` in a DI module, rebind `SqlLogger` instead.

  As part of this change the `cayenne.query_execution_time_logging_threshold` property no longer has any effect — the
  slow-query threshold warning it controlled has been removed. The `Constants.QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY`
  constant is retained (deprecated) but ignored. A new `cayenne.jdbc.log.batch.threshold` property (default 3) controls how
  many batch rows are logged in full before the bindings are truncated to `[first]..N..[last]`.

* Per [CAY-2954](https://issues.apache.org/jira/browse/CAY-2954) selecting queries are no longer wrapped in
transactions internally by Cayenne. Using connections in "auto-commit" mode instead has a significant positive impact 
on DB performance. This should not affect manually-managed transactions. But in theory, in some rare cases this may 
still change consistency behavior of disjoint prefetches (as multiple related selects will no longer be wrapped in a 
single transaction). We'd like to look at the actual cases to propose a mitigation approach, but one possible 
solution may be changing to "joint" prefetches.

* Per [CAY-2956](https://issues.apache.org/jira/browse/CAY-2956) the dedicated Oracle 8 adapter has been removed.
  `org.apache.cayenne.dba.oracle.Oracle8Adapter` and its supporting classes no longer exist, and the
  `OracleSniffer` now maps all Oracle versions to `OracleAdapter` regardless of the JDBC driver version.
  If you referenced `Oracle8Adapter` explicitly (e.g. in a DataNode adapter configuration or custom DI
  bindings), switch to `org.apache.cayenne.dba.oracle.OracleAdapter`.

* Per [CAY-2957](https://issues.apache.org/jira/browse/CAY-2957) the legacy HSQLDB adapter (HSQL <= 1.8) has been removed.
  `org.apache.cayenne.dba.hsqldb.HSQLDBNoSchemaAdapter` no longer exists, and the `HSQLDBSniffer` now maps all. If you
  happen to be on those older HSQL versions, update to the latest one.

* Per [CAY-2970](https://issues.apache.org/jira/browse/CAY-2970) deferred batch parameter values (e.g. a generated PK
  propagated to a dependent PK or FK within the same transaction) are now represented by the dedicated
  `org.apache.cayenne.access.DeferredValue` type instead of a bare `java.util.function.Supplier`. Cayenne now resolves
  only its own `DeferredValue` instances, leaving user-supplied `Supplier` attribute values untouched. If you have
  custom code that fed deferred values into batch bindings or `ObjectId` snapshots via `Supplier`, implement
  `DeferredValue` instead — it is a `@FunctionalInterface`, so an existing lambda or `Supplier` implementation can
  usually be adapted with a minimal change.

* Per [CAY-2985](https://issues.apache.org/jira/browse/CAY-2985) `DataDomain` became mostly immutable. The `DataDomain(String)` constructor and all the setters 
below were removed in favor of a single full constructor that takes every collaborator and setting. Only DataNodes, 
DataMaps, filters and listeners can still be added (and removed) after creation. Replacements for the removed setters:
  - `setName(String)` — the name comes from the project XML, and can be overridden with the
    `cayenne.domain.name` property (`Constants.DOMAIN_NAME_PROPERTY`).
  - `setEntityResolver(EntityResolver)` — keep using `addDataMap(..)` / `removeDataMap(..)` to change resolver contents.
  - `setEntitySorter(EntitySorter)` — the sorter is produced by the new `EntitySorterFactory` DI service. Bind your
    own `EntitySorterFactory` to replace it.
  - `setEventManager(EventManager)` — bind `EventManager` in a DI module instead.
  - `setQueryCache(QueryCache)` — bind `QueryCache` in a DI module instead.
  - `setSharedSnapshotCache(DataRowStore)`, `setDataRowStoreFactory(DataRowStoreFactory)` and
    `getDataRowStoreFactory()` — bind `DataRowStoreFactory` in a DI module to customize the cache.
  - `setSharedCacheEnabled(boolean)` — use the "Shared Cache" checkbox in the Modeler.
  - `setValidatingObjectsOnCommit(boolean)` — use the "Object Validation" checkbox in the Modeler.
  - `setMaxIdQualifierSize(int)` — use the `cayenne.max_id_qualifier_size` property
    (`Constants.MAX_ID_QUALIFIER_SIZE_PROPERTY`).

* Per [CAY-2986](https://issues.apache.org/jira/browse/CAY-2986) cgen now runs unconditionally. Previously it compared
  the DataMap file mtime against the mtime of the generated classes and skipped generation when the classes looked
  newer. That optimization saved very little (cgen is idempotent and fast) while regularly producing stale classes
  after project upgrades or when switching between machines and branches. Consequences:
  - The `force` flag is now a deprecated no-op — its former behavior is the only behavior.

## Upgrading to 5.0-M2

* Per [CAY-2947](https://issues.apache.org/jira/browse/CAY-2947) the `cayenne-commitlog` artifact has been removed. Commit log support is now part of the
  core `cayenne` artifact — no extra dependency needed. Migrate as follows:
  - Remove the `cayenne-commitlog` dependency from your build.
  - Replace `CommitLogModule.extend(binder).addListener(l)` with:
    ```java
    CoreModule.extend(binder).addCommitLogListener(l)
    ```
  - `excludeFromTransaction()` is now `excludeCommitLogFromTransaction()` on `CoreModuleExtender`.
  - Replace `@org.apache.cayenne.commitlog.CommitLog` on entity classes with
    `@org.apache.cayenne.annotation.CommitLog`.
  - The `CommitLogListener`, `ChangeMap`, `ObjectChange` and related model classes remain in the
    `org.apache.cayenne.commitlog` package (now part of the core artifact).

* Per [CAY-2935](https://issues.apache.org/jira/browse/CAY-2935) Minimum required Java version for Apache Cayenne 5.0 is 21.

* Per [CAY-2937](https://issues.apache.org/jira/browse/CAY-2937) the visual graph feature (entity layout diagrams) has been removed from CayenneModeler.
  Existing `.graph.xml` files will be automatically deleted and their references removed from
  `cayenne-project.xml` when a project is opened in the Modeler and upgraded to the newest format.

* Per [CAY-2859](https://issues.apache.org/jira/browse/CAY-2859) `SelectById` query factory methods are redesigned with a bunch of old methods deprecated —
  update your calls accordingly.

* Per [CAY-2917](https://issues.apache.org/jira/browse/CAY-2917) joins are generated in a different order in the Select SQL. This should not affect any
  logic except if your code relies on the generated SQL in any way.

* Per [CAY-2924](https://issues.apache.org/jira/browse/CAY-2924) the `org.apache.cayenne.map.event` package (mapping events and listener interfaces) was
  moved from the core to the CayenneModeler module — these events are not used at runtime. As part of this:
  - `DbEntity`, `ObjEntity` and `DataMap` no longer implement the `*Listener` interfaces and no longer
    expose the internal event-consumer methods (`dbEntityChanged`, `objEntityChanged`, `dbAttributeAdded`,
    `handleAttributeUpdate`, etc.).
  - New public rename APIs replace the previous "set name + fire change event" pattern:
    `DataMap.renameDbEntity(DbEntity, String)`, `DataMap.renameObjEntity(ObjEntity, String)`,
    `DbEntity.renameAttribute(DbAttribute, String)` and `DbEntity.renameRelationship(DbRelationship, String)`.
    Prefer these over `setName(...)` for renames, as they re-key the parent maps and update dependent
    references.
  - `DbAttribute.setPrimaryKey(boolean)` and `DbAttribute.setGenerated(boolean)` no longer fire events;
    they update their parent `DbEntity`'s cached collections via direct method calls, behavior-equivalent
    to before. If your application code subscribed to these mapping events at runtime, migrate to direct
    calls or to the Modeler.

* Per [CAY-2925](https://issues.apache.org/jira/browse/CAY-2925) the `cayenne-modeler-maven-plugin` was removed. Launch CayenneModeler from the downloaded
  distribution instead. A CLI option also exists for all platform flavors:
  ```
  java -jar CayenneModeler.jar path/to/cayenne-project.xml
  ```
  or on macOS:
  ```
  open CayenneModeler.app --args path/to/cayenne-project.xml
  ```

* Per [CAY-2955](https://issues.apache.org/jira/browse/CAY-2955) the obsolete `QueryEngine` abstraction (`org.apache.cayenne.access.QueryEngine`) has been removed.
  `DataNode` is now used directly wherever `QueryEngine` was previously referenced. So you must subclass `DataNode` 
  and override `performQueries()` if you previously implemented a custom `QueryEngine`.

## Upgrading to 5.0-M1

* Per [CAY-2737](https://issues.apache.org/jira/browse/CAY-2737) All code deprecated in Cayenne 4.1 and 4.2 was deleted — please review your code before
  upgrading. Most notable removals are `SelectQuery` and these Cayenne modules:
  - `cayenne-dbcp2`
  - `cayenne-joda`
  - `cayenne-client`
  - `cayenne-client-jetty`
  - `cayenne-protostuff`
  - `cayenne-rop-server`
  - `cayenne-web`
  - `cayenne-jgroups`
  - `cayenne-jms`
  - `cayenne-xmpp`

* Per [CAY-2742](https://issues.apache.org/jira/browse/CAY-2742) Minimum required Java version for Apache Cayenne is 11.

* Per [CAY-2747](https://issues.apache.org/jira/browse/CAY-2747) Cayenne XML schemas are updated — update your projects by opening them in the Modeler or
  using the `cayenne-project-compatibility` module.

* Per [CAY-2751](https://issues.apache.org/jira/browse/CAY-2751) There is no more JNDI DataSource provided by Cayenne, nor password encoding capabilities.
  If you need these, provide your own custom DataSource.

* Per [CAY-2752](https://issues.apache.org/jira/browse/CAY-2752) Code generation configuration has minor changes — review and update Maven, Gradle and Ant
  configs accordingly.

* Per [CAY-2772](https://issues.apache.org/jira/browse/CAY-2772) Module extension is done differently. This may result in compile errors in some module
  extensions. If you encounter those, change how you configure the modules, following this general pattern
  (using `CacheInvalidationModule` as an example):
  ```java
  CayenneRuntime.of(..)
      .addModule(b -> CacheInvalidationModule.extend(b).addHandler(MyHandler.class))
      .build();
  ```
  Two things to note: (1) a module-specific extender is created using an `extend(Binder)` method of the
  module, and (2) an extender does not produce a `Module` — instead it adds services directly to the
  `Binder`. So it is usually invoked within a lambda that produces a `Module`, or within an app `Module`.

* Per [CAY-2822](https://issues.apache.org/jira/browse/CAY-2822) `cayenne-server` module is renamed to `cayenne` — update your build scripts accordingly:
  ```xml
  <dependency>
      <groupId>org.apache.cayenne</groupId>
      <artifactId>cayenne</artifactId>
      <version>{version}</version>
  </dependency>
  ```

* Per [CAY-2823](https://issues.apache.org/jira/browse/CAY-2823) `ServerRuntime` is deprecated. Use `org.apache.cayenne.runtime.CayenneRuntime` instead.

* Per [CAY-2824](https://issues.apache.org/jira/browse/CAY-2824) `CayenneServerModuleProvider` was renamed to `CayenneRuntimeModuleProvider` and moved to
  the `org.apache.cayenne.runtime` package. If you are using the auto-loading mechanism for your custom
  modules, update your `META-INF/services` reference accordingly.

* Per [CAY-2825](https://issues.apache.org/jira/browse/CAY-2825) Package `org.apache.cayenne.configuration.server` was renamed to
  `org.apache.cayenne.configuration.runtime` — fix your imports accordingly.

* Per [CAY-2826](https://issues.apache.org/jira/browse/CAY-2826) `ServerModule` renamed to `CoreModule`. The new builder pattern combining both changes:
  ```java
  CayenneRuntime runtime = CayenneRuntime.of()
          .addConfig("cayenne-project.xml")
          .module(b -> CoreModule.extend(b).setProperty("some_property", "some_value"))
          .build();
  ```

* Per [CAY-2828](https://issues.apache.org/jira/browse/CAY-2828) The `server` prefix was removed from the names of runtime properties and named collections
  defined in `org.apache.cayenne.configuration.Constants`. Update references in code and in any scripts
  that use them as system properties.

* Per [CAY-2845](https://issues.apache.org/jira/browse/CAY-2845) `DataObject` interface and `BaseDataObject` class were deprecated and all logic moved to
  the `Persistent` interface and `PersistentObject` class. Regenerate model classes via the cgen tool in
  CayenneModeler or Maven/Gradle plugins.
