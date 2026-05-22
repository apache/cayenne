# Apache Cayenne Upgrade Guide — 4.2 and Older

> These notes cover Cayenne releases up to and including 4.2.
> For 5.0 and newer, see [UPGRADE.md](UPGRADE.md).

## Upgrading to 4.2.M2

* Per CAY-2659 All batch translators (`InsertBatchTranslator`, `UpdateBatchTranslator`, etc.) are updated
  to the new SQLBuilder utility. If you are using customized versions of these classes you should either
  update them accordingly, or keep using the old versions which are moved to the
  `org.apache.cayenne.access.translator.batch.legacy` package.

## Upgrading to 4.2.M1

* Per CAY-2520 `ObjectId` can't be instantiated directly — use `ObjectId.of(..)` methods instead:
  ```java
  ObjectId.of("Artist", 1)  // instead of new ObjectId("Artist", 1)
  ```

* Per CAY-2523 `SelectQuery` was deprecated. Use `ObjectSelect` instead.

* Per CAY-2525 OpenBase adapter was deprecated.

* Per CAY-2467 `Property` class is replaced with a type-aware `Property` API — mostly backwards
  compatible. To take advantage of the new API, regenerate code via Modeler
  ("Tools" → "Generate Classes") or cgen tools.

* Per CAY-2563 The following methods were deprecated:
  ```java
  SQLSelect.scalarQuery(Class<T>, String)
  SQLSelect.scalarQuery(Class<T>, String, String)
  ```

* Per CAY-2585 The following methods were deprecated:
  ```java
  SQLSelect.scalarQuery(String sql)
  SQLSelect.scalarQuery(String sql, String dataMapName)
  SQLSelect.scalarQuery(String sql, Class<?> firstType, Class<?>... types)
  SQLSelect.scalarQuery(String sql, String dataMapName, Class<?> firstType, Class<?>... types)
  SQLSelect.params(String name, Object value)
  ```

## Upgrading to 4.1.M3

* Per CAY-2514 `SERVER_CONTEXTS_SYNC_PROPERTY` default value was set to `false`.

## Upgrading to 4.1.M2

* Per CAY-2438 `DataChannelFilter` was deprecated and two new independent filters are introduced:
  `DataChannelSyncFilter` and `DataChannelQueryFilter`.

* Per CAY-2400 `cayenne-dbcp2` integration was deprecated.

* Per CAY-2377 All code deprecated in Cayenne 4.0 was removed — please review your code before upgrading.

* Per CAY-2372 Three new modules were extracted from Cayenne core. No changes to packages or API were
  made, so only include the additional modules in your project if you use them:
  - `cayenne-web` — contains all logic related to bootstrapping Cayenne inside a servlet container.
    `WebModule` is autoloaded, so you shouldn't add it to the runtime explicitly any more.
  - `cayenne-osgi` — contains OSGi related functionality.
  - `cayenne-rop-server` — ROP server part.

## Upgrading to 4.1.M1

* Per CAY-2351 Minimum supported Java version is now Java 8. There is no option to use Cayenne 4.1 with
  earlier versions — use 4.0 if Java 7 is required.

* Per CAY-2345 Velocity was replaced with Cayenne's own template engine by default in `cayenne-server`.
  This should be transparent in almost all cases; however if you relied on advanced Velocity features in
  `SQLTemplate` you can include the auto-loaded `cayenne-velocity` module to keep using Velocity — no
  other action required.

* Per CAY-2335 `ServerRuntime` by default will fail to load projects in case of version mismatch.
  You have two options:
  - Update project files by opening them in CayenneModeler.
  - Use the `cayenne-project-compatibility` module — add it as a dependency. Note: it only supports
    versions created by Cayenne 3.1 or later.

* Per CAY-2330 Field-based data objects are introduced and enabled by default. Your existing code will
  continue to work, but to get the benefits you should regenerate code via Modeler
  ("Tools" → "Generate Classes") or cgen tools in Maven/Ant/Gradle plugins. Also note that the
  serialization format of old data objects has changed — do not use serialized form to store objects.

## Upgrading to 4.0.B1

