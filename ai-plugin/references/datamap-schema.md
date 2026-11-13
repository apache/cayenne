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
# DataMap XML schema (`*.map.xml`)

Reference for editing Cayenne DataMap files directly.

- **Namespace**: `http://cayenne.apache.org/schema/15/dataMap`
- **XSD**: `http://cayenne.apache.org/schema/15/dataMap.xsd`
- **Cayenne version**: 6.0. The schema version is the number in the namespace; there is no `project-version`
  attribute anymore. Files of older versions are upgraded automatically when loaded.
- **Working example**: `cayenne-ant/src/test/resources/testmap.map.xml`

## Root element

```xml
<?xml version="1.0" encoding="utf-8"?>
<dataMap xmlns="http://cayenne.apache.org/schema/15/dataMap"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://cayenne.apache.org/schema/15/dataMap http://cayenne.apache.org/schema/15/dataMap.xsd">
    ...
</dataMap>
```

## DataMap settings

Optional attributes of `<dataMap>` configure DataMap-wide settings: `defaultPackage`, `defaultSuperclass`,
`defaultCatalog`, `defaultSchema`, `defaultLockType` (`optimistic`) and `quoteSqlIdentifiers` (`true`). The two used in
practice:

```xml
<dataMap xmlns="http://cayenne.apache.org/schema/15/dataMap" ...
         defaultPackage="com.example.model"
         defaultSuperclass="org.apache.cayenne.GenericPersistentObject">
```

- `defaultPackage` — Java package for generated classes when an `objEntity` `className` is a short name.
- `defaultSuperclass` — superclass for generated `_<Name>` superclasses. Default is `PersistentObject`.

## Element order

The schema requires this order inside `<dataMap>` (`?` — at most one, `*` — any number):

1. `<comment>` ?
2. `<embeddable>` *
3. `<procedure>` *
4. `<dbEntity>` *
5. `<objEntity>` *
6. `<dbRelationship>` *
7. `<objRelationship>` *
8. `<objectQuery>`, `<sqlQuery>`, `<procedureQuery>` * (in any mix)
9. `<dbImport>` ?
10. `<cgen>` *

All of them are in the DataMap namespace. Keep this order when inserting elements by
hand. Cayenne writes the elements in this order, schema-aware tools reject a misplaced element, and Cayenne's loader
fails on or silently misreads some misplaced elements: a `<dbRelationship>` or `<objRelationship>` placed before the
entity it starts from fails the loading, and a `<sqlQuery>` or `<procedureQuery>` placed before the entity or
procedure it is rooted in loads with the DataMap as its root instead.

## `<comment>` — user comment

Most elements can hold a free-text comment, edited in CayenneModeler and ignored by the runtime. The default class
generation templates write the comment of an ObjEntity to the Javadoc of the generated class.

```xml
<objEntity name="Artist" className="com.example.Artist" dbEntityName="ARTIST">
    <comment>A painter. Paintings &amp; exhibitions are separate entities.</comment>
    <objAttribute name="artistName" type="java.lang.String" dbAttributePath="ARTIST_NAME">
        <comment>The name as signed on the paintings</comment>
    </objAttribute>
</objEntity>
```

## `<dbEntity>` — database table

```xml
<dbEntity name="ARTIST" catalog="public" schema="public">
    <dbAttribute name="ARTIST_ID" type="BIGINT" primaryKey="true" mandatory="true"/>
    <dbAttribute name="ARTIST_NAME" type="VARCHAR" length="254" mandatory="true"/>
    <dbAttribute name="DATE_OF_BIRTH" type="DATE"/>
</dbEntity>
```

Attribute fields:

| Attribute | Meaning |
|---|---|
| `name` | Column name (case-preserved as-is from the DB) |
| `type` | JDBC type name: `INTEGER`, `BIGINT`, `VARCHAR`, `CHAR`, `DATE`, `TIMESTAMP`, `BOOLEAN`, `BIT`, `NUMERIC`, `DECIMAL`, `FLOAT`, `DOUBLE`, `BLOB`, `CLOB`, `VARBINARY`, etc. |
| `length` | Column length (for VARCHAR, CHAR, VARBINARY, NUMERIC) |
| `scale` | Decimal scale (NUMERIC, DECIMAL) |
| `precision` | Numeric precision, rarely set (the digits of NUMERIC/DECIMAL go in `length`) |
| `primaryKey` | `true` for PK columns |
| `mandatory` | `true` for NOT NULL |
| `generated` | `true` for DB-generated columns (identity, sequence) |

