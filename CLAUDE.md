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

The `cayenneTestConnection` property selects the database backend for tests. Default is HSQL.

```bash
# Run all tests (HSQL)
mvn verify

# Run against specific databases
mvn verify -DcayenneTestConnection=h2
mvn verify -DcayenneTestConnection=derby
mvn verify -DcayenneTestConnection=mysql-tc        # via TestContainers
mvn verify -DcayenneTestConnection=postgres-tc      # via TestContainers
mvn verify -DcayenneTestConnection=sqlserver-tc     # via TestContainers

# Run a single test class (in the relevant module directory)
mvn test -Dtest=PoolingDataSourceTest

# Run a single test method
mvn test -Dtest=PoolingDataSourceTest#testConnection

# Run a single test against a specific database
mvn test -Dtest=SomeTest -DcayenneTestConnection=h2
```

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

## `pom.xml` Style

All POM plugins from submodules must be delcared in the parent module `<pluginManagement>`. All plugin versions should be 
declared as properties in the root pom.xml


## CI Matrix

GitHub Actions runs on push to master/STABLE-* branches:
- JDK: 21, 25
- Databases: hsql, h2, derby, mysql-tc, postgres-tc, sqlserver-tc

## Issue Tracker

JIRA: https://issues.apache.org/jira/browse/CAY — Ticket format: `CAY-XXXX`
