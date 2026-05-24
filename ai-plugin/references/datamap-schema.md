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

- **Namespace**: `http://cayenne.apache.org/schema/12/modelMap`
- **XSD**: `http://cayenne.apache.org/schema/12/modelMap.xsd`
- **Project version**: `12` (Cayenne 5.0)
- **Working example**: `cayenne-ant/src/test/resources/testmap.map.xml`

## Root element

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-map xmlns="http://cayenne.apache.org/schema/12/modelMap"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://cayenne.apache.org/schema/12/modelMap http://cayenne.apache.org/schema/12/modelMap.xsd"
          project-version="12">
    ...
</data-map>
```

## DataMap properties

Top-level `<property>` children of `<data-map>` configure DataMap-wide settings. The two used in practice:

```xml
<property name="defaultPackage" value="com.example.model"/>
<property name="defaultSuperclass" value="org.apache.cayenne.GenericPersistentObject"/>
```

- `defaultPackage` — Java package for generated classes when an `obj-entity` `className` is a short name.
- `defaultSuperclass` — superclass for generated `_<Name>` superclasses. Default is `BaseDataObject`.

## Element order

The schema enforces this order inside `<data-map>`:

1. `<property>` *
2. `<procedure>` *
3. `<embeddable>` *
4. `<db-entity>` *
5. `<obj-entity>` *
6. `<db-relationship>` *
7. `<obj-relationship>` *
8. `<query>` *
9. `<cgen>` ? (different namespace, embedded)
10. `<dbImport>` ? (different namespace, embedded)

When inserting elements by hand, respect this order or Cayenne's parser will reject the file.

## `<db-entity>` — database table

```xml
<db-entity name="ARTIST" catalog="public" schema="public">
    <db-attribute name="ARTIST_ID" type="BIGINT" isPrimaryKey="true" isMandatory="true"/>
    <db-attribute name="ARTIST_NAME" type="VARCHAR" length="254" isMandatory="true"/>
    <db-attribute name="DATE_OF_BIRTH" type="DATE"/>
</db-entity>
```

Attribute fields:

| Attribute | Meaning |
|---|---|
| `name` | Column name (case-preserved as-is from the DB) |
| `type` | JDBC type name: `INTEGER`, `BIGINT`, `VARCHAR`, `CHAR`, `DATE`, `TIMESTAMP`, `BOOLEAN`, `BIT`, `NUMERIC`, `DECIMAL`, `FLOAT`, `DOUBLE`, `BLOB`, `CLOB`, `VARBINARY`, etc. |
| `length` | Column length (for VARCHAR, CHAR, VARBINARY, NUMERIC) |
| `scale` | Decimal scale (NUMERIC, DECIMAL) |
| `isPrimaryKey` | `true` for PK columns |
| `isMandatory` | `true` for NOT NULL |
| `isGenerated` | `true` for DB-generated columns (identity, sequence) |

## `<obj-entity>` — Java object mapped to a DbEntity

```xml
<obj-entity name="Artist" className="com.example.Artist" dbEntityName="ARTIST">
    <obj-attribute name="artistName" type="java.lang.String" db-attribute-path="ARTIST_NAME"/>
    <obj-attribute name="dateOfBirth" type="java.util.Date" db-attribute-path="DATE_OF_BIRTH"/>
</obj-entity>
```

Element attributes:

| Attribute | Meaning |
|---|---|
| `name` | Object name (used in queries) |
| `className` | Fully-qualified Java class. If using DataMap `defaultPackage`, a short name works. |
| `superClassName` | Optional explicit superclass |
| `dbEntityName` | Matching `<db-entity name="...">` |
| `superEntityName` | For inheritance — name of parent ObjEntity |
| `readOnly` | `true` to disallow writes |
| `abstract` | `true` for abstract entities (single-table inheritance) |

`obj-attribute`:

| Attribute | Meaning |
|---|---|
| `name` | Java property name |
| `type` | Java type, FQN — `java.lang.String`, `java.lang.Integer`, `java.util.Date`, `java.math.BigDecimal`, `byte[]`, `boolean`, etc. |
| `db-attribute-path` | Column name. May be a dotted path through a `db-relationship` for derived attributes: `toArtist.ARTIST_NAME`. |

PK columns are not normally mapped as `obj-attribute` — they're handled implicitly. Map a PK column only if `meaningfulPK` (you want to expose the PK value to the Java side).

## `<db-relationship>` — FK join in the DB layer

```xml
<db-relationship name="paintingArray" source="ARTIST" target="PAINTING" toMany="true">
    <db-attribute-pair source="ARTIST_ID" target="ARTIST_ID"/>
