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
# Reverse-engineering config (`<dbImport>`)

Reference for the options shown by the CayenneModeler reverse-engineering dialog, persisted as a `<dbImport>` block inside a DataMap.

## XML shape (persisted inside a DataMap)

```xml
<dbImport xmlns="http://cayenne.apache.org/schema/12/dbimport">
    <catalog>
        <name>public</name>
        <schema>
            <name>app</name>
            <includeTable>
                <pattern>CUSTOMER.*</pattern>
            </includeTable>
            <excludeTable>
                <pattern>TMP_.*</pattern>
            </excludeTable>
        </schema>
    </catalog>
    <tableTypes>
        <tableType>TABLE</tableType>
        <tableType>VIEW</tableType>
    </tableTypes>
    <defaultPackage>com.example.model</defaultPackage>
    <forceDataMapCatalog>false</forceDataMapCatalog>
    <forceDataMapSchema>false</forceDataMapSchema>
    <meaningfulPkTables></meaningfulPkTables>
    <namingStrategy>org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator</namingStrategy>
    <skipPrimaryKeyLoading>false</skipPrimaryKeyLoading>
    <skipRelationshipsLoading>false</skipRelationshipsLoading>
    <stripFromTableNames></stripFromTableNames>
    <useJava7Types>false</useJava7Types>
</dbImport>
```

## Field reference

### Filtering

| Field | Type | Meaning |
|---|---|---|
| `<catalog><name>` | string | DB catalog to import from. Many databases have a single default catalog. |
| `<schema><name>` | string | DB schema (Postgres `public`, MSSQL `dbo`, etc.). |
| `<includeTable><pattern>` | regex | Tables to include. Multiple `<includeTable>` elements union. Default: include all. |
| `<excludeTable><pattern>` | regex | Tables to skip. Multiple `<excludeTable>` elements union. |
| `<includeColumn><pattern>` | regex | Columns to include (rarely needed). |
| `<excludeColumn><pattern>` | regex | Columns to skip — useful for system columns (`CREATED_AT`, `ROWVERSION`). |
| `<includeProcedure><pattern>` | regex | Stored procedures to include. By default, none are imported. |
| `<excludeProcedure><pattern>` | regex | Stored procedures to skip. |
| `<tableTypes><tableType>` | enum | Which JDBC table types to import. Typical values: `TABLE`, `VIEW`, `SYSTEM TABLE`, `GLOBAL TEMPORARY`, `LOCAL TEMPORARY`, `ALIAS`, `SYNONYM`. If empty, defaults to `TABLE`. |

### Generation

| Field | Type | Default | Meaning |
|---|---|---|---|
| `<defaultPackage>` | string | (empty) | Java package for generated ObjEntity classes. If the DataMap already has `defaultPackage`, that wins. |
| `<namingStrategy>` | FQN string | `org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator` | Class implementing `ObjectNameGenerator` that converts `CUSTOMER_ORDER` → `CustomerOrder`, `customer_id` → `customerId`. Plug in a custom one for non-default conventions. |
| `<stripFromTableNames>` | regex | (empty) | Regex applied to each table name *before* naming. E.g. `^TBL_` strips a `TBL_` prefix so `TBL_CUSTOMER` → `Customer`. |
| `<meaningfulPkTables>` | comma-separated regex | (empty) | Tables whose PK columns should be mapped as ObjAttributes (visible on the Java side). `*` matches all. By default PKs are hidden. |
| `<useJava7Types>` | boolean | `false` | When `true`, generate `java.util.Date` instead of `java.time.LocalDate`/`LocalDateTime`. Use only when targeting legacy JVMs. |

### Behavior toggles

| Field | Type | Default | Meaning |
|---|---|---|---|
| `<skipPrimaryKeyLoading>` | boolean | `false` | When `true`, PK metadata is not read from the DB. Rare. |
| `<skipRelationshipsLoading>` | boolean | `false` | When `true`, FKs are not read. Useful when FKs are missing in the schema and modeled manually. |
| `<forceDataMapCatalog>` | boolean | `false` | When `true`, every imported DbEntity is tagged with the DataMap's catalog, overriding the DB's reported value. Use only when the DB catalog reported by JDBC is wrong or noisy. |
| `<forceDataMapSchema>` | boolean | `false` | Same as above for schema. |

## How the Modeler dialog maps to these fields

Walking the **Tools → Reengineer Database Schema** dialog, the screens correspond to:

1. **Datasource** — JDBC connection (adapter, driver, URL, user/password). Not stored in `<dbImport>`; it's a one-shot connection.
2. **Configure** — filter tables/columns/procedures. Maps to `<includeTable>`, `<excludeTable>`, `<tableTypes>`, etc.
3. **Naming** — `<namingStrategy>`, `<stripFromTableNames>`, `<defaultPackage>`, `<meaningfulPkTables>`.
4. **Other options** — checkboxes for `skipPrimaryKeyLoading`, `skipRelationshipsLoading`, `forceDataMapCatalog`, `forceDataMapSchema`, `useJava7Types`.

After running the dialog, the chosen settings are persisted as a `<dbImport>` block in the DataMap so subsequent re-imports re-use them.

## Re-running an import

A second import against the same DataMap is *merge-based*: existing entities/attributes/relationships that match the DB are preserved; new ones are added; the user is offered a chance to drop ones that no longer exist in the DB.

User-edited names, types, and custom attributes generally survive re-import. Custom relationships that don't correspond to a DB FK do not survive.

## Anti-patterns

- Setting `forceDataMapCatalog` / `forceDataMapSchema` "just to be safe" — they suppress useful DB metadata and cause subtle bugs in multi-catalog setups.
- Using `useJava7Types=true` on a new project — `java.time` types are strictly better. Only enable for legacy.
- Importing without any `<includeTable>` filter on a large schema — you get every system table and view.