* Per CAY-2302 `postcommit` module and all its internals renamed to `commitlog`. The most important
  change is the new `@CommitLog` annotation which should be used instead of `@Auditable`. This change
  is backward incompatible and most likely to be missed, as the IDE won't give you a hint. Note that
  the new `@CommitLog` annotation is used only by the `commitlog` module — deprecated functionality
  in the `lifecycle` module still depends on `@Auditable`.

  Steps to update:
  1. Include `cayenne-commitlog` in your project (add a dependency to your `pom.xml`).
  2. Remove `cayenne-lifecycle` (and `cayenne-postcommit` if present) from your project.
  3. Switch usages of `@Auditable` to `@CommitLog`.
  4. Change usages of renamed classes:
     ```
     PostCommitListener      -> CommitLogListener
     PostCommitModuleBuilder -> CommitLogModuleExtender
     ```
  5. Fix all imports for renamed packages:
     ```
     org.apache.cayenne.lifecycle.postcommit -> org.apache.cayenne.commitlog
     org.apache.cayenne.lifecycle.changemap  -> org.apache.cayenne.commitlog.model
     ```
  6. Change `CommitLogModuleExtender` methods:
     ```
     auditableEntitiesOnly() -> commitLogAnnotationEntitiesOnly()
     build()                 -> module()
     ```

* Per CAY-2280 Cayenne migrated from commons-logging to SLF4J. Options:
  1. Migrate your logging to SLF4J. See https://www.slf4j.org for documentation.
  2. Use commons-logging over SLF4J to keep logging compatible with previous Cayenne versions:
     - Remove the `commons-logging` dependency if you have it.
     - Add `slf4j-jcl` as a dependency.

  As part of this change `CommonsJdbcEventLogger` and `FormattedCommonsJdbcEventLogger` were renamed to
  `Slf4jJdbcEventLogger` and `FormattedSlf4jJdbcEventLogger` respectively. They now use
  `org.apache.cayenne.log.JdbcEventLogger` as the logger name.

* Per CAY-2278 Packages `org.apache.cayenne.lifecycle.audit` and `org.apache.cayenne.lifecycle.changeset`
  were deprecated. Use the `cayenne-commitlog` module and its new `@CommitLog` annotation instead.

  Weighted graph sorter moved to `cayenne-server` into `org.apache.cayenne.ashwood` package.

  Packages `org.apache.cayenne.lifecycle.changemap` and `org.apache.cayenne.lifecycle.postcommit` were
  moved to the new `cayenne-commitlog` module. Update your code accordingly (see also notes for CAY-2302
  above).

* Per CAY-2277 `ClientRuntime` is now created with `ClientRuntimeBuilder` — direct instantiation of
  `ClientRuntime` is deprecated. Also the whole `ClientLocalRuntime` class is deprecated; use
  `ClientRuntimeBuilder.local()` instead.

* Per CAY-2262 Client modules are now auto-loaded by default. To turn off auto-loading use
  `ClientRuntimeBuilder.disableModulesAutoLoading()`.

  Client modules:
  - `cayenne-client`
  - `cayenne-client-jetty`
  - `cayenne-protostuff` (also supports auto-loading by `ServerRuntimeBuilder`)

  New modules extracted from existing ones — add to your `pom.xml` if you use this functionality:
  - `cayenne-cache-invalidation` (was part of `cayenne-lifecycle`)
  - `cayenne-commitlog` (was part of `cayenne-lifecycle`)

* Per CAY-2259 Cache invalidation module refactored. Changes:
  - Package `org.apache.cayenne.lifecycle.cache` renamed to `org.apache.cayenne.cache.invalidation`.
  - `CacheInvalidationModuleBuilder` renamed to `CacheInvalidationModuleExtender`; its `build()` method
    renamed to `module()`.
  - `InvalidationFunction` now returns `CacheGroupDescriptor` instead of a plain `String` with the cache
    group name — update your custom functions accordingly.

* Per CAY-2268 DI methods for binding ordered lists (introduced in 4.0.M3) were changed:
  - `after()` replaced by `addAfter()`, `addAllAfter()`
  - `before()` replaced by `insertBefore()`, `insertAllBefore()`

