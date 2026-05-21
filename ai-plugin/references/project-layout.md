# Cayenne project layout

How a Cayenne project lays out on disk and how to locate the relevant files in a user's repo.

## Two file types

A Cayenne project consists of two kinds of XML files:

1. **Project descriptor** — one per project. File name pattern: `cayenne-*.xml` (e.g. `cayenne-mydb.xml`). Root element `<domain>` in namespace `http://cayenne.apache.org/schema/12/domain`. Lists DataMaps and DataNodes.
2. **DataMap files** — one or more per project. File name pattern: `*.map.xml` (commonly `<name>.map.xml`, where `<name>` matches the `<map name="...">` reference in the project descriptor). Root element `<data-map>` in namespace `http://cayenne.apache.org/schema/12/modelMap`. Contains entities, relationships, queries.

The descriptor and its DataMaps live in the **same directory**. The descriptor references DataMaps by name only (no path), so they must be siblings on the classpath.

## Where to look

Common locations, in order of likelihood:

1. **`src/main/resources/`** of the module that bootstraps `CayenneRuntime`. This is the typical placement for a runtime-bundled project. Scan for `cayenne-*.xml` here first.
2. **`src/main/resources/<package>/`** — a subdirectory if the user wants to keep mapping files namespaced.
3. **Repo root or `cayenne/` subdir** — small projects sometimes keep mapping at the top.

To find them programmatically:

```bash
find . -name "cayenne-*.xml" -not -path "*/target/*" -not -path "*/build/*"
find . -name "*.map.xml" -not -path "*/target/*" -not -path "*/build/*"
```

Filter out `target/`, `build/`, and any test-resource paths unless the user is explicitly editing test fixtures.

## Identifying the active project

If multiple `cayenne-*.xml` files exist, identify the one the application actually uses:

- Search the Java code for `CayenneRuntime.builder().addConfig("...")` — the string argument is the descriptor file name (e.g. `"cayenne-mydb.xml"`).
- Or look for `addConfigs("...")` for multi-config setups.
- If none is found, the runtime may rely on auto-loading (`cayenne-project.xml` is *not* a Cayenne convention — Cayenne does not auto-load by a default name).

When ambiguous, ask the user which project they mean. Cache the answer for the rest of the session.

## What goes where

| Change | File to edit |
|---|---|
| Add/remove an `ObjEntity`, `DbEntity`, attribute, relationship, embeddable, named query | DataMap (`*.map.xml`) |
| Change a DataMap's `defaultPackage` or `defaultSuperclass` | DataMap (`*.map.xml`) — top-level `<property>` |
| Add/remove a DataMap from the project | Project descriptor (`cayenne-*.xml`) — `<map>` element |
| Add/configure a DataNode (DB connection) | Project descriptor (`cayenne-*.xml`) — `<node>` element |
| Embedded code-generation config (`<cgen>`) | DataMap (`*.map.xml`) |
| Embedded reverse-engineering config (`<dbImport>`) | DataMap (`*.map.xml`) |

Schema details live in `datamap-schema.md` and `project-descriptor-schema.md`.
