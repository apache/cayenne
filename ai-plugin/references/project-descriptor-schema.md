# Project descriptor XML schema (`cayenne-*.xml`)

Reference for editing the top-level Cayenne project descriptor.

- **Namespace**: `http://cayenne.apache.org/schema/12/domain`
- **XSD**: `http://cayenne.apache.org/schema/12/domain.xsd`
- **Project version**: `12` (Cayenne 5.0)

## Minimal descriptor

```xml
<?xml version="1.0" encoding="utf-8"?>
<domain xmlns="http://cayenne.apache.org/schema/12/domain"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://cayenne.apache.org/schema/12/domain https://cayenne.apache.org/schema/12/domain.xsd"
        project-version="12">
    <map name="mydb"/>
</domain>
```

This references a sibling `mydb.map.xml`.

## With a DataNode (DB connection)

Most production setups configure the DataSource in code via `CayenneRuntimeBuilder` (see `runtime-api.md`) rather than the descriptor — keeps secrets out of source control. But the descriptor *can* hold a DataNode:

```xml
<domain ...>
    <map name="mydb"/>
    <node name="mydb-node"
          factory="org.apache.cayenne.configuration.runtime.XMLPoolingDataSourceFactory"
          schema-update-strategy="org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy">
        <map-ref name="mydb"/>
        <data-source>
            <driver value="org.postgresql.Driver"/>
            <url value="jdbc:postgresql://localhost:5432/mydb"/>
            <connectionPool min="1" max="10"/>
            <login userName="cayenne" password="secret"/>
        </data-source>
    </node>
</domain>
```

| Element/attribute | Meaning |
|---|---|
| `<map name="...">` | References a sibling DataMap file. Must match the `*.map.xml` file name minus the extension. |
| `<node name="...">` | A DataNode (database). The `factory=` selects the DataSource factory. |
| `factory=` | `XMLPoolingDataSourceFactory` reads the inline `<data-source>` block. `DBCPDataSourceFactory`, `JNDIDataSourceFactory` are alternatives. |
| `schema-update-strategy=` | What to do on startup when DB schema disagrees with the model. `SkipSchemaUpdateStrategy` is the safe default. |
| `<map-ref name="...">` | Which DataMap this node serves. A node can serve multiple maps. |

## Multi-map projects

A descriptor can reference multiple DataMaps:

```xml
<domain ...>
    <map name="customers"/>
    <map name="orders"/>
    <node name="primary">
        <map-ref name="customers"/>
        <map-ref name="orders"/>
        ...
    </node>
</domain>
```

All referenced `*.map.xml` files must sit in the same directory as the descriptor.

## Recommended pattern

For a typical app:

- Descriptor lists DataMaps with `<map>` only; **no** `<node>` element.
- Wire the DataSource in code via `CayenneRuntimeBuilder.dataSource(...)` or `.url(...).jdbcDriver(...)`. See `runtime-api.md`.

This keeps DB credentials out of XML and lets the app pick the DataSource per environment (test, dev, prod).

## Anti-patterns

- Putting JDBC credentials in `cayenne-*.xml` and committing them to source control. Use code-level configuration.
- Referencing a DataMap that doesn't exist as a sibling file — the runtime will fail to load.
- Forgetting `project-version="12"` — older versions are silently rejected by Cayenne 5.0.
