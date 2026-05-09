---
paths:
  - "cayenne-gradle-plugin/**"
description: cayenne-gradle-plugin Gradle version compatibility
---

The `cayenne-gradle-plugin` integration tests run Gradle builds via `exec-maven-plugin`.
Gradle 8.x does not support Java > 21.

CI excludes the module on Java 25+ by passing `--projects !cayenne-gradle-plugin`
to Maven (see `verify-deploy-on-push.yml`). No pom changes are needed — Maven's
built-in project exclusion handles it. Releases always use Java 21, so the module
is always included there.

The IT test (`GradlePluginIT`) tests Gradle 8.5 compatibility and only runs on Java 21.