* Per CAY-2258 Injection of `List` and `Map` is now type-safe, introducing small incompatibilities.
  Change:
  ```java
  bindMap(String bindingName)
  bindList(String bindingName)
  ```
  to the type-safe versions:
  ```java
  bindMap(Class<T> valueType, String bindingName)
  bindList(Class<T> valueType, String bindingName)
  ```
  Also change DI keys like `Key.get(Map.class, "bindingName")` or `Key.get(List.class, "bindingName")`
  to the new factory methods `Key.mapOf(MapValues.class, "bindingName")` and
  `Key.listOf(ListValues.class, "bindingName")`.

  The new API also allows binding `List`s and `Map`s without names:
  ```java
  binder.bindList(SomeUniqueType.class).add(...);
  @Inject List<SomeUniqueType> list;
  ```

* Per CAY-1873 and CAY-2266 Cache and remote notification configuration was moved from Modeler into
  runtime DI settings. To set a custom cache size, use a custom module:
  ```java
  Module module = binder -> {
      ServerModule.setSnapshotCacheSize(binder, 20000);
  };
  ```
  Or via command line: `-Dcayenne.DataRowStore.snapshot.size=20000`

  If you use remote notifications, include one of these modules:
  - `cayenne-jgroups`
  - `cayenne-jms`
  - `cayenne-xmpp`

  For Maven, add a dependency, e.g.:
  ```xml
  <dependency>
      <groupId>org.apache.cayenne</groupId>
      <artifactId>cayenne-jgroups</artifactId>
      <version>4.0.M6</version>
  </dependency>
  ```

  The module will be autoloaded and remote notifications enabled — you only need to provide configuration
  via a custom DI module. For JGroups:
  ```java
  Module module = binder -> {
      JGroupsModule.contributeMulticastAddress(binder, MCAST_ADDRESS);
      JGroupsModule.contributeMulticastPort(binder, MCAST_PORT));
  };
  ```

* Per CAY-2256 Fix for CAY-2146 was reverted — it is not possible to reliably deduce whether a
  relationship is optional or not. For mandatory relationships in vertical inheritance, perform manual
  validation before insert using a `prePersist` callback (create it in CayenneModeler) or by overriding
  `validateForSave()`.

## Upgrading to 4.0.M5

* Per CAY-2186 `DerbyPkGenerator` switched from `AUTO_PK_TABLE` to a sequence-based PK generator.
  If you relied on `AUTO_PK_TABLE` usage in Derby, update your code.

* Per CAY-2228 Support for multiple cache groups has been removed from caching and query API, as none of
  the modern providers supports it. If you relied on this feature you should implement it yourself or
  change the caching provider.

* Per CAY-1980 `maven-cayenne-modeler-plugin` renamed to `cayenne-modeler-maven-plugin`.

* Per CAY-2225 `CacheInvalidationFilter` has been changed to support custom invalidation rules in addition
  to the rule based on `@CacheGroups`. If you have used it previously, change its binding in your custom
  module to use `CacheInvalidationModuleBuilder` instead of a direct binding.

* Per CAY-2212 `cdbimport` tool revisited — Maven plugin configuration should be updated:
  - `maven-cayenne-plugin` is deprecated; switch to `cayenne-maven-plugin`.
  - `<reverseEngineering>` tag replaced with `<dbimport>`.
  - New `<dataSource>` tag introduced that should enclose all connection properties:
    1. `<driver>`
    2. `<url>`
    3. `<user>`
    4. `<password>`
  - Top-level properties moved into `<dbimport>`:
    1. `<defaultPackage>`
    2. `<forceDataMapCatalog>`
    3. `<forceDataMapSchema>`
    4. `<meaningfulPkTables>`
    5. `<namingStrategy>`
    6. `<stripFromTableNames>`
    7. `<usePrimitives>`
  - Java 8 `java.time.*` types are now used by default in cdbimport (and in the "Reengineer Database
    Schema" tool in Modeler). Control this with `<useJava7Types>` in `<dbimport>` (or a checkbox in
    Modeler).
  - For Ant users: `cayenne-tools.jar` split into two parts:
    1. `cayenne-ant.jar` for Ant tasks
    2. `cayenne-cgen.jar` for class generation (required only for the cgen task)

