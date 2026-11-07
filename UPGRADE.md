# Apache Cayenne Upgrade Guide

> For upgrade notes for Cayenne 4.2 and older, see [UPGRADE-4.2-and-older.md](UPGRADE-4.2-and-older.md).

## Upgrading to 6.0-M1

*  Per [CAY-3055](https://issues.apache.org/jira/browse/CAY-3055) the `DbRelationship` "To Dep PK" flag is replaced
   with a "Foreign Key" flag on the opposite side: the flag now marks the relationship whose source columns hold the
   FK, instead of the master side of a PK-to-PK pair. In the DataMap XML `toDependentPK="true"` becomes
   `foreignKey="true"` on the reverse relationship, and every other to-one gets `foreignKey="true"`. This is a part
   of project version 15 (schema namespace `http://cayenne.apache.org/schema/15/dataMap`); projects are upgraded
   automatically, creating the reverse of a "To Dep PK" relationship when it is missing. In the API
   `isToDependentPK()`, `setToDependentPK(..)` and `isValidForDepPk()` are replaced with `isForeignKey()`,
   `setForeignKey(..)` and `isIdentifying()`.

*  Per [CAY-3060](https://issues.apache.org/jira/browse/CAY-3060) callbacks are no longer mapped on an ObjEntity in
   the Modeler. The upgrade removes them from the project, listing the affected methods. To keep an existing callback
   working, implement the callback interface of its event in the Persistent class (see CAY-3061 below).

*  Per [CAY-3061](https://issues.apache.org/jira/browse/CAY-3061) lifecycle annotations (`@PostAdd`, `@PrePersist`,
   etc.) are removed, together with the `LifecycleListener` interface and the annotation-based listener registration
   (`CoreModuleExtender.addListener(Object)`, `addListenerType(Class)`, `DataDomain.addListener(Object)`). The
   annotations are removed without deprecation, so their users get compilation errors pointing to the code that
   needs to change:

   * A Persistent class receives its own events by implementing the callback interfaces from
     `org.apache.cayenne.lifecycle`: `PostAddAware`, `PrePersistAware`, `PostPersistAware`, `PreUpdateAware`,
     `PostUpdateAware`, `PreRemoveAware`, `PostRemoveAware`, `PostLoadAware`. Each declares a single no-arg method,
     e.g. `onPostAdd()`. An interface may be implemented by a (mapped or unmapped) superclass shared by several
     entities; the callback is invoked once per object.

     ```java
     public class Order extends _Order implements PostAddAware {
         @Override
         public void onPostAdd() {
             ...
         }
     }
     ```

   * Listeners are registered per event type as method references or lambdas, for one or more entities (and their
     subclasses) passed as trailing varargs, or for all entities when no entity classes are given. The `value` and
     `entityAnnotations` filters of the annotations are replaced by listing the entity classes:

     ```java
     CoreModule.extend(binder)
         .addPostAddListener(listener::orderCreated, Order.class)
         .addPostRemoveListener(listener::objectRemoved, Order.class, Invoice.class)
         .addPostPersistListener(listener::objectCommitted);
     ```

     The same registrations are available at runtime via `DataDomain.getLifecycleEventsManager()`.

   * A `DataChannelSyncFilter` / `DataChannelQueryFilter` that used to have annotated listener methods overrides the
     new `init(LifecycleEventsManager)` default method instead. It is called once when the filter is added to the
     DataDomain, and registers the filter's listeners explicitly.

   * `org.apache.cayenne.Validating` is deprecated in favor of `org.apache.cayenne.lifecycle.Validating`.

*  Per [CAY-3062](https://issues.apache.org/jira/browse/CAY-3062) `SQLTemplate` is gone. Its descent (`SQLQuery`) does not support multiple parameter sets. The 
   preferred way to run raw SQL is `SQLQuery` that can be called multiple times, once per a set of parameters.

*  Per [CAY-3067](https://issues.apache.org/jira/browse/CAY-3067) selecting and non-selecting queries share a single
   shape. `Query` is now generic, `Query<T>`, with `T` being the type of the rows the query selects, and the selecting
   shortcuts (`select(..)`, `selectOne(..)`, `selectFirst(..)`, `iterate(..)`, `iterator(..)`, `batchIterator(..)`) and
   `execute(..)` are defined on `Query` itself as default methods. The `Select` interface is removed, and
   `ObjectContext` selecting methods take `Query<T>`.

   The user-facing queries are aligned with this shape:

   * `SQLSelect` and `SQLExec` are merged into `SQLQuery`, and are deprecated as its subclasses. `SQLQuery.query(sql)`
     and `SQLQuery.query(sql).dataMap(dataMapName)` replace `SQLExec.query(..)`, and the other `SQLSelect` factory methods are
     kept under the same names. There is no `updateBatch(..)`: batch counts are read from the
     `UpdateResult` items returned by `execute(..)`, as in `((UpdateResult) query.execute(context).getFirst()).counts()`.
   * `SQLSelect.map(..)` and `ColumnSelect.map(..)` are removed without a replacement. Map the selected rows with
     a `Stream`.
   * `MappedSelect` and `MappedExec` are merged into `MappedQuery`, and are deprecated as its subclasses.
     `MappedExec.update(..)`, that returned an `int[]`, is renamed to `updateBatch(..)`, as `update(..)` inherited from
     `Query` returns a single `int`. Batch counts are read from `execute(..)` the same way.
   * `ProcedureCall.call(..)` is deprecated in favor of the inherited `execute(..)`, and its `batchUpdate(..)` in
     favor of reading the `UpdateResult` items of `execute(..)`. `ProcedureCall.select(..)` no longer throws when the
     procedure produced no result sets, returning an empty list like every other query.

*  Per [CAY-3068](https://issues.apache.org/jira/browse/CAY-3068) a query is resolved ahead of its execution. `Query`
   has a single abstract method, `ResolvedQuery resolve(EntityResolver)`, returning an immutable description of the
   statements to run (`org.apache.cayenne.query.resolved`), and Cayenne routes, translates and executes the resolved
   query on its own. The `getMetaData(..)`, `route(..)` and `createSQLAction(..)` hooks are removed, together with
   `QueryMetadata` and its implementations. So:

   * A custom `Query` can no longer run its own JDBC code via `createSQLAction(..)`. It is a facade over a standard
     query instead: its `resolve(..)` builds an `ObjectSelect`, `SQLQuery`, `ProcedureQuery` or `MappedQuery` and
     returns the result of resolving that.
   * `SQLTemplate` is removed in favor of `SQLQuery`, The explicit
     `SQLResult` mapping of `SQLTemplate.setResult(..)` has no replacement; use `#result` directives or
     `resultColumnsTypes(..)` instead. `SQLQuery` getters (`getSql()`, `getParams()`, `getCacheStrategy()`, etc.) are
     removed: the query is write-only, and its settings are read from the `ResolvedSQL` it resolves to. The `SQLQuery`
     and `ProcedureQuery` constructors are no longer public; use the static factory methods (`query(..)`,
     `columnQuery(..)`, `entityQuery(..)`).
     `SQLTemplateDescriptor` is renamed
     `NamedSQLQuery` (the `sqlQuery` XML element is unchanged) and builds `SQLQuery`.
   * The setter-style `ProcedureQuery` is removed, and `ProcedureCall` takes over the name `ProcedureQuery`, keeping
     its fluent API. `ProcedureCall` remains as a deprecated
     subclass for one release.

*  `QueryDescriptor` and its subclasses are renamed: `QueryDescriptor` is now
   `NamedQuery`, `SelectQueryDescriptor` is `NamedSelectQuery`, `SQLQueryDescriptor` is `NamedSQLQuery` and
   `ProcedureQueryDescriptor` is `NamedProcedureQuery`. The `DataMap` accessors follow: `getNamedQuery(..)`,
   `addNamedQuery(..)`, `removeNamedQuery(..)`, `getNamedQueries()` and `getNamedQueryMap()` replace the
   `*QueryDescriptor*` methods. The XML format is unchanged.

*  Per [CAY-3069](https://issues.apache.org/jira/browse/CAY-3069) `ColumnSelect` and `FluentSelect` are merged into
   `ObjectSelect`. A single `ObjectSelect<T>` now selects the root objects, DataRows or columns, with `T` being the type
   of result row: `column(..)`, `columns(..)` and the aggregate shortcuts (`count()`, `min(..)`, etc.) retype the
   query in place, the same way `fetchDataRows()` always did. So:

   * Replace `ColumnSelect<E>` in declarations with `ObjectSelect<E>`, and `FluentSelect<?, ?>` with `ObjectSelect<?>`.
     The fluent chains themselves compile as before, except for the appending aggregates of `ColumnSelect`:
     `columns(A).count()` now selects the count alone, so list all the columns in one call, e.g.
     `columns(A, PropertyFactory.COUNT)`.
   * `column(..)`, `columns(..)` and the aggregates modify the query they are called on instead of returning a copy.
     `selectCount(..)` still leaves the query unchanged.
   * `ObjectSelect.parseColumn(..)` and `parseColumns(..)` are replaced by `parse(Class<T>, ..)` with the type of a
     column or `Object[].class`. The subquery methods (`ExpressionFactory.inExp(..)`, `all(..)`, `any(..)`,
     `BaseProperty.in(..)`, `ComparableProperty.ltAll(..)`, etc.) accept any `ObjectSelect`. That the subquery selects a
     single column is checked when the query is translated, for the fluent, the factory and the parsed forms alike.
     A query of the root objects stands for their ids, e.g. `Painting.ARTIST.in(ObjectSelect.query(Artist.class))`,
     provided the PK of the entity is a single column.
   * `isColumnQuery()` and `isSingleColumn()` are added to `ObjectSelect` to inspect the shape of a query, e.g. one
     returned by `NamedSelectQuery.getParsedQuery()`. The getters of the execution settings
     (`getCacheStrategy()`, `getCacheGroup()`, `getPageSize()`, `getStatementFetchSize()`, `getQueryTimeout()`,
     `isFetchingDataRows()`) are removed, like those of `SQLQuery`, and so are `getLimit()`, `getOffset()`,
     `getEntityType()` and `isDistinct()`: read them from the `ResolvedSelect` the query resolves to. The getters that describe the clauses of a parsed query String (`getWhere()`,
     `getColumns()`, `getOrderings()`, etc.) stay for now.
   * `QLSelectPrinter` is removed. A query prints itself in the query language with `ObjectSelect.toQL()` (or
     `appendQL(Appendable)`), and its `toString()` returns the same.
   * `where(..)`, `and(..)` and `or(..)` always work with the WHERE clause. Previously, once `having(..)` was called
     on a query, they silently appended to the HAVING clause instead. `having(..)` AND's its expression to any
     HAVING set before, and the new `havingAnd(..)` and `havingOr(..)` extend the HAVING clause the way `and(..)`
     and `or(..)` extend WHERE. So replace `having(a).and(b)` with `having(a).havingAnd(b)` (or `having(a).having(b)`),
     and `having(a).or(b)` with `having(a).havingOr(b)`.
   * An expression with a subquery (`exists(..)`, `in(..)` with a select, `all(..)`, `any(..)`) is temporarily not
     evaluable in memory, e.g. with `Expression.filterObjects(..)` or `match(..)`: it throws an `ExpressionException`.
     Previously an uncorrelated `exists` subquery was silently run against the database for every evaluated object,
     while the other shapes failed anyway. Until a proper in-memory evaluation is designed, such expressions are for
     query qualifiers only.

## Upgrading from 4.2 to 5.0

Cayenne 5.0 removes a lot of legacy API and changes the project structure. Read (or, more likely, feed the notes below 
to your agent) to understand the changes that may be required to be done in your mapping and code.

If you are upgrading from a version older than 4.2, follow [UPGRADE-4.2-and-older.md](UPGRADE-4.2-and-older.md)
first.

### Prerequisites and Build

*  Per [CAY-2935](https://issues.apache.org/jira/browse/CAY-2935) the minimum required Java version is 21.

*  Per [CAY-2737](https://issues.apache.org/jira/browse/CAY-2737) all code deprecated in Cayenne 4.1 and 4.2 was
   deleted. The most notable removal is `SelectQuery` (use `ObjectSelect`, see "Queries" below).

*  Per [CAY-2822](https://issues.apache.org/jira/browse/CAY-2822) the `cayenne-server` module is renamed to `cayenne`:
   ```xml
   <dependency>
       <groupId>org.apache.cayenne</groupId>
       <artifactId>cayenne</artifactId>
       <version>{version}</version>
   </dependency>
   ```

*  The `cayenne-project` module was split. The project XML upgrade handlers (`UpgradeHandler`, `UpgradeHandler_V10`
   and later, `UpgradeType`) moved to the core `cayenne` module, package `org.apache.cayenne.project.upgrade`. The rest
   (saving, validation and the file-level upgrade driver) was renamed to `cayenne-project-tools`, placed in the 
   `org.apache.cayenne.projecttools` package. In an unlikely event that your code relied on `cayenne-project`, update
   its coordinates.

*  Per [CAY-2947](https://issues.apache.org/jira/browse/CAY-2947) the `cayenne-commitlog` artifact has been removed.
   Commit log support is now part of the core `cayenne` artifact — no extra dependency needed. Migrate as follows:
   - Remove the `cayenne-commitlog` dependency from your build.
   - Replace `CommitLogModule.extend(binder).addListener(l).excludeFromTransaction()` with:
     ```java
     CoreModule.extend(binder).addCommitLogListener(l).excludeCommitLogFromTransaction();
     ```
   - Replace `@org.apache.cayenne.commitlog.CommitLog` on entity classes with
     `@org.apache.cayenne.annotation.CommitLog`.

*  The `groupId` of `cayenne-modeler` and `cayenne-wocompat` changed from `org.apache.cayenne.modeler` to
   `org.apache.cayenne`. Artifact ids and versions are unchanged. If you depend on any of them directly, update the
   coordinates:
   ```xml
   <dependency>
       <groupId>org.apache.cayenne</groupId>
       <artifactId>cayenne-modeler</artifactId>
       <version>{version}</version>
   </dependency>
   ```

*  The following modules were removed, following the general direction of focusing on the Cayenne core and dropping
   external integrations:
   - `cayenne-client`, `cayenne-client-jetty`, `cayenne-rop-server`, `cayenne-protostuff`, `cayenne-web` (ROP and
     web support)
   - `cayenne-jgroups`, `cayenne-jms`, `cayenne-xmpp` (remote event bridges)
   - `cayenne-dbcp2`, `cayenne-joda`
   - `cayenne-modeler-maven-plugin` ([CAY-2925](https://issues.apache.org/jira/browse/CAY-2925)). Launch
     CayenneModeler from the downloaded distribution instead. A CLI option exists for all platform flavors:
     ```
     java -jar CayenneModeler.jar path/to/cayenne-project.xml
     ```
   - `cayenne-osgi` ([CAY-3034](https://issues.apache.org/jira/browse/CAY-3034)). Users running Cayenne in an OSGi
     container are welcome to copy the module's code into their own project: it consisted of a few small classes that
     provide an OSGi-aware `ClassLoaderManager` and a `Provider<DataDomain>` that swaps the thread context ClassLoader
     while the `DataDomain` is created, bound via a `CayenneRuntime` module.
   - `cayenne-lifecycle` ([CAY-3035](https://issues.apache.org/jira/browse/CAY-3035)). It was an opinionated way of
     encoding `ObjectId`s as Strings (`IdCoder`, `EntityIdCoder`, `StringIdFetcher`), with an even more opinionated
     `@ObjectIdRelationship` annotation and `ObjectIdRelationshipFilter` on top of it to resolve "virtual"
     relationships stored as such String IDs. There's no appetite to keep supporting this in Cayenne. The users who
     still need it are welcome to grab the source code of `IdCoder` and friends from the Cayenne git history and
     incorporate it in their projects.
   - `cayenne-project-compatibility` ([CAY-3036](https://issues.apache.org/jira/browse/CAY-3036)). The runtime now
     upgrades older project XML on its own, see "Project XML and CayenneModeler" below.

### Project XML and CayenneModeler

*  The project XML version is bumped to 14. Projects are upgraded automatically, either in memory by the runtime, or
   permanently by CayenneModeler:
   - Per [CAY-3036](https://issues.apache.org/jira/browse/CAY-3036) the runtime upgrades project XML created by
     older Cayenne versions on its own, in memory and without extra dependencies, logging a warning that recommends a
     permanent upgrade in CayenneModeler. An upgrade that would drop a part of the project without a replacement
     (e.g. DataNodes, or a `SQLTemplate` with a fetch limit, see below) still fails to load; open such a project in
     CayenneModeler to upgrade it.
   - Per [CAY-3041](https://issues.apache.org/jira/browse/CAY-3041) the oldest project that can be upgraded is of
     version 9, i.e. the one created by Cayenne 4.0. The older projects (versions 6 to 8, created by Cayenne 3.1 or
     the early 4.0 milestones) must first be upgraded with CayenneModeler 4.2 or older.

*  The upgrade changes the XML format itself. Any custom code or scripts that read or generate the project XML must
   be updated:
   - Per [CAY-3041](https://issues.apache.org/jira/browse/CAY-3041) all tags and attributes use camelCase, so the
     hyphenated names were renamed: e.g. `data-map` is now `dataMap`, `db-entity` is `dbEntity` and
     `db-attribute-path` is `dbAttributePath`. The `project-version` attribute of the root tags was removed, as the
     version is already a part of the schema namespace (e.g. `http://cayenne.apache.org/schema/14/dataMap`). The
     boolean attributes of `dbAttribute` lost the "is" prefix (`isMandatory` is now `mandatory`, etc.), and the delete
     rules are stored in lowercase (`nullify`, `cascade`, `deny`).
   - Per [CAY-3042](https://issues.apache.org/jira/browse/CAY-3042) "domain" is now called "project" in the project
     XML, the API and CayenneModeler. The root tag of the project XML is `project` instead of `domain`.
   - Per [CAY-3037](https://issues.apache.org/jira/browse/CAY-3037) a `SelectQuery` in a DataMap is stored as a
     single query String in the syntax of `ObjectSelect.parse(..)`, instead of the root attributes and the separate
     `qualifier`, `ordering` and `prefetch` elements. CayenneModeler edits a mapped `ObjectSelect` as a query String.
     A query String that can't be parsed no longer fails the DataMap loading: the
     query fails when executed, and the project validator reports it.

*  The upgrade drops the parts of the project that have no replacement in 5.0, reporting each removal:
   - Per [CAY-2981](https://issues.apache.org/jira/browse/CAY-2981) DataNodes are no longer a part of the XML mapping.
     Every `<node>` element is dropped. DataNodes / DataSources are defined in runtime instead, see "Runtime Bootstrap
     and Dependency Injection" below. In CayenneModeler, the DataNode editors and the "Create DataNode" / "Link
     DataMap" actions were removed.
   - Per [CAY-2937](https://issues.apache.org/jira/browse/CAY-2937) the visual graph feature (entity layout diagrams)
     has been removed from CayenneModeler. Existing `.graph.xml` files are deleted and their references removed from
     `cayenne-project.xml`.
   - Per [CAY-3038](https://issues.apache.org/jira/browse/CAY-3038) an `EJBQLQuery` stored in a DataMap is dropped
     with a warning, as there is no automatic conversion of EJBQL to the new query language. It must be recreated as
     an `ObjectSelect` query String, see `EJBQLQuery` under "Queries" below.
   - Per [CAY-3040](https://issues.apache.org/jira/browse/CAY-3040) a fetch limit or offset of a `SQLTemplate`
     stored in a DataMap is dropped, and must be moved into the query SQL. A DataMap with such a query can not be
     upgraded in memory by the runtime; open the project in CayenneModeler to upgrade it.

*  Per [CAY-3042](https://issues.apache.org/jira/browse/CAY-3042) the project API follows the "domain" to "project"
   rename. `DataChannelDescriptor` was renamed to `Project`, `ConfigurationNode` to `ProjectNode`, and the related
   classes follow (`ProjectLoader`, `ProjectMerger`, `ProjectMetaData`, `ProjectNodeVisitor`, etc.). The old
   `org.apache.cayenne.project.Project` wrapper, `ProjectLoader` of the `cayenne-project` module and
   `ConfigurationTree` were removed: `ProjectLoader` returns a `Project`, and `ProjectSaver` takes a `ProjectNode`. The
   runtime `DataDomain` is not affected.

### Runtime Bootstrap and Dependency Injection

*  Per [CAY-2981](https://issues.apache.org/jira/browse/CAY-2981) DataNodes are no longer a part of the XML mapping.
   DataNodes / DataSources are defined in runtime, and DataMaps are linked to them in runtime as well. A node is 
   defined either from a bare `DataSource`, or from a `DataNodeDescriptor`. The `DataSource` flavor is a
   shortcut for a node that needs no customization — it gets a generated name and the default settings:
   ```java
   DataSource dataSource = CayenneDataSource.of("jdbc:postgresql://localhost:5432/mydb")
           .userName("user")
           .password("secret")
           .pool(1, 5)
           .build();

   CayenneRuntime runtime = CayenneRuntime.of()
           .addConfig("cayenne-project.xml")
           .defaultDataNode(dataSource)
           .build();
   ```

   Use a `DataNodeDescriptor` when the node has to be customized, e.g., to let Cayenne create the
   schema on the first connection, pin the adapter, give the node a stable name, etc.:
   ```java
   DataNodeDescriptor node = DataNodeDescriptor.of("node1")
                   .dataSource(dataSource)
                   .createSchemaIfNeeded()
                   .build();
   CayenneRuntime runtime = CayenneRuntime.of()
           .addConfig("cayenne-project.xml")
           .defaultDataNode(node)
           .build();
   ```

   Both flavors work with any number of nodes. Projects that used to declare several `<node>`s link each node to its
   DataMaps by name, mixing the two forms as needed. A default node remains optional, and picks up every DataMap not
   linked to a node explicitly:
   ```java
   CayenneRuntime runtime = CayenneRuntime.of()
           .addConfig("cayenne-project.xml")
           .addDataNode(ds1, "map1", "map2")
           .addDataNode(DataNodeDescriptor.of("node2").dataSource(ds2).createSchemaIfNeeded().build(), "map3")
           .defaultDataNode(ds3)
           .build();
   ```
   `cayenne.jdbc.*` properties are still recognized.

   Core API changes:
   - Nodes defined from a bare `DataSource` are named after the domain (`cayenne-0`, `cayenne-1`, ...). Use a
     `DataNodeDescriptor` if you need a stable name for `DataDomain.getDataNode(String)`,
     `CayenneRuntime.getDataSource(String)` or `SQLTemplate.setDataNodeName(String)`.
   - `org.apache.cayenne.configuration.runtime.DataSourceFactory` was removed and is no longer an extension point
   - `org.apache.cayenne.configuration.runtime.DbAdapterFactory` was removed and is no longer an extension point.
     A recommended way to add a custom adapter is `CoreModule.extend(binder).addAdapterDetector(...)`, or you can set
     it directly on DataNodeDescriptor
   - `org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory` was removed and is no longer an extension point.
     Set it directly on DataNodeDescriptor if needed.

*  Per [CAY-2823](https://issues.apache.org/jira/browse/CAY-2823) `ServerRuntime` is deprecated. Use
   `org.apache.cayenne.CayenneRuntime` instead. Per [CAY-2826](https://issues.apache.org/jira/browse/CAY-2826)
   `ServerModule` is renamed to `CoreModule`, and `CoreModule` / `CoreModuleExtender` live in the `org.apache.cayenne`
   package. The new builder pattern combining both changes:
   ```java
   CayenneRuntime runtime = CayenneRuntime.of()
           .addConfig("cayenne-project.xml")
           .addModule(b -> CoreModule.extend(b).setProperty("some_property", "some_value"))
           .build();
   ```

*  Per [CAY-2772](https://issues.apache.org/jira/browse/CAY-2772) most modules, including the core one, is now
   customized through a Binder-aware "extender", which is more convenient than the earlier "contributeXyz" and "extend"
   methods:
   ```java
   // before
   ServerRuntime.builder()
           .addModule(binder -> ServerModule.contributeUserTypes(binder).add(new MyType()))
           .addModule(CacheInvalidationModule.extend().addHandler(MyHandler.class).module())
           .build();

   // after
   CayenneRuntime.of()
           .addModule(b -> CoreModule.extend(b).addUserExtendedType(new MyType()))
           .addModule(b -> CacheInvalidationModule.extend(b).addHandler(MyHandler.class))
           .build();
   ```

*  Per [CAY-2824](https://issues.apache.org/jira/browse/CAY-2824) `CayenneServerModuleProvider` was renamed to
   `CayenneRuntimeModuleProvider` and moved to the `org.apache.cayenne` package. If you are using the auto-loading
   mechanism for your custom modules, update your `META-INF/services` reference accordingly.

*  Several core packages were renamed — fix your imports accordingly:

   | 4.2                                                                    | 5.0                                                                                   |
   |------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
   | `org.apache.cayenne.configuration.server` ([CAY-2825](https://issues.apache.org/jira/browse/CAY-2825)) | `org.apache.cayenne.configuration.runtime` (the deprecated `ServerRuntime` stays in `configuration.server`) |
   | `org.apache.cayenne.configuration.server.ServerModule`                 | `org.apache.cayenne.CoreModule`                                                       |
   | `org.apache.cayenne.configuration` and its `xml` and `upgrade` subpackages (`Project`, `ProjectLoader`, `DataNodeDescriptor`, etc.) | `org.apache.cayenne.project`                              |
   | `org.apache.cayenne.configuration.RuntimeProperties`, `DefaultRuntimeProperties` | `org.apache.cayenne.RuntimeProperties`, `DefaultRuntimeProperties`          |
   | `org.apache.cayenne.project` (the `cayenne-project` module)            | `org.apache.cayenne.projecttools` (the `cayenne-project-tools` module)                |

*  Per [CAY-2828](https://issues.apache.org/jira/browse/CAY-2828) the `server` prefix was removed from the names of
   runtime properties. Update deployment scripts, startup flags and config files that set them as system properties.
   The `cayenne.jdbc.*` properties and the other properties not listed here are unchanged:

   | 4.2                                                     | 5.0                                                                 |
   |---------------------------------------------------------|---------------------------------------------------------------------|
   | `cayenne.server.domain.name`                            | `cayenne.domain.name`                                               |
   | `cayenne.server.external_tx`                            | `cayenne.external_tx`                                               |
   | `cayenne.server.contexts_sync_strategy`                 | `cayenne.contexts_sync_strategy`                                    |
   | `cayenne.server.object_retain_strategy`                 | `cayenne.object_retain_strategy`                                    |
   | `cayenne.server.max_id_qualifier_size`                  | `cayenne.max_id_qualifier_size`                                     |
   | `cayenne.server.query_execution_time_logging_threshold` | `cayenne.query_execution_time_logging_threshold`, but ignored, see [CAY-2912](https://issues.apache.org/jira/browse/CAY-2912) below |
   | `cayenne.server.rop_event_bridge_factory`               | removed with ROP                                                    |

   The constants for the property names moved from `org.apache.cayenne.configuration.Constants` to
   `RuntimeProperties` (`EXTERNAL_TX_PROPERTY`, `MAX_ID_QUALIFIER_SIZE_PROPERTY`, etc.).

*  Per [CAY-2751](https://issues.apache.org/jira/browse/CAY-2751) there is no more JNDI DataSource provided by
   Cayenne, nor password encoding capabilities. If you need these, provide your own custom DataSource.

*  The thread-bound runtime is gone: `CayenneRuntime.bindThreadInjector(..)` and `getThreadInjector()` were
   removed without a replacement. Keep the `CayenneRuntime` in your application's own scope (a static holder, a DI
   singleton, a `ScopedValue`) and pass it or its contexts to the code that needs them. `BaseContext` was removed as
   well; its thread-bound `ObjectContext` moved to `DataContext.getThreadObjectContext()` /
   `bindThreadObjectContext(..)` and is deprecated for removal, so treat it the same way:
   ```java
   // before
   CayenneRuntime.bindThreadInjector(runtime.getInjector());
   BaseContext.bindThreadObjectContext(runtime.newContext());
   ObjectContext context = BaseContext.getThreadObjectContext();

   // after: no thread-bound runtime; thread-bound context still works, but is deprecated
   DataContext.bindThreadObjectContext(runtime.newContext());
   ObjectContext context = DataContext.getThreadObjectContext();
   ```

### Persistent Objects and Generated Code

*  Per [CAY-2845](https://issues.apache.org/jira/browse/CAY-2845) the `DataObject` interface and the `BaseDataObject`
   class were deprecated and all logic moved to the `Persistent` interface and the `PersistentObject` class.
   `CayenneDataObject` and `HybridDataObject` were removed; the default superclass of generated classes is
   `GenericPersistentObject`. A JSON mapper that introspects bean properties sees the same Cayenne getters on
   `PersistentObject` as before plus one more: `getObjectContext()`, `getObjectId()`, `getPersistenceState()` and
   now `getSnapshotVersion()`. Update Jackson mixins or `@JsonIgnore` lists that were written against `BaseDataObject`
   or `CayenneDataObject`.

*  `PersistentObjectList`, `PersistentObjectMap` and the other `ValueHolder`-based collections were removed with ROP.
   `ToManyList`, the to-many collection of a `PersistentObject`, no longer extends `PersistentObjectList`; declare
   and check against `ToManyList` or plain `List`.

*  Per [CAY-3020](https://issues.apache.org/jira/browse/CAY-3020) Cayenne no longer supports Java serialization of
   contexts, queries and mapping objects. `Persistent`, `ObjectContext`, `ObjectId`, `Query`, `Expression`, `Ordering`,
   `EntityResolver`, `DataMap`, etc. do not implement `java.io.Serializable` anymore, and the custom
   `readObject` / `writeObject` logic that re-attached deserialized contexts and objects to the runtime is gone. The
   class generation templates no longer emit `serialVersionUID`, `writeObject` / `readObject` and
   `writeState` / `readState`.

*  Class generation (`cgen`) changed in a few ways:
   - Custom templates are now part of the project XML configuration, edited in the Class Generation UI of
     CayenneModeler (which also allows multiple `cgen` setups per project), and don't require a separate setup in
     either the Modeler or the Maven / Gradle plugins. Per
     [CAY-2752](https://issues.apache.org/jira/browse/CAY-2752) the code generation configuration of the Maven, Gradle
     and Ant plugins has minor changes — review and update the configs accordingly.
   - Per [CAY-2986](https://issues.apache.org/jira/browse/CAY-2986) cgen now runs unconditionally. Previously it
     compared the DataMap file mtime against the mtime of the generated classes and skipped generation when the
     classes looked newer. That optimization saved very little (cgen is idempotent and fast) while regularly
     producing stale classes after project upgrades or when switching between machines and branches. The `force`
     flag is now a deprecated no-op — its former behavior is the only behavior.
   - Per [CAY-3023](https://issues.apache.org/jira/browse/CAY-3023) the templates emit `List<QueryResult>` instead of
     the old `QueryResult<?>` for the `perform*` methods of mapped exec queries (see "Queries" below).

*  Regenerate your model classes once the project XML is upgraded, via the cgen tool in CayenneModeler ("Tools" →
   "Generate Classes"), the Maven / Gradle plugins or the AI plugin. This picks up the `Persistent` superclass, the
   removed serialization code and the new query result types in one go.

### Queries

*  `SelectQuery`, deprecated in 4.2, was removed per [CAY-2737](https://issues.apache.org/jira/browse/CAY-2737). Use
   `ObjectSelect` instead.

*  Per [CAY-3038](https://issues.apache.org/jira/browse/CAY-3038) `EJBQLQuery` was removed. The replacement is an
   `ObjectSelect` or `ColumnSelect` parsed from a String with `ObjectSelect.parse(..)` / `parseColumns(..)`. The query
   language (QL) follows the structure of those two queries, and the parser produces a regular `ObjectSelect` or
   `ColumnSelect`, the same as the one built with the fluent API. There is no automatic conversion of EJBQL to QL, so
   each query must be rewritten by hand:
   ```java
   // before
   EJBQLQuery<Artist> query = new EJBQLQuery<>(
           "select a from Artist a where a.artistName like :name order by a.dateOfBirth desc");
   query.setParameter("name", "A%");
   List<Artist> artists = context.select(query);

   // after
   List<Artist> artists = ObjectSelect
           .parse(Artist.class, "from Artist where artistName like $name order by dateOfBirth desc", "A%")
           .select(context);

   // parameters bound by name, the equivalent of EJBQLQuery.setParameter(name, value). A parameter without a value
   // is dropped from the query together with its condition
   List<Artist> artists = ObjectSelect
           .parse(Artist.class, "from Artist where artistName like $name and dateOfBirth > $born", Map.of("name", "A%"))
           .select(context);

   // column queries, aggregates and subqueries
   List<Object[]> counts = ObjectSelect
           .parseColumns("select self, count(paintingArray) from Artist having count(paintingArray) > 2")
           .select(context);

   List<Artist> withExpensivePaintings = ObjectSelect
           .parse(Artist.class, """
                   from Artist where exists (
                       from Painting where toArtist = enclosing(self) and estimatedPrice > $price)""", 1000)
           .select(context);
   ```
   QL is a select-only language. The EJBQL bulk `UPDATE` and `DELETE` statements have no replacement; rewrite them as
   SQL run via `SQLExec`:
   ```java
   // before
   context.performGenericQuery(new EJBQLQuery("delete from Painting p where p.estimatedPrice < 100"));

   // after
   SQLExec.query("DELETE FROM PAINTING WHERE ESTIMATED_PRICE < 100").execute(context);
   ```
   An `EJBQLQuery` stored in a DataMap is dropped by the project upgrade with a warning, and must be recreated in
   CayenneModeler as an `ObjectSelect` query String.

*  Per [CAY-3007](https://issues.apache.org/jira/browse/CAY-3007) `SelectById` is deprecated. Instead, use
   `ObjectSelect` directly with the new `byId(..)` and `byIds(..)` methods:
   ```java
   // before
   Artist a = SelectById.query(Artist.class, 42).selectOne(ctx);
   List<Artist> list = SelectById.query(Artist.class, 1, 2, 3).select(ctx);
   DataRow row = SelectById.dataRowQuery(Artist.class, 42).selectOne(ctx);

   // after
   Artist a = ObjectSelect.query(Artist.class).byId(42).selectOne(ctx);
   List<Artist> list = ObjectSelect.query(Artist.class).byIds(1, 2, 3).select(ctx);
   DataRow row = ObjectSelect.query(Artist.class).byId(42).fetchDataRows().selectOne(ctx);
   ```
   The same matches are available as expressions via the entity `SELF` property (`Artist.SELF.eqId(42)`,
   `Artist.SELF.idsIn(1, 2, 3)`, `Artist.SELF.eqIdMap(map)`, etc.) and on to-one relationship properties.

*  Per [CAY-3022](https://issues.apache.org/jira/browse/CAY-3022) the `ObjectContext` query API was cleaned up:
   - `ObjectContext.performGenericQuery(Query)` was renamed to `execute(Query)`.
   - `ObjectContext.performQuery(Query)` and `Cayenne.objectForQuery(ObjectContext, Query)` are deprecated in favor
     of `ObjectContext.select(Select)` and `ObjectContext.selectOne(Select)`. `SQLTemplate` and `ProcedureQuery` now
     implement `Select<T>`, so every selecting query can be passed to `select(..)`.
   - The `DataContext` convenience methods `performNonSelectingQuery(..)` and `performQuery(String, ..)` (all
     flavors) are deprecated. Use `execute(Query)`, `update(Query)`, and
     `MappedSelect.query(name).select(context)` instead.
   - `DataContext.performIteratedQuery(Query)` was removed. Use `ObjectContext.iterator(Select)` instead; to iterate
     over `DataRow`s, pass a `DataRow` query, e.g. `ObjectSelect.dataRowQuery(Artist.class)` or
     `SQLSelect.dataRowQuery(sql)`.

   ```java
   // before
   List<Artist> artists = context.performQuery(new SQLTemplate(Artist.class, "SELECT * FROM ARTIST"));
   QueryResponse response = context.performGenericQuery(SQLExec.query("DELETE FROM ARTIST"));

   // after
   List<Artist> artists = context.select(new SQLTemplate<>(Artist.class, "SELECT * FROM ARTIST"));
   List<QueryResult> result = context.execute(SQLExec.query("DELETE FROM ARTIST"));
   ```

*  Per [CAY-3023](https://issues.apache.org/jira/browse/CAY-3023) `QueryResult` was redesigned from a class holding
   the entire multipart result to a sealed interface describing a single item of it. `SQLExec.execute(..)`,
   `MappedExec.execute(..)` and `ProcedureCall.call(..)` now return a `List<QueryResult>` holding the items in the
   order the query produced them, so callers that know the shape of their query should access them by index.
   `QueryResult` has four permitted subtypes: the `SelectResult`, `UpdateResult` and `OutParametersResult` records,
   and `ResultIterator`, reported directly as an item of an iterated query. They replace the `isSelectResult()` /
   `getSelectResult()` / `getUpdateCount()` accessors, so a multipart result can also be scanned with a
   pattern-matching switch:
   ```java
   List<QueryResult> result = SQLExec.query(sql).execute(context);
   int updated = ((UpdateResult) result.getFirst()).count();

   for (QueryResult item : result) {
       switch (item) {
           case SelectResult<?> select -> process(select.objects());
           case UpdateResult update -> process(update.counts());
           case ResultIterator<?> iterator -> process(iterator);
           case OutParametersResult out -> process(out.values());
       }
   }
   ```
   Stored procedure OUT parameters are no longer disguised as a one-row result set. They arrive as a dedicated
   `OutParametersResult` item (a map keyed by parameter name).

*  Per [CAY-3024](https://issues.apache.org/jira/browse/CAY-3024) `org.apache.cayenne.QueryResponse` was removed.
   Everything that used to return a `QueryResponse` - `ObjectContext.performGenericQuery(..)`, etc. - now returns
   `List<QueryResult>`. See CAY-3023 above for the example of how to process the result. Custom
   `DataChannelQueryFilter` implementations now require a new signature (see "Stack Customizations" below).

*  Per [CAY-3014](https://issues.apache.org/jira/browse/CAY-3014) `org.apache.cayenne.query.QueryChain` was removed.
   To run several queries together, execute them one by one, wrapping them in an explicit transaction if they must
   share one:
   ```java
   runtime.performInTransaction(() -> {
       List<Artist> artists = ObjectSelect.query(Artist.class).select(context);
       List<Painting> paintings = ObjectSelect.query(Painting.class).select(context);
       return null;
   });
   ```

*  Per [CAY-3015](https://issues.apache.org/jira/browse/CAY-3015) `org.apache.cayenne.query.RefreshQuery` was
   removed. It was not a query, but a carrier for four cache management operations, each of which has a direct
   replacement:
   - `new RefreshQuery(objects)` / `new RefreshQuery(object)` &rarr; `context.invalidateObjects(objects)`. In nested
     contexts, there was previously an issue with cascading invalidation (intermediate contexts would not get
     invalidated). This was fixed in the current version of `invalidateObjects`.
   - `new RefreshQuery("group1", "group2")` &rarr; `runtime.getDataDomain().getQueryCache().removeGroup("group1")`,
     once per group.
   - `new RefreshQuery(query)` &rarr; run the query itself, with its cache strategy set to `LOCAL_CACHE_REFRESH` or
     `SHARED_CACHE_REFRESH`. A query with no caching needs no change, as refreshing it was the same as running it:
     ```java
     ObjectSelect.query(Artist.class)
             .cacheStrategy(QueryCacheStrategy.SHARED_CACHE_REFRESH)
             .cacheGroup("artists")
             .select(context);
     ```
   - `new RefreshQuery()` (refresh everything) has no direct replacement. To start over, create a new
     `ObjectContext`. To also drop shared state, call `runtime.getDataDomain().getQueryCache().clear()` and
     `runtime.getDataDomain().getSharedSnapshotCache().clear()`.

*  Per [CAY-3016](https://issues.apache.org/jira/browse/CAY-3016) `org.apache.cayenne.query.RelationshipQuery` was
   removed. It was an internal mechanism for resolving relationship faults. If you were running it explicitly, use
   `ObjectSelect` matching on the reverse relationship instead:
   ```java
   // before
   // List<Painting> paintings = context.performQuery(new RelationshipQuery(artist.getObjectId(), "paintingArray"));

   // after
   List<Painting> paintings = ObjectSelect.query(Painting.class)
           .where(Painting.TO_ARTIST.eq(artist))
           .select(context);
   ```

*  Per [CAY-3021](https://issues.apache.org/jira/browse/CAY-3021) `org.apache.cayenne.query.ObjectIdQuery` was
   removed. It was an internal mechanism for resolving objects by id via the caches. Its use cases have direct
   replacements:
   ```java
   // object lookup via the caches (ObjectIdQuery.CACHE)
   Artist a = (Artist) context.objectForPK(id);

   // forced refresh from the database (ObjectIdQuery.CACHE_REFRESH)
   Artist a = ObjectSelect.query(Artist.class).where(Artist.SELF.eqId(id)).selectOne(context);

   // committed snapshot lookup (fetchingDataRows == true), from the snapshot cache or the database
   DataRow row = context.getObjectStore().getSnapshot(id);

   // committed snapshot lookup in the snapshot cache only (ObjectIdQuery.CACHE_NOREFRESH)
   DataRow row = context.getObjectStore().getCachedSnapshot(id);
   ```

*  Per [CAY-3032](https://issues.apache.org/jira/browse/CAY-3032) a `ColumnSelect` with an entity column over a
   to-many relationship no longer filters out the root rows that have no related objects. It now uses an outer join,
   same as the to-one entity columns always did, so such rows are returned with a `null` in place of the related
   object:
   ```java
   List<Object[]> rows = ObjectSelect
       .columnQuery(Artist.class, Artist.ARTIST_NAME, Artist.PAINTING_ARRAY.flat())
       .select(context);

   // before: artists with no paintings were missing from the result
   // after: they are present as ["artist name", null]
   ```
   If you relied on the old behavior, check the related object for `null`, or exclude such rows explicitly by
   adding a condition on the relationship path to `where(..)` (e.g. `Artist.PAINTING_ARRAY.exists()`). Entity columns
   whose path is also used in the `where(..)` clause are not affected, as they share the join with the qualifier.
   Scalar columns (`Artist.PAINTING_ARRAY.dot(Painting.PAINTING_TITLE)`) and aggregates are not affected either.

*  Per [CAY-3050](https://issues.apache.org/jira/browse/CAY-3050) `DataRow` is now immutable: `put`, `putAll`,
   `remove` and `clear` throw `UnsupportedOperationException`, including on rows returned by `dataRows()` queries.

### Expressions

*  Per [CAY-3039](https://issues.apache.org/jira/browse/CAY-3039) the expression node classes are no longer tied to
   the parser. They moved from `org.apache.cayenne.exp.parser` to `org.apache.cayenne.exp` and lost the `AST` prefix:
   `ASTObjPath` is now `ObjPathExp`, `ASTEqual` is `EqualExp`, `ASTAnd` is `AndExp`, etc. The rename is mechanical
   and can be done with a search and replace: drop the `AST` prefix and append `Exp`. The only 4.2 node classes
   without a counterpart are the parser-internal `ASTEnum` and `ASTNamedParameter`: enum literals and `$name`
   placeholders are now plain operands (`EnumRef`, `ExpressionParameter`) of the enclosing node.

   Expression nodes no longer know their parent, so `jjtGetParent()` / `jjtSetParent(n)` have no replacement. As a
   result an expression can be shared between trees: `a.andExp(b)` no longer changes how `a` itself prints, and a node
   taken out of a tree prints without the parentheses it used to keep.

   The node classes are now a sealed hierarchy, and the kind of node is its class. `Expression.getType()`,
   `Expression.setType(..)` and the `Expression.OBJ_PATH`, `Expression.EQUAL_TO`, etc. int constants are removed
   without replacement: use `instanceof` or a pattern-matching `switch` over `ObjPathExp`, `EqualExp`, etc. instead.
   This also removed the int-typed factory methods: `ExpressionFactory.expressionOfType(int)` and
   `ExpressionFactory.joinExp(int, ..)` are gone (use `ExpressionFactory.and(..)` / `or(..)`), and so are
   `Expression.joinExp(int, ..)` (use `andExp(..)` / `orExp(..)`). `ExpressionFactory.matchAllExp(Map, int)`,
   `matchAnyExp(Map, int)`, `matchAllDbExp(Map, int)` and `matchAnyDbExp(Map, int)` lost the pair type parameter and
   always match by equality.

### Database and SQL Behavior

*  Per [CAY-2954](https://issues.apache.org/jira/browse/CAY-2954) selecting queries are no longer wrapped in
   transactions internally by Cayenne. Using connections in "auto-commit" mode instead has a significant positive
   impact on DB performance. This should not affect manually-managed transactions. But in theory, in some rare cases
   this may still change consistency behavior of disjoint prefetches (as multiple related selects will no longer be
   wrapped in a single transaction). We'd like to look at the actual cases to propose a mitigation approach, but one
   possible solution may be changing to "joint" prefetches.

*  Per [CAY-2917](https://issues.apache.org/jira/browse/CAY-2917) joins are generated in a different order in the
   Select SQL. This should not affect any logic except if your code relies on the generated SQL in any way.

*  Per [CAY-2912](https://issues.apache.org/jira/browse/CAY-2912) SQL logging was redesigned to be compact and
   single-line. The `JdbcEventLogger` interface and its `Slf4jJdbcEventLogger` / `FormattedSlf4jJdbcEventLogger`
   implementations were removed and replaced by `org.apache.cayenne.log.SqlLogger` (default implementation
   `Slf4jSqlLogger`). Log output now goes to a logger named `cayenne-sql` (previously
   `org.apache.cayenne.log.JdbcEventLogger`) — update your logging configuration accordingly. Each statement is logged
   as one line combining the SQL, its bindings and the result count (e.g. `... bind:[user_id:15] selected:1`);
   transaction boundaries moved to `DEBUG`. If you bound a custom `JdbcEventLogger` in a DI module, rebind `SqlLogger`
   instead.

   As part of this change the `cayenne.query_execution_time_logging_threshold` property no longer has any effect —
   the slow-query threshold warning it controlled has been removed. The
   `RuntimeProperties.QUERY_EXECUTION_TIME_LOGGING_THRESHOLD_PROPERTY` constant is retained (deprecated) but ignored.
   A new `cayenne.jdbc.log.batch.threshold` property (default 3) controls how many batch rows are logged in full
   before the bindings are truncated to `[first]..N..[last]`.

*  Per [CAY-2875](https://issues.apache.org/jira/browse/CAY-2875) a `java.util.UUID` attribute now honors the JDBC
   type of the column it is mapped to, instead of always being converted to a 36-char string:
   - A binary column (`BINARY`, `VARBINARY`, `LONGVARBINARY`) stores the UUID in its 16-byte big-endian form, most
     significant bits first. `VARBINARY(16)` is the recommended mapping for new models — it uses less than half the
     space of `VARCHAR(36)` and indexes better.
   - A character column keeps storing the canonical string, so existing models are unaffected and need no data
     migration.
   - Any other JDBC type is passed to the driver as an object, which makes a PostgreSQL native `uuid` column work.

   `UUIDValueType` is deprecated and no longer registered, replaced by `org.apache.cayenne.access.types.UUIDType`.

*  Per [CAY-3010](https://issues.apache.org/jira/browse/CAY-3010) `LocalDate`, `LocalTime` and `LocalDateTime` are
   passed to and from the JDBC driver directly, instead of being converted through `java.sql.Date` / `Time` /
   `Timestamp` in the JVM default time zone. Reads are faster, and application behavior changes in these cases:
   - A `LocalDateTime` inside a DST gap of the JVM zone is no longer shifted. E.g. with the JVM in `America/New_York`,
     `LocalDateTime.of(2021, 3, 14, 2, 35)` (a time that does not exist in that zone) used to be stored and read back
     as `03:35`; now it round-trips as `02:35`. MySQL already behaved this way.
   - Values no longer depend on the JVM default zone: a value stored under one zone and read under another comes back
     unchanged. Previously it was shifted by the offset difference on all databases except MySQL.
   - Sub-millisecond precision is no longer truncated on write. `LocalTime.of(13, 45, 30, 123_456_000)` used to reach
     the database as `13:45:30.123`; now it is sent as is and the database rounds to the column precision, so a
     `TIME(3)` column may store `13:45:30.123` or `13:45:30.124` depending on the database.
   - `java.time` attributes now work on SQLite, stored as `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd` and `HH:mm:ss` text
     with a seconds fraction appended only when non-zero.

   Exceptions: PostgreSQL `timestamp with time zone` columns (Cayenne's default mapping of `TIMESTAMP`) are an instant
   type and keep the previous JVM-zone conversion; Derby and SQL Server still shift DST-gap timestamps on write due to
   engine / driver limitations. `LocalDateValueType`, `LocalTimeValueType` and `LocalDateTimeValueType` are deprecated
   and no longer registered.

*  Two legacy database adapters were removed:
   - Per [CAY-2956](https://issues.apache.org/jira/browse/CAY-2956) the dedicated Oracle 8 adapter.
     `org.apache.cayenne.dba.oracle.Oracle8Adapter` and its supporting classes no longer exist, and the
     `OracleSniffer` now maps all Oracle versions to `OracleAdapter` regardless of the JDBC driver version. If you
     referenced `Oracle8Adapter` explicitly (e.g. in a DataNode adapter configuration or custom DI bindings), switch
     to `org.apache.cayenne.dba.oracle.OracleAdapter`.
   - Per [CAY-2957](https://issues.apache.org/jira/browse/CAY-2957) the legacy HSQLDB adapter (HSQL <= 1.8).
     `org.apache.cayenne.dba.hsqldb.HSQLDBNoSchemaAdapter` no longer exists, and the `HSQLDBSniffer` now maps all
     HSQL versions to `HSQLDBAdapter`. If you happen to be on those older HSQL versions, update to the latest one.

### Stack Customizations

This section only matters if you customize the Cayenne stack: bind your own services in a DI module, subclass
`DbAdapter`, `DataContext`, `DataDomain` or the SQL translators, or implement `DataChannel` filters. A plain
application can skip it.

*  The supported customization surface is `CoreModule.extend(binder)`. Anything
   it doesn't cover is done by overriding a `CoreModule` binding in your own module, which is loaded after the core
   one: `binder.bind(SomeService.class).to(MyService.class)`. The 4.2 hooks map as follows:

   | 4.2                                                                | 5.0                                                                   |
   |--------------------------------------------------------------------|-----------------------------------------------------------------------|
   | `ServerModule.contributeUserTypes(binder)`                         | `addUserExtendedType(..)`                                             |
   | `ServerModule.contributeDefaultTypes(binder)`                      | `addDefaultExtendedType(..)`                                          |
   | `ServerModule.contributeTypeFactories(binder)`                     | `addExtendedTypeFactory(..)`                                          |
   | `ServerModule.contributeValueObjectTypes(binder)`                  | `addValueObjectType(..)`                                              |
   | `ServerModule.contributeAdapterDetectors(binder)`                  | `addAdapterDetector(..)`                                              |
   | `ServerModule.contributeDomainQueryFilters(binder)`                | `addQueryFilter(..)`                                                  |
   | `ServerModule.contributeDomainSyncFilters(binder)`                 | `addSyncFilter(..)`                                                   |
   | `ServerModule.contributeDomainFilters(binder)` (`DataChannelFilter`) | removed; implement `DataChannelQueryFilter` and / or `DataChannelSyncFilter` instead |
   | `ServerModule.contributeDomainListeners(binder)`                   | `addListener(..)` / `addListenerType(..)`                             |
   | `ServerModule.contributeProjectLocations(binder)`                  | `addProjectLocation(..)`                                              |
   | `ServerModule.contributeProperties(binder)`                        | `setProperty(..)`                                                     |
   | `ServerModule.useExternalTransactions(binder, true)`               | `externalTransactions()`                                              |
   | `ServerModule.setSnapshotCacheSize(binder, n)`                     | `snapshotCacheSize(n)`                                                |
   | `ServerModule.contributePkGenerators(binder)`                      | removed; see `PkGenerator` below                                      |
   | `bind(DataSourceFactory.class)`                                    | removed; `DataNodeDescriptor.of(..).dataSource(..)`                   |
   | `bind(DbAdapterFactory.class)`                                     | removed; `addAdapterDetector(..)` or `DataNodeDescriptor.of(..).adapter(..)` |
   | `bind(SchemaUpdateStrategyFactory.class)`                          | removed; `DataNodeDescriptor.of(..).schemaUpdateStrategy(..)`         |
   | `bind(JdbcEventLogger.class)`                                      | `bind(SQLLogger.class)`, see [CAY-2912](https://issues.apache.org/jira/browse/CAY-2912) above |
   | `bind(EntitySorter.class)`                                         | `bind(EntitySorterFactory.class)`                                     |
   | `bind(SelectTranslatorFactory.class)`                              | `bind(SelectTranslator.class)`, see translators below                 |
   | `bind(BatchTranslatorFactory.class)`                               | `bind(Key.get(BatchTranslator.class, BatchTranslator.INSERT))`, `UPDATE`, `DELETE`; soft delete via `softDeleteIfColumnPresent(..)` |
   | `bind(SQLTemplateProcessor.class)`                                 | `bind(SQLTemplateTranslator.class)`                                   |
   | `bind(ObjectStoreFactory.class)`, `bind(ObjectMapRetainStrategy.class)` | removed; override `DataContextFactory.newBuilder(..)`, the retain strategy is the `cayenne.object_retain_strategy` property |
   | `bind(ObjectContextFactory.class)`, `bind(QueryCache.class)`, `bind(EventManager.class)`, `bind(DataRowStoreFactory.class)`, `bind(TransactionFactory.class)`, `bind(DataDomainFlushActionFactory.class)`, `bind(RowReaderFactory.class)`, `bind(AdhocObjectFactory.class)`, `bind(ClassLoaderManager.class)` | unchanged |
   | -                                                                  | `bind(DataNodeFactory.class)`, new: creates `DataNode`s from `DataNodeDescriptor`s, including their `PkGenerator` |

*  The same prefix removal applies to the DI collection keys, which moved from `Constants` to `CoreModule`:
   `cayenne.server.user_types` is now `cayenne.user_types` (`CoreModule.USER_TYPES_LIST`), etc.
   `cayenne.server.domain_filters`, `cayenne.server.resource_locator` and `cayenne.server.rop_event_bridge_properties`
   were removed. This does not affect deployment, but may break service customizations that reference these
   collections by their String key, e.g.
   `@Inject("cayenne.server.user_types")` or `binder.bindList(ExtendedType.class, "cayenne.server.user_types")`, as
   the old names are gone. Reference `CoreModule` to pick the right key constant.

*  A custom `DbAdapter` needs a few changes:
   - The constructor: the DI collection keys moved to `CoreModule`, and the `ResourceLocator` parameter is gone.
     ```java
     // before
     public MyAdapter(@Inject RuntimeProperties runtimeProperties,
                      @Inject(Constants.SERVER_DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                      @Inject(Constants.SERVER_USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                      @Inject(Constants.SERVER_TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                      @Inject(Constants.SERVER_RESOURCE_LOCATOR) ResourceLocator resourceLocator,
                      @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
         super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories, resourceLocator,
                 valueObjectTypeRegistry);
     }

     // after
     public MyAdapter(@Inject RuntimeProperties runtimeProperties,
                      @Inject(CoreModule.DEFAULT_TYPES_LIST) List<ExtendedType> defaultExtendedTypes,
                      @Inject(CoreModule.USER_TYPES_LIST) List<ExtendedType> userExtendedTypes,
                      @Inject(CoreModule.TYPE_FACTORIES_LIST) List<ExtendedTypeFactory> extendedTypeFactories,
                      @Inject ValueObjectTypeRegistry valueObjectTypeRegistry) {
         super(runtimeProperties, defaultExtendedTypes, userExtendedTypes, extendedTypeFactories,
                 valueObjectTypeRegistry);
     }
     ```
   - Per [CAY-2963](https://issues.apache.org/jira/browse/CAY-2963) the `types.xml` resource and `TypesHandler` are
     gone. The JDBC-to-native type mapping is a `NativeColumnType[]` returned by `createNativeTypes()`, override it
     to adjust the mapping.
   - `bindParameter(PreparedStatement, ParameterBinding)` became `bindParameter(PreparedStatement, PSParameter<?>)`,
     plus `bindParameter(CallableStatement, CSParameter<?>)` for procedure parameters.
   - `getPkGenerator()` / `setPkGenerator(..)` became `createPkGenerator()`, see `PkGenerator` below.
   - Registration: pin the adapter on a node with `DataNodeDescriptor.of("node").adapter(MyAdapter.class)`, or add
     a `DbAdapterDetector` via `CoreModule.extend(binder).addAdapterDetector(..)` to detect it from the JDBC
     metadata. `DbAdapterFactory` is gone.

*  The SQL translators are stateless services now, bound in `CoreModule` and configured per `DataNode`:
   - `SelectTranslatorFactory` is gone. `SelectTranslator` is a single service with
     `translate(Select<?>, DbAdapter, EntityResolver)` returning a `TranslatedSelect`. The default
     `DbAdapterDelegatedSelectTranslator` delegates to `DbAdapter.getSelectTranslator(..)`, so a custom adapter can
     still supply its own. `DefaultSelectTranslator` was rewritten as a chain of `TranslationStage`s, so a 4.2
     subclass of it has to be redone against the new stages.
   - `BatchTranslatorFactory` and the `legacy` batch translators are gone. The insert, update and delete translators
     are bound separately under the `BatchTranslator.INSERT`, `UPDATE` and `DELETE` keys, and can also be set on a
     `DataNode`. `SoftDeleteBatchTranslator` is replaced by `CoreModule.extend(binder).softDeleteIfColumnPresent(..)`.
   - `SQLTemplateProcessor` is now `SQLTemplateTranslator`. The `cayenne-velocity` module still provides the Velocity
     flavor, as `VelocitySQLTemplateTranslator`.

*  A custom `ObjectContext` is installed via the `ObjectContextFactory` binding, as before, but `DataContext` lost its
   public constructors. Subclass `DataContext` through its protected constructor, extend `DataContextFactory` and
   override `newBuilder(DataChannel)` to produce your subclass, then bind the factory:
   ```java
   CayenneRuntime.of()
           .addConfig("cayenne-project.xml")
           .addModule(b -> b.bind(ObjectContextFactory.class).to(MyDataContextFactory.class))
           .build();
   ```

*  Per [CAY-2985](https://issues.apache.org/jira/browse/CAY-2985) `DataDomain` became mostly immutable. The
   `DataDomain(String)` constructor and all the setters below were removed in favor of a single full constructor that
   takes every collaborator and setting. Only DataNodes, DataMaps, filters and listeners can still be added (and
   removed) after creation. Replacements for the removed setters:
   - `setName(String)` — the name comes from the project XML, and can be overridden with the
     `cayenne.domain.name` property (`RuntimeProperties.DOMAIN_NAME_PROPERTY`).
   - `setEntityResolver(EntityResolver)` — keep using `addDataMap(..)` / `removeDataMap(..)` to change resolver
     contents.
   - `setEntitySorter(EntitySorter)` — the sorter is produced by the new `EntitySorterFactory` DI service. Bind your
     own `EntitySorterFactory` to replace it.
   - `setEventManager(EventManager)` — bind `EventManager` in a DI module instead.
   - `setQueryCache(QueryCache)` — bind `QueryCache` in a DI module instead.
   - `setSharedSnapshotCache(DataRowStore)`, `setDataRowStoreFactory(DataRowStoreFactory)` and
     `getDataRowStoreFactory()` — bind `DataRowStoreFactory` in a DI module to customize the cache.
   - `setSharedCacheEnabled(boolean)` — use the "Shared Cache" checkbox in the Modeler.
   - `setValidatingObjectsOnCommit(boolean)` — use the "Object Validation" checkbox in the Modeler.
   - `setMaxIdQualifierSize(int)` — use the `cayenne.max_id_qualifier_size` property
     (`RuntimeProperties.MAX_ID_QUALIFIER_SIZE_PROPERTY`).

*  Per [CAY-2987](https://issues.apache.org/jira/browse/CAY-2987), `PkGenerator` is now owned by `DataNode` rather
   than by `DbAdapter`. A `DbAdapter` is only the source of the default generator for its database. This change is
   entirely transparent unless you need to install a custom PkGenerator. You can do that via a custom injected
   `DefaultDataNodeFactory`, an explicit call to `dataNode.setPkGenerator(..)` or use a custom adapter.

*  Per [CAY-2955](https://issues.apache.org/jira/browse/CAY-2955) the obsolete `QueryEngine` abstraction
   (`org.apache.cayenne.access.QueryEngine`) has been removed. `DataNode` is now used directly wherever `QueryEngine`
   was previously referenced. So you must subclass `DataNode` and override `performQueries()` if you previously
   implemented a custom `QueryEngine`.

*  Per [CAY-3017](https://issues.apache.org/jira/browse/CAY-3017) `ObjectContext` no longer extends `DataChannel`.
   This doesn't change the overall architecture (there are still "channels" downstream from an ObjectContext). But the
   channel methods are no longer accessible on the context itself. Application code was never supposed to use them, as
   everything you need to do, you can do via the ObjectContext API.

*  `DataChannel.onQuery(..)`, `DataChannelQueryFilter.onQuery(..)` and `DataChannelQueryFilterChain.onQuery(..)`
   take an extra `boolean iteratedResult` parameter, which is `true` when the caller expects a `ResultIterator` (via
   `ObjectContext.iterator(..)` and friends) rather than a list. It replaces the internal `IteratedQueryDecorator`
   wrapper, so query filters now see the actual query instead of the wrapper. Together with the `List<QueryResult>`
   return type (see "Queries" above), custom `DataChannel` implementations and query filters need to add the parameter
   and pass it down the chain:
   ```java
   public List<QueryResult> onQuery(ObjectContext context, Query query, boolean iteratedResult,
                                    DataChannelQueryFilterChain chain) {
       return chain.onQuery(context, query, iteratedResult);
   }
   ```

*  `EventManager` listener registration no longer takes a listener method name to be looked up via reflection.
   `addListener(..)` and `addNonBlockingListener(..)` now take the event class and an `EventHandler` callback,
   which is normally an unbound method reference to the listener method:
   ```java
   // before
   eventManager.addListener(this, "snapshotsChanged", SnapshotEvent.class, subject, sender);

   // after
   eventManager.addListener(this, SnapshotEvent.class, MyListener::snapshotsChanged, subject, sender);
   ```
   The listener object is still passed and is still held via a weak reference, so an unregistered listener is
   released once it becomes unreachable. This only holds when the handler does not capture the listener: a bound
   reference such as `this::snapshotsChanged` or a capturing lambda keeps the listener alive for as long as the
   `EventManager` is. The listener method no longer needs to be public. The reflection-based
   `org.apache.cayenne.util.Invocation` class and the `SnapshotEventListener` interface (which was only ever a
   naming convention for the reflective lookup) were removed.

*  Per [CAY-3051](https://issues.apache.org/jira/browse/CAY-3051) `PropertyVisitor`, `PropertyDescriptor.visit(..)`
   and the `ClassDescriptor` methods `visitProperties(..)`, `visitDeclaredProperties(..)` and
   `visitAllProperties(..)` are deprecated. Iterate over the new `ClassDescriptor.getProperties()`,
   `getDeclaredProperties()` or `getAllProperties()` and switch on the property type (`AttributeProperty`,
   `ToOneProperty`, `ToManyProperty`) instead.