## `<objEntity>` — Java object mapped to a DbEntity

```xml
<objEntity name="Artist" className="com.example.Artist" dbEntityName="ARTIST">
    <objAttribute name="artistName" type="java.lang.String" dbAttributePath="ARTIST_NAME"/>
    <objAttribute name="dateOfBirth" type="java.util.Date" dbAttributePath="DATE_OF_BIRTH"/>
</objEntity>
```

Element attributes:

| Attribute | Meaning |
|---|---|
| `name` | Object name (used in queries) |
| `className` | Fully-qualified Java class. If using DataMap `defaultPackage`, a short name works. |
| `superClassName` | Optional explicit superclass |
| `dbEntityName` | Matching `<dbEntity name="...">` |
| `superEntityName` | For inheritance — name of parent ObjEntity |
| `readOnly` | `true` to disallow writes |
| `abstract` | `true` for abstract entities (single-table inheritance) |
| `lockType` | `optimistic` for optimistic locking on the properties marked with `lock` |

`objAttribute`:

| Attribute | Meaning |
|---|---|
| `name` | Java property name |
| `type` | Java type, FQN — `java.lang.String`, `java.lang.Integer`, `java.util.Date`, `java.math.BigDecimal`, `byte[]`, `boolean`, etc. |
| `dbAttributePath` | Column name. May be a dotted path through a `dbRelationship` for derived attributes: `toArtist.ARTIST_NAME`. |
| `lock` | `true` to use the attribute for optimistic locking |
| `lazy` | `true` to load the attribute's value lazily, on first access |

The children of `<objEntity>` go in this order: `<comment>`, `<qualifier>`, `<embeddedAttribute>`s,
`<objAttribute>`s, `<attributeOverride>`s. An entity with `superEntityName` maps an inherited attribute to another
column with `<attributeOverride name="..." dbAttributePath="..."/>`.

PK columns are not normally mapped as `objAttribute` — they're handled implicitly. Map a PK column only if `meaningfulPK` (you want to expose the PK value to the Java side).

## `<dbRelationship>` — FK join in the DB layer

```xml
<dbRelationship name="paintingArray" source="ARTIST" target="PAINTING" toMany="true">
    <dbAttributePair source="ARTIST_ID" target="ARTIST_ID"/>
</dbRelationship>
```

| Attribute | Meaning |
|---|---|
| `name` | Identifier; referenced by `objRelationship`'s `dbRelationshipPath` |
| `source` | Owning DbEntity name |
| `target` | Target DbEntity name |
| `toMany` | `true` for one-to-many or many-to-many side |
| `foreignKey` | `true` on the side whose join columns hold the FK: every to-one except the master side of a PK-to-PK one-to-one |

Each `<dbAttributePair>` is one column of the join. Compound joins use multiple `<dbAttributePair>` elements.

**Symmetry.** Most FKs need *two* `<dbRelationship>` entries, one from each side. They are not auto-derived.

## `<objRelationship>` — object-layer view of a dbRelationship

```xml
<objRelationship name="paintings"
                  source="Artist"
                  target="Painting"
                  deleteRule="cascade"
                  dbRelationshipPath="paintingArray"/>
```

