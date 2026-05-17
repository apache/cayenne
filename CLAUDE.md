# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Apache Cayenne is a Java ORM and persistence framework. This is a multi-module Maven project targeting Java 21+.
When generating code, if appropraite use the latest Java syntax vs old (multiline-strings, new switches, etc.)

## Build Commands

```bash
# Full build
mvn clean verify

# Skip tests
mvn clean verify -DskipTests

# Build a single module and its dependencies
mvn clean verify -pl cayenne -am
```

## Testing

Tests split into two suites driven by different plugins, and **selecting a single test uses a different property for each**:

- `*Test.java` — unit tests, run by Surefire on `mvn test`. Selector: `-Dtest=`
- `*IT.java` — integration tests, run by Failsafe on `mvn verify` (not `mvn test`). Selector: `-Dit.test=`

Passing `-Dtest=SomeIT` to `mvn test` will fail with "No tests matching pattern" because Surefire's default include patterns exclude `*IT.java`. Always pair `-Dit.test=` with `mvn verify`.

The `cayenneTestConnection` property selects the database backend for tests. Default is HSQL.

```bash
# Run all tests, unit + integration (HSQL)
mvn verify

# Run against a specific database
mvn verify -DcayenneTestConnection=h2
mvn verify -DcayenneTestConnection=derby
mvn verify -DcayenneTestConnection=mysql        # via TestContainers
mvn verify -DcayenneTestConnection=postgres     # via TestContainers
mvn verify -DcayenneTestConnection=sqlserver    # via TestContainers

# Skip tests
mvn verify -DskipITs        # skip integration tests, still run unit tests
mvn verify -DskipTests      # skip both

# --- Run a single UNIT test (*Test.java) — Surefire ---
mvn test -Dtest=PoolingDataSourceTest -Dsurefire.failIfNoSpecifiedTests=false -pl cayenne -am
mvn test -Dtest=PoolingDataSourceTest#testConnection -Dsurefire.failIfNoSpecifiedTests=false -pl cayenne -am
mvn test -Dtest=PoolingDataSourceTest -DcayenneTestConnection=h2 -Dsurefire.failIfNoSpecifiedTests=false -pl cayenne -am

# --- Run a single INTEGRATION test (*IT.java) — Failsafe ---
# Runs the IT plus all unit tests in the module (simple form):
mvn verify -Dit.test=Cay2412IT -Dfailsafe.failIfNoSpecifiedTests=false -pl cayenne -am
mvn verify -Dit.test=Cay2412IT#disjointByIdPrefetch -Dfailsafe.failIfNoSpecifiedTests=false -pl cayenne -am

# Runs only the IT, skipping unit tests in the same module (faster for iteration):
mvn verify -Dit.test=Cay2412IT \
    -Dtest='__nothing__' \
    -Dsurefire.failIfNoSpecifiedTests=false \
    -Dfailsafe.failIfNoSpecifiedTests=false \
    -pl cayenne -am
```

Why the `failIfNoSpecifiedTests=false` flags are needed when using `-am`: Surefire and Failsafe are bound in the parent `<pluginManagement>` and inherit into every reactor module, including upstream JAR modules that have no matching test class. Without these flags the build fails on the first such module. If you instead `cd` into the target module and the dependencies are already installed, you can omit both `-pl ... -am` and the flags.

## Module Structure

- **cayenne** — Core ORM library (main module to work with)
- **cayenne-di** — Lightweight DI container used internally
- **cayenne-project** — Cayenne project/model file management
- **cayenne-cgen** — Code generation from database schemas
- **cayenne-dbsync** — Database schema synchronization
- **cayenne-gradle-plugin**, **cayenne-maven-plugin**, **cayenne-ant** — Build tool integrations
- **cayenne-crypto**, **cayenne-commitlog**, **cayenne-lifecycle**, **cayenne-jcache**, **cayenne-cache-invalidation** — Optional extension modules
- **modeler** — CayenneModeler GUI application (Swing)


## Test Style

Test naming: `*Test.java` = unit tests (Surefire), `*IT.java` = integration tests (Failsafe).
All new tests must use JUnit 5. Test classes and methods must be `public`. Method names must not use the `test` prefix (e.g. `someFeature()` not `testSomeFeature()`).
Do not add `@since` tags to test classes — version tags are only meaningful on public API.

## `pom.xml` Style

All POM plugins from submodules must be delcared in the parent module `<pluginManagement>`. All plugin versions should be 
declared as properties in the root pom.xml

Do not set `<scope>` inside `<dependencyManagement>` (except `<scope>import</scope>` for BOMs). `<dependencyManagement>`
pins versions only — each consuming `<dependency>` declares its own scope explicitly.


## CI Matrix

GitHub Actions runs on push to master/STABLE-* branches:
- JDK: 21, 25
- Databases: hsql, h2, derby, mysql, postgres, sqlserver

## Issue Tracker

JIRA: https://issues.apache.org/jira/browse/CAY — Ticket format: `CAY-XXXX`