* Per CAY-2166 Cayenne supports auto-loading of DI modules. Notable changes:
  - **Service override policy:** Custom modules now override "builder" modules (implicit modules wrapping
    builder method call customizations), since the builder is invoked explicitly during stack assembly
    while modules may be written without knowledge of the final stack.
  - **Module renaming:** If you see compile errors for `CayenneJodaModule` or `CayenneJava8Module`, just
    remove explicit loading — they will be auto-loaded if on the classpath. If auto-loading is disabled,
    use the new names: `JodaModule` and `Java8Module`.

* Per CAY-2164 Creating a `ServerRuntimeBuilder` is now done via `ServerRuntime.builder()` (static
  method). The previous style `ServerRuntimeBuilder.builder()` is deprecated.

## Upgrading to 4.0.M4

* Per CAY-2133 `LegacyObjectNameGenerator` is no longer provided, as it wasn't possible to maintain it
  in a fully backwards-compatible manner. Embrace the new naming scheme, or provide your own
  `ObjectNameGenerator` if you absolutely need the old names.

* Per CAY-2125 `SchemaUpdateStrategy` is no longer injected directly — `SchemaUpgradeStrategyFactory` is
  injected instead. If you have custom modules with `SchemaUpdateStrategy` injection they will be ignored.
  Update your DI code to use `SchemaUpgradeStrategyFactory` (or a subclass).

* Per CAY-2060 4.0.M4 changes how queries are stored in mapping files — all existing `*.map.xml` files
  should be upgraded. Open each project in the new CayenneModeler and agree to upgrade when prompted.

  `EntityResolver.getQuery(String)` was removed. Consider switching to `MappedSelect` or `MappedExec`,
  or use `EntityResolver.getQueryDescriptor(String).buildQuery()` if you need to retrieve a specific query.

* Per CAY-2065 `ROPHessianServlet` was replaced by `ROPServlet`. Update your `web.xml`:
  ```
  org.apache.cayenne.configuration.rop.server.ROPHessianServlet  ->  org.apache.cayenne.rop.ROPServlet
  ```

* Per CAY-2118 Several deprecated keys in cdbimport configuration were removed, along with the ability
  to set `reverseEngineering` config properties at the top level. In Maven you must always wrap them
  inside `<reverseEngineering>`. Removed top-level keys:
  1. `catalog`
  2. `schema` (also `schemaName`)
  3. `excludeTables`
  4. `includeTables`
  5. `procedurePattern`
  6. `tablePattern`
  7. `importProcedures`
  8. `meaningfulPk`
  9. `overwrite`

## Upgrading to 4.0.M3

* Per CAY-2026 Minimal Java version is now 1.7. Use Cayenne 3.1 or 4.0.M2 if your application requires
  Java 1.6.

* `@Deprecated` annotation is no longer added to generated String property names in entity superclasses.
  String property name inclusion became optional, controlled by the `createPropertyNames` flag in cgen
  (`false` by default). A similar option was added to the "Advanced" cgen dialog in CayenneModeler.
  If you have references to `@Deprecated` String properties and run cgen without `createPropertyNames`,
  there will be compile errors. See CAY-1991.

* Per CAY-2008, CAY-2009 `org.apache.cayenne.conn.PoolManager` and its associated classes were removed.
  A replacement pooling DataSource is available under `org.apache.cayenne.datasource` (`PoolingDataSource`,
  `ManagedPoolingDataSource`), best assembled using `org.apache.cayenne.datasource.DataSourceBuilder`.

* Per CAY-2012 API for `ObjectSelect` and `SelectById` was changed to remove "reset" functionality.
  Methods like `where`, `prefetch`, `orderBy` that previously reset the corresponding option state now
  work as "append". Methods that previously appended were removed as redundant. Revisit your code if you
  relied on reset behavior.

