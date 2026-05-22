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


## Upgrading to 5.0.M2

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

## Upgrading to 5.0.M1

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
  CayenneRuntime.builder(..)
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
  CayenneRuntime runtime = CayenneRuntime.builder()
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