</db-relationship>
```

| Attribute | Meaning |
|---|---|
| `name` | Identifier; referenced by `obj-relationship`'s `db-relationship-path` |
| `source` | Owning DbEntity name |
| `target` | Target DbEntity name |
| `toMany` | `true` for one-to-many or many-to-many side |
| `toDependentPK` | `true` when the target row's PK depends on this FK (typical for one-to-one or master/detail) |

Each `<db-attribute-pair>` is one column of the join. Compound joins use multiple `<db-attribute-pair>` elements.

**Symmetry.** Most FKs need *two* `<db-relationship>` entries, one from each side. They are not auto-derived.

## `<obj-relationship>` — object-layer view of a db-relationship

```xml
<obj-relationship name="paintings"
                  source="Artist"
                  target="Painting"
                  deleteRule="Cascade"
                  db-relationship-path="paintingArray"/>
```

| Attribute | Meaning |
|---|---|
| `name` | Java property name (typically plural for to-many) |
| `source` | Owning ObjEntity name |
| `target` | Target ObjEntity name |
| `deleteRule` | `Nullify`, `Cascade`, `Deny`, or `NoAction` |
| `db-relationship-path` | One or more `<db-relationship>` names, dot-separated for flattened (many-to-many) relationships: `artistGroupArray.toGroup` |

Every `obj-relationship` requires a matching `db-relationship` (or chain) — never add one without the backing DB-layer relationship.

## `<embeddable>` — value object without identity

```xml
<embeddable className="com.example.Address">
    <embeddable-attribute name="street" type="java.lang.String" db-attribute-name="STREET"/>
    <embeddable-attribute name="city"   type="java.lang.String" db-attribute-name="CITY"/>
</embeddable>
```

To embed it inside an ObjEntity, use `<embedded-attribute>`:

```xml
<obj-entity name="User" className="com.example.User" dbEntityName="USER">
    <embedded-attribute name="homeAddress" type="com.example.Address"/>
    <embedded-attribute name="workAddress" type="com.example.Address">
        <embeddable-attribute-override name="street" db-attribute-path="WORK_STREET"/>
        <embeddable-attribute-override name="city"   db-attribute-path="WORK_CITY"/>
    </embedded-attribute>
</obj-entity>
```

`<embeddable-attribute-override>` is only needed when the host columns differ from the embeddable's default `db-attribute-name`.

## `<procedure>` — stored procedure

```xml
<procedure name="search_artists">
    <procedure-parameter name="name_filter" type="VARCHAR" length="254" direction="in"/>
    <procedure-parameter name="result_count" type="INTEGER" direction="out"/>
</procedure>
```

`direction` is `in`, `out`, or `in_out`.

## `<query>` — named query

Three flavors, all named with `<query name="...">` and selected by `type=`:

### SelectQuery

```xml
<query name="ArtistsByName" type="SelectQuery" root="obj-entity" root-name="Artist">
    <property name="cayenne.GenericSelectQuery.cacheStrategy" value="LOCAL_CACHE"/>
    <qualifier><![CDATA[artistName like $name]]></qualifier>
    <ordering descending="true"><![CDATA[dateOfBirth]]></ordering>
    <prefetch>paintings</prefetch>
</query>
```

### SQLTemplate

```xml
<query name="LowercasedArtists" type="SQLTemplate" root="data-map" root-name="testmap">
    <property name="cayenne.SQLTemplate.columnNameCapitalization" value="LOWER"/>
    <sql><![CDATA[select * from ARTIST]]></sql>
    <sql adapter-class="org.apache.cayenne.dba.postgres.PostgresAdapter"><![CDATA[select * from artist]]></sql>
</query>
```

Use a second `<sql>` with `adapter-class=` to vary by DB adapter. SQLTemplate placeholders use Velocity syntax — `#bind($paramName)` for parameters.

### EJBQLQuery

```xml
<query name="ArtistByName" type="EJBQLQuery">
    <ejbql><![CDATA[select a from Artist a where a.artistName = ?1]]></ejbql>
</query>
```

### ProcedureQuery

```xml
<query name="SearchArtists" type="ProcedureQuery" root="procedure" root-name="search_artists" result-entity="Artist"/>
```

## `<cgen>` — embedded code-gen config

A separate namespace, embedded directly in the DataMap. See `cgen-config.md` for fields.

## `<dbImport>` — embedded reverse-engineering config

A separate namespace, used by the Modeler's reverse-engineering dialog to persist its options. See `dbimport-config.md` for fields.

## Anti-patterns to avoid

- Adding an `obj-relationship` without a backing `db-relationship` — Cayenne will validate-fail at runtime load.
- Setting `db-attribute` `type` without `length` for VARCHAR/CHAR
- Using a Java primitive (`int`, `long`) for an `obj-attribute` `type` when the column is nullable — primitives can't represent NULL; use the wrapper (`java.lang.Integer`).
- Reordering top-level elements — the schema requires the order listed above.
- Hand-editing `_<Entity>` superclass `.java` files — they are regenerated by cgen and will be overwritten. Edit the user subclass instead.