* If you are using `DBCPDataSourceFactory`, take the following steps:
  - Per CAY-2025 and CAY-2026, `DBCPDataSourceFactory` is now based on DBCP2 (required under Java 1.7+).
  - Check your DBCP properties file — property names must be supported by DBCP2. Remove any
    `cayenne.dbcp.` prefix if still present.
  - `DBCPDataSourceFactory` must now be explicitly included as a Cayenne module. For Maven:
    ```xml
    <parent>
      <groupId>org.apache.cayenne</groupId>
      <artifactId>cayenne-dbcp2</artifactId>
      <version>4.0.M3</version>
    </parent>
    ```

## Upgrading to 4.0.M2

* Note that the 3.2 line of development was renamed to 4.0 — 4.0.M2 is a direct descendant of 3.2M1.

* `org.apache.cayenne.map.naming.SmartNamingStrategy` was replaced with
  `org.apache.cayenne.map.naming.DefaultNameGenerator`. If you mentioned `SmartNamingStrategy`
  explicitly in Maven or Ant configs, rename it. Since this was the default, chances are you didn't.

* Minimal required JDK version is now 1.6.

* Managing listeners in the Modeler was removed per CAY-1842. If you have listeners in the model, delete
  them from XML and use annotations, registering them in runtime:
  ```java
  runtime.getDataDomain().addListener(myListener);
  ```

* `Cayenne.objectForSelect(Select)` (present in 3.2M1) was replaced with `ObjectContext.selectOne(Select)`.

* In-memory expression evaluation (`Expression.match`/`Expression.evaluate`) will now return `true` when
  matching a `Persistent` with an `ObjectId`, `Number`, or `String` (if those correspond to the object's
  `ObjectId`). Also, two objects in different `ObjectContexts` will match even if they have differing
  local changes — only their `ObjectId`s are compared. See CAY-1860 for details.

* `ResultIterator` was moved to `org.apache.cayenne` to make it available on both server and client. When
  upgrading related iterator code, review `ResultIterator` improvements: it implements `Iterable`, is no
  longer limited to `DataRows`, and no longer requires catching checked exceptions. Also see
  `ObjectContext.iterate(..)`.

* Transaction management was refactored significantly:
  - External transactions are no longer configured in the Modeler — they are provided as a DI property
    defined in `Constants.SERVER_EXTERNAL_TX_PROPERTY`.
  - `TransactionDelegate` is no longer present. Equivalent functionality can be achieved by writing a
    decorator for the `Transaction` interface and using a custom `TransactionFactory`.
  - If your code relied on `Transaction.externalTransaction()` or `Transaction.internalTransaction()`
    for manual transaction management, use constructors of `ExternalTransaction` and `CayenneTransaction`
    instead.

* When switching to `ServerRuntimeBuilder`, users of multi-config projects may assume it has the same
  behavior as 3.1 `ServerRuntime` for assigning the domain name (using the name of the last config).
  `ServerRuntimeBuilder` will only use the config name if there's a single config and no override —
  otherwise it uses the override, or `"cayenne"` as the default. See CAY-1972.

## Upgrading to 3.1.B1

* Per CAY-1665 all properties and DI collection keys were placed in a single `Constants` interface, with
  values following a single naming convention. Refer to
  https://issues.apache.org/jira/browse/CAY-1665 for the mapping between old and new names.

  If you are upgrading from an earlier 3.1 release, update your code and runtime parameters accordingly.

## Upgrading to 3.1.M3

* DataMap listeners are no longer supported. Use global listeners registered through the annotations API
  instead:
  ```java
  public class SomeListener {
      @PrePersist
      public void onPrePersist(Object object) {
          // callback method
      }
  }
  ```
  To register a listener:
  ```java
  runtime.getChannel().getEntityResolver().getCallbackRegistry().addListener(listenerInstance);
  ```
  Note that DataMap listener entries from old `*.map.xml` files will be ignored.

## Upgrading to 3.1.M1

The most essential change in Cayenne 3.1 is a new Dependency-Injection (DI) based bootstrap and
configuration mechanism, which is not backwards compatible with 3.0. Read on for specific areas
requiring attention.

* **Upgrading 3.0.x mapping files:** Open each project in the new CayenneModeler and agree to upgrade.
  Note that Cayenne 3.1 supports only one DataDomain per project — if multiple domains existed, you'll
  end up with multiple project files after the upgrade, each requiring a separate `ServerRuntime`.

