---
paths:
  - "cayenne-gradle-plugin/**"
description: cayenne-gradle-plugin Gradle version compatibility
---

The `cayenne-gradle-plugin` integration tests run Gradle builds via `exec-maven-plugin`.
The Gradle version is selected in `GradlePluginIT.java` based on the running JDK:

- Java >= 22 → Gradle 8.8
- Java 21 (minimum baseline) → Gradle 8.5

Gradle 8.5+ supports Java 21 class files, so the full reactor build works on Java 21+.
