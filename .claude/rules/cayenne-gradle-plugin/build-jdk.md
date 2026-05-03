---
paths:
  - "cayenne-gradle-plugin/**"
description: cayenne-gradle-plugin must build on Java 11
---

Any Maven build that includes `cayenne-gradle-plugin` (the module itself, or
any reactor invocation that doesn't exclude it) must be run on **Java 11**.

The module's tests run a Gradle build via `exec-maven-plugin`, which spins up a
Gradle daemon that compiles `build.gradle` with its embedded Groovy. That Groovy
version cannot read class files newer than the daemon's supported JVM. On newer
JDKs the build fails before any test runs with:

> BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
> Unsupported class file major version NN

To run a full reactor build on a newer JDK, exclude this module:

```
mvn clean verify -pl '!cayenne-gradle-plugin'
```
