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
# Project descriptor XML schema (`cayenne-*.xml`)

Reference for editing the top-level Cayenne project descriptor.

- **Namespace**: `http://cayenne.apache.org/schema/15/project`
- **XSD**: `http://cayenne.apache.org/schema/15/project.xsd`
- **Cayenne version**: 6.0. The schema version is the number in the namespace; there is no `project-version`
  attribute anymore. Descriptors of older versions (including the 5.0-era `<domain>` root) are upgraded
  automatically when loaded.

## Minimal descriptor

```xml
<?xml version="1.0" encoding="utf-8"?>
<project xmlns="http://cayenne.apache.org/schema/15/project"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://cayenne.apache.org/schema/15/project https://cayenne.apache.org/schema/15/project.xsd">
    <map name="mydb"/>
</project>
```

This references a sibling `mydb.map.xml`.

## Contents

The descriptor is a list of DataMaps plus two project-wide switches. That is all the schema allows.

| Element/attribute | Meaning |
|---|---|
| `<map name="...">` | References a sibling DataMap file. Must match the `*.map.xml` file name minus the extension. A descriptor may list any number of maps. |
| `sharedCache="false"` | Attribute of `<project>`. Turns off the snapshot cache shared between all `ObjectContext`s. Default `true`. |
| `validateOnCommit="false"` | Attribute of `<project>`. Turns off object validation before commit. Default `true`. |

```xml
<project xmlns="http://cayenne.apache.org/schema/15/project" ...
         sharedCache="true"
         validateOnCommit="true">
    <map name="customers"/>
    <map name="orders"/>
</project>
```

All referenced `*.map.xml` files must sit in the same directory as the descriptor.

## Where the database connection goes

**Not here.** DataNodes (database connections) are not a part of the XML project. They are defined in code when the
runtime is created: `CayenneRuntimeBuilder.defaultDataNode(..)` and `addDataNode(..)`, see `runtime-api.md`.
This keeps DB credentials out of XML and lets the app pick the DataSource per environment (test, dev, prod).

The `<node>`, `<map-ref>` and `<data-source>` elements of the 4.x descriptors are gone. An old descriptor that
still has them can't be upgraded in memory by the runtime: open it in CayenneModeler, which drops the nodes and
reports each removal, and move the connection into the code.

## Anti-patterns

- Adding `<node>` or `<data-source>` elements, or a `project-version` attribute, from memory of older Cayenne
  versions — the schema rejects them.
- Referencing a DataMap that doesn't exist as a sibling file — the runtime will fail to load.
