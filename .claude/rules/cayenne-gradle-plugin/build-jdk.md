---
paths:
  - "cayenne-gradle-plugin/**"
description: cayenne-gradle-plugin Gradle version compatibility
---

The `cayenne-gradle-plugin` integration tests run Gradle builds via `exec-maven-plugin`.
The module uses Gradle 9.4.0, which supports running on Java 21 through 26.

CI includes this module on Java 21 and Java 25. The Maven enforcer in the module
keeps the build aligned with Gradle's supported runtime range.

The IT test (`GradlePluginIT`) tests Gradle 9.4.0 compatibility.
