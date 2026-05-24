<!--
	Licensed to the Apache Software Foundation (ASF) under one
	or more contributor license agreements.  See the NOTICE file
	distributed with this work for additional information
	regarding copyright ownership.  The ASF licenses this file
	to you under the Apache License, Version 2.0 (the
	"License"); you may not use this file except in compliance
	with the License.  You may obtain a copy of the License at
	
	https://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an
	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, either express or implied.  See the License for the
	specific language governing permissions and limitations
	under the License.   
-->
# Class generation config (`<cgen>`)

Reference for the embedded `<cgen>` block inside a DataMap that drives `mcp__cayenne__cgen_run`.

## XML shape (embedded in a DataMap)

```xml
<data-map xmlns="http://cayenne.apache.org/schema/12/modelMap" ...>
    ...entities, relationships, etc...

    <cgen xmlns="http://cayenne.apache.org/schema/12/cgen">
        <destDir>../java</destDir>
        <mode>entity</mode>
        <excludeEntities></excludeEntities>
        <excludeEmbeddables></excludeEmbeddables>
        <outputPattern>*.java</outputPattern>
        <makePairs>true</makePairs>
        <usePkgPath>true</usePkgPath>
        <overwrite>false</overwrite>
        <createPropertyNames>false</createPropertyNames>
        <createPKProperties>true</createPKProperties>
    </cgen>
</data-map>
```

Note the **different namespace** for `<cgen>` (`http://cayenne.apache.org/schema/12/cgen`) — it is a separate schema embedded in the modelMap schema.

## Field reference

### Required

| Field | Default | Meaning |
|---|---|---|
| `<destDir>` | — | Output directory, **relative to the directory containing `*.map.xml`**. Typical values: `.`, `../java`, `../../main/java`. |
| `<mode>` | `entity` | What to generate. `entity` = per-entity classes. `all` = per-entity classes + a single DataMap class with named-query helpers. |

### Pairs vs single class

| Field | Default | Meaning |
|---|---|---|
| `<makePairs>` | `true` | When `true`, generate two files per entity: `_Artist.java` (regenerated, never edit) and `Artist.java` (user subclass, edit freely). When `false`, generate a single `Artist.java` — overwritten each run. **Almost always leave at `true`.** |
| `<overwrite>` | `false` | Only meaningful when `makePairs=false`. When `true`, the single subclass is overwritten on each run. Dangerous if user code lives there. |

### Filtering

| Field | Default | Meaning |
|---|---|---|
| `<excludeEntities>` | (empty) | Comma-separated ObjEntity names to skip. Inverse — what's *included* is everything else. |
| `<excludeEmbeddables>` | (empty) | Comma-separated embeddable class names to skip. |

### Output

| Field | Default | Meaning |
|---|---|---|
| `<outputPattern>` | `*.java` | File name pattern. `*` is replaced by the class name. Change to `*.kt` for Kotlin templates. |
| `<encoding>` | platform default | Character encoding for generated files. |
| `<usePkgPath>` | `true` | When `true`, generated files go into subdirectories matching their Java package (`com/example/Artist.java`). When `false`, all files are flat in `destDir`. |
| `<superPkg>` | (empty) | Override package for generated superclasses (`_Artist`). By default, `<defaultPackage>.auto`. |

### Templates

| Field | Default | Meaning |
|---|---|---|
| `<template>` | built-in `subclass.vm` | Velocity template for the user subclass (`Artist.java`). Use a CDATA path to a custom `.vm` file. |
| `<superTemplate>` | built-in `superclass.vm` | Template for the auto-generated superclass (`_Artist.java`). |
| `<embeddableTemplate>` | built-in | Template for embeddable subclass. |
| `<embeddableSuperTemplate>` | built-in | Template for embeddable superclass. |
| `<dataMapTemplate>` | built-in | Template for the DataMap helper class (only used when `<mode>` is `all`). |
| `<dataMapSuperTemplate>` | built-in | Superclass template for the DataMap helper. |