| Attribute | Meaning |
|---|---|
| `name` | Java property name (typically plural for to-many) |
| `source` | Owning ObjEntity name |
| `target` | Target ObjEntity name |
| `deleteRule` | `nullify`, `cascade` or `deny` (no attribute means no action) |
| `dbRelationshipPath` | One or more `<dbRelationship>` names, dot-separated for flattened (many-to-many) relationships: `artistGroupArray.toGroup` |
| `collectionType` | Java collection of a to-many: `java.util.List` (default), `java.util.Set`, `java.util.Map` or `java.util.Collection` |
| `mapKey` | For `java.util.Map`: the target entity property used as the map key (default: the target's PK value, or its ObjectId if the PK is compound) |
| `lock` | `true` to use the relationship for optimistic locking |

Every `objRelationship` requires a matching `dbRelationship` (or chain) — never add one without the backing DB-layer relationship.

## `<embeddable>` — value object without identity

```xml
<embeddable className="com.example.Address">
    <embeddableAttribute name="street" type="java.lang.String" dbAttributeName="STREET"/>
    <embeddableAttribute name="city"   type="java.lang.String" dbAttributeName="CITY"/>
</embeddable>
```

To embed it inside an ObjEntity, use `<embeddedAttribute>`:

```xml
<objEntity name="User" className="com.example.User" dbEntityName="USER">
    <embeddedAttribute name="homeAddress" type="com.example.Address"/>
    <embeddedAttribute name="workAddress" type="com.example.Address">
        <embeddableAttributeOverride name="street" dbAttributePath="WORK_STREET"/>
        <embeddableAttributeOverride name="city"   dbAttributePath="WORK_CITY"/>
    </embeddedAttribute>
</objEntity>
```

`<embeddableAttributeOverride>` is only needed when the host columns differ from the embeddable's default `dbAttributeName`.

## `<procedure>` — stored procedure

```xml
<procedure name="search_artists">
    <procedureParameter name="name_filter" type="VARCHAR" length="254" direction="in"/>
    <procedureParameter name="result_count" type="INTEGER" direction="out"/>
</procedure>
```

`direction` is `in`, `out`, or `in_out`. `length` and `precision` are optional.

## Named queries

Three flavors, each with its own element, all named with a `name=` attribute:

### `<objectQuery>`

```xml
<objectQuery name="ArtistsByName" cacheStrategy="LOCAL_CACHE">
    <ql><![CDATA[from Artist where artistName like $name order by dateOfBirth desc prefetch paintings]]></ql>
</objectQuery>
```

The `<ql>` String follows the syntax of `ObjectSelect.parse(..)`:
`from Entity [where exp] [order by exp [desc] [insensitive], ...] [limit n] [offset m] [prefetch path [joint|disjoint|disjointById], ...]`.
`$name` placeholders are the query parameters. The root entity, qualifier, orderings, prefetches, limit, offset and
`distinct` (`select distinct self from ...`) all live in the String; other settings are optional attributes
of `<objectQuery>`: `cacheStrategy`, `dataRows`, `pageSize`, `statementFetchSize`. An optional
`<cacheGroup><![CDATA[name]]></cacheGroup>` element follows `<ql>`. A `select` clause with columns, a `having` clause or a `db:` root are not supported in a mapped query.

### `<sqlQuery>`

```xml
<sqlQuery name="LowercasedArtists" root="dataMap" rootName="testmap" columnNameCapitalization="LOWER">
    <sql><![CDATA[select * from ARTIST]]></sql>
    <sql adapterClass="org.apache.cayenne.dba.postgres.PostgresAdapter"><![CDATA[select * from artist]]></sql>
</sqlQuery>
```

The optional settings are attributes of `<sqlQuery>`: `cacheStrategy`, `dataRows`, `pageSize`,
`statementFetchSize`, `columnNameCapitalization` (`DEFAULT`, `UPPER`, `LOWER`). There are no limit and offset
settings, those must be a part of the SQL. An optional `<cacheGroup><![CDATA[name]]></cacheGroup>` element follows the `<sql>`
elements. A `<procedureQuery>` takes the same attributes and `<cacheGroup>`, plus `fetchLimit` and `fetchOffset`.

Use a second `<sql>` with `adapterClass=` to vary by DB adapter. SQL query templates use a Velocity-like syntax — `#bind($paramName)` for parameters.

### `<procedureQuery>`

```xml
<procedureQuery name="SearchArtists" root="procedure" rootName="search_artists" resultEntity="Artist"/>
```

## `<dbImport>` — embedded reverse-engineering config

At most one, after the named queries. Holds the DB import options, edited on the "DB Import" tab of CayenneModeler and
read by the `dbimport_run` MCP tool. See `dbimport-config.md` for fields.

## `<cgen>` — embedded code-gen config

Any number, last in the DataMap, told apart by their `<name>`. Holds the class generation options, edited on the
"Class Generation" tab of CayenneModeler and read by the `cgen_run` MCP tool, which uses the first `<cgen>`. See
`cgen-config.md` for fields.

## Anti-patterns to avoid

- Adding an `objRelationship` without a backing `dbRelationship` — Cayenne will validate-fail at runtime load.
- Setting `dbAttribute` `type` without `length` for VARCHAR/CHAR
- Using a Java primitive (`int`, `long`) for an `objAttribute` `type` when the column is nullable — primitives can't represent NULL; use the wrapper (`java.lang.Integer`).
- Reordering top-level elements — the schema requires the order listed above.
- Hand-editing `_<Entity>` superclass `.java` files — they are regenerated by cgen and will be overwritten. Edit the user subclass instead.