* **Upgrading 2.0.x and earlier mapping files:** CayenneModeler 3.1 cannot upgrade projects created
  with a Modeler older than 3.0. Upgrade in two steps: download Cayenne 3.0 and upgrade there first,
  then upgrade from 3.0 to 3.1.

* **Cayenne runtime bootstrap:** All classes under `org.apache.cayenne.conf` were removed, superseded
  by DI-based configuration under `org.apache.cayenne.configuration` and its subpackages. To instantiate
  the Cayenne stack:
  ```java
  ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-UntitledDomain.xml");
  ```
  To obtain a new `ObjectContext`:
  ```java
  ObjectContext context = cayenneRuntime.getContext();
  ```

* **No static configuration singleton:** The `Configuration.sharedConfiguration` singleton was removed.
  Users must decide where to store their `ServerRuntime` instance — e.g., as a `ServletContext` attribute
  (see `org.apache.cayenne.configuration.web.CayenneFilter` and `WebUtil`), inside a DI container like
  Spring, or in a static singleton variable.

* **No static DataContext creation methods:** Methods like `DataContext.createDataContext()` relied on
  the removed static singleton. Use `ServerRuntime` instance methods to create contexts.

* **Webapp configuration changes:** `org.apache.cayenne.conf.WebApplicationContextFilter` was replaced
  by `org.apache.cayenne.configuration.web.CayenneFilter`. See its javadocs for init parameters.

* **ROP Server configuration changes:** `org.apache.cayenne.remote.hessian.service.HessianServlet` was
  replaced by `org.apache.cayenne.configuration.rop.server.ROPHessianServlet`. See its javadocs for
  init parameters.

* **ROP Client configuration changes:** There is now a DI "runtime" object —
  `org.apache.cayenne.configuration.rop.client.ClientRuntime` — so client connection and channel can
  be managed via DI with connection parameters specified as properties:
  ```java
  Map<String, String> properties = new HashMap<String, String>();
  properties.put(ClientModule.ROP_SERVICE_URL, "http://localhost:8080/tutorial/cayenne-service");
  properties.put(ClientModule.ROP_SERVICE_USER_NAME, "cayenne-user");
  properties.put(ClientModule.ROP_SERVICE_PASSWORD, "secret");

  ClientRuntime runtime = new ClientRuntime(properties);
  ObjectContext context = runtime.getContext();
  ```

* **Deprecated API removal:** All API deprecated as of 3.0 is removed. If custom class generation
  templates are used, check that they do not reference removed `EntityUtil` methods — these were replaced
  by variables placed directly into the Velocity context.

* **Custom DbAdapter / DbAdapterFactory:** The auto-detection interface changed from
  `org.apache.cayenne.dba.DbAdapterFactory` to `org.apache.cayenne.configuration.DbAdapterDetector`.
  Custom implementations can now rely on Cayenne DI for dependencies via `@Inject`. To register:
  ```java
  public void configure(Binder binder) {
      binder.bindList(DbAdapterFactory.class).add(new MyDbAdapterDetector());
  }
  ```

* **Custom DataSourceFactory:** The interface changed from `org.apache.cayenne.conf.DataSourceFactory`
  to `org.apache.cayenne.configuration.DataSourceFactory`. Custom implementations can use `@Inject`
  for Cayenne dependencies.

* **JNDI preferences hack replaced with runtime properties:** The "JNDI hack" (reading JNDI connection
  info from Modeler preferences) is no longer available. Instead, any `DataSourceFactory` (including
  JNDI) can be overridden via runtime properties. See
  `org.apache.cayenne.configuration.server.PropertyDataSourceFactory` javadocs. Examples:
  ```
  -Dcayenne.jdbc.url=jdbc://urloverride
  -Dcayenne.jdbc.driver=com.example.MyDriver
  -Dcayenne.jdbc.username=foo
  -Dcayenne.jdbc.password=bar
  ```

## Upgrading to 3.0.B1

* Per CAY-1281 the `pre-persist` callback was renamed to `post-add` (while `pre-persist` now has a
  different meaning). Open the project in the Modeler and agree to the automated upgrade.