Custom templates are rare. Skip unless the user explicitly asks for them.

### Properties

| Field | Default | Meaning |
|---|---|---|
| `<createPropertyNames>` | `false` | When `true`, also generate `public static final String` constants for each attribute name. Legacy — most code uses the typed `Property<>` constants instead. |
| `<createPKProperties>` | `true` | When `true`, generate `Property<>` constants for PK columns too. |
| `<externalToolConfig>` | (empty) | Free-form config consumed by external tools (e.g., Kotlin generator extensions). |

## Modeler equivalent

In CayenneModeler: select the DataMap → "Class Generation" tab. Every field above has a UI control. Saving the project persists them back as `<cgen>` in the DataMap.

## How `mcp__cayenne__cgen_run` uses this

The MCP tool:

1. Loads the project at `projectPath`.
2. Finds the named DataMap (`dataMap` argument).
3. Reads its embedded `<cgen>` block.
4. Resolves `destDir` relative to the `*.map.xml` file.
5. Runs the generator. Returns a JSON object listing files `written`, files `skipped` (already up-to-date), and any `errors`.

If the DataMap has no `<cgen>` block, the tool returns an error and the user must add one — typically via the `cayenne-modeling` skill or the Modeler GUI.

## Determining `destDir`

`destDir` is relative to the directory that contains `*.map.xml`.

**Before writing the default config, inspect the project tree** to find the Java source root and express it as a relative path from the map file's directory. Do not blindly paste `../java`.

### Standard Maven layout

Map files are frequently placed in a package subdirectory under `resources/` rather than directly in `resources/` itself. Count the directory levels between `resources/` and the map file and add that many extra `../` segments.

| Map file location | `destDir` |
|---|---|
| `src/main/resources/mydb.map.xml` | `../java` |
| `src/main/resources/com/mydb.map.xml` | `../../java` |
| `src/main/resources/com/example/mydb.map.xml` | `../../../java` |
| `src/main/resources/com/example/app/mydb.map.xml` | `../../../../java` |
| `src/test/resources/mydb.map.xml` | `../java` |
| `src/test/resources/com/example/app/mydb.map.xml` | `../../../../java` |

The rule: one `../` to escape `resources/`, then one `../` per package segment, then `java`. Always verify by resolving the resulting path mentally before writing it.

### Non-standard layouts

If the project doesn't follow Maven conventions, locate the actual Java sources:

1. Look for existing `.java` files or a `src/` directory near the map file.
2. Compute the relative path from the map file's directory to that sources root.
3. If no Java sources exist yet, ask the user where generated classes should go.

### Distinguishing main vs test maps

If the map file lives under a test resource directory (`src/test/resources/`, `test/`, or a path containing `test`), the generated classes belong in the test source tree. Point `destDir` there rather than at the main sources — test entity classes mixed into `src/main/java/` are a common mistake.

## Recommended starting config

```xml
<cgen xmlns="http://cayenne.apache.org/schema/12/cgen">
    <destDir>../java</destDir>
    <mode>entity</mode>
    <makePairs>true</makePairs>
    <usePkgPath>true</usePkgPath>
    <createPKProperties>true</createPKProperties>
</cgen>
```

The `../java` value above assumes a standard Maven layout (`src/main/resources/` or `src/test/resources/`). Adjust if the project uses a non-standard layout — see "Determining `destDir`" above.

## Anti-patterns

- Setting `<makePairs>false</makePairs>` to "simplify" — you lose the ability to add user code without it being overwritten on the next cgen run.
- Editing `_<Entity>.java` files (the superclasses). They are regenerated. Customize `<Entity>.java` (the subclass) instead.
- `<destDir>` as an absolute path checked into source — breaks reproducibility for other developers. Use a relative path.
- Pasting `../java` blindly without checking the actual project layout. Inspect the tree first; see "Determining `destDir`" above.
- Pointing a test DataMap's `destDir` at `src/main/java/` — test entity classes belong in the test source tree.
