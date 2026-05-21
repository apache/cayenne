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
| `<destDir>` | — | Output directory, **relative to the project XML directory** (the directory containing `cayenne-*.xml`). Typical values: `.`, `../java`, `../../main/java`. Absolute paths also work. |
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
4. Resolves `destDir` relative to the project file.
5. Runs the generator. Returns a JSON object listing files `written`, files `skipped` (already up-to-date), and any `errors`.

If the DataMap has no `<cgen>` block, the tool returns an error and the user must add one — typically via the `cayenne-modeling` skill or the Modeler GUI.

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

Assuming the project XML lives at `src/main/resources/cayenne-mydb.xml`, this writes to `src/main/java/<package>/`.

## Anti-patterns

- Setting `<makePairs>false</makePairs>` to "simplify" — you lose the ability to add user code without it being overwritten on the next cgen run.
- Editing `_<Entity>.java` files (the superclasses). They are regenerated. Customize `<Entity>.java` (the subclass) instead.
- `<destDir>` as an absolute path checked into source — breaks reproducibility for other developers. Use a relative path.