## Upgrading to 3.0.M6

* Per CAY-1154 `org.apache.cayenne.access.reveng` was renamed to `org.apache.cayenne.map.naming`. If
  you use custom naming strategies, update accordingly.

* Per CAY-1161 The custom columns feature in `SelectQuery` was deprecated. Consider switching to EJBQL.
  Custom column support will likely go away completely after 3.0.M6.

* Per CAY-1175 The `columnNameCapitalization` property of `SQLTemplate` now takes an enum, not a
  `String` — fix the calling code.

## Upgrading to 3.0.M5

* Per CAY-1127 Query "name" is no longer used as an internal cache key. This change is transparent to
  most users, but if your code explicitly depends on the cache key value, update it:
  ```java
  String cacheKey = query.getQueryMetadata(entityResolver).getCacheKey();
  ```

## Upgrading to 3.0.M4

* Per CAY-1049 API of internal classes that participate in `SelectQuery` translation has changed in a
  backwards-incompatible way. This should not affect regular users, but if you implemented a custom
  `DbAdapter`, check classes that directly or indirectly inherit from `QueryAssembler` and
  `QueryAssemblerHelper`.

## Upgrading to 3.0.M3

* Java 5 is now required as a minimum for CayenneModeler and the Cayenne libraries.

* After the move to Java 5, generics have been implemented in many Cayenne APIs. If you don't use
  generics this should not affect you, but if you do you will need to review any new compiler errors
  or warnings. Generics only affect compile time — the runtime behaviour is unchanged.

## Upgrading to 3.0.M2

* Per CAY-843 Lifecycle callback functionality is now built into `DataContext` and `DataDomain` — all
  custom setup code is no longer needed. As a result of this change `org.apache.cayenne.intercept`
  package was removed. See http://cayenne.apache.org/doc/lifecycle-callbacks.html for details.

## Upgrading to 3.0.M1

* **Jar files:**
  - All jar files now include version numbers in their names.
  - `cayenne-nodeps.jar` is renamed to `cayenne-server-x.x.x.jar`.
  - The "fat" `cayenne.jar` (which included all dependencies) is no longer distributed. Use the new
    `cayenne-server-x.x.x.jar` plus the separate dependency jars under `cayenne-x.x.x/lib/third-party/`.
  - A new `cayenne-agent-x.x.x.jar` is included for class enhancement with POJOs and JPA. Classic
    Cayenne users can ignore it.

* Ant class generator now uses what was called "version 1.2" by default. If you were using custom
  Velocity templates in 1.1 mode, either change the templates or specify `version="1.1"` in the
  buildfile.

* Cross-platform Modeler startup is now done without a batch file or shell script. Run the included
  `CayenneModeler.jar` directly:
  - Double-click (on platforms that support it), or
  - `java -jar CayenneModeler.jar`

* FireBird adapter is no longer distributed with Cayenne.

* `DataContextTransactionEventListener`, `DataObjectTransactionEventListener` and `DataContextEvent`
  were deprecated in favor of callbacks. **This API will be removed in following 3.0 milestones.**

* Long PK: Cayenne now supports `long` primary key generation (previously only `int`). You may
  optionally change the PK lookup table to accommodate large IDs. On MySQL:
  ```sql
  ALTER TABLE AUTO_PK_SUPPORT CHANGE COLUMN NEXT_ID NEXT_ID BIGINT NOT NULL;
  ```

## Upgrading to 2.0.x

Since 2.0, Cayenne is an Apache project and all `org.objectstyle.*` packages were renamed to
`org.apache.*` analogues. Since the 1.2.x and 2.0.x release lines are fully compatible (differing
only in package names), upgrading to 2.0.x can be a first step in a safe upgrade to the latest version.

* **Upgrading mapping files:** Open them in the new Modeler — you will see an upgrade dialog. Confirm
  the upgrade.

* **Upgrading the code:** Replace `org.objectstyle.` with `org.apache.` everywhere in imports and do
  a clean recompile.

* **Upgrading logging configuration:** If you are using a custom logging configuration file, change all
  Cayenne loggers from `org.objectstyle` to `org.apache`.
