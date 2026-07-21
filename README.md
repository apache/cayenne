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
# Apache Cayenne

[![Maven Central](https://img.shields.io/maven-central/v/org.apache.cayenne/cayenne-server/4.2.svg)](https://cayenne.apache.org/download/)
[![Build Status](https://github.com/apache/cayenne/actions/workflows/verify-deploy-on-push.yml/badge.svg?branch=master)](https://github.com/apache/cayenne/actions/workflows/verify-deploy-on-push.yml)


[Apache Cayenne](https://cayenne.apache.org) is an open source persistence framework for Java. With a wealth of unique and powerful features, Cayenne can address a wide range of persistence needs. Cayenne seamlessly binds one or more database schemas to Java objects, managing atomic commits and rollbacks, SQL generation, joins, sequences, and more.

Cayenne supports database reverse engineering and generation, as well as templated class generation. All of these functions can be controlled through the GUI CayenneModeler app. An entire database schema can be mapped directly to Java objects within minutes, all from the comfort of the GUI-based CayenneModeler.

Cayenne supports numerous other features, including caching, an object query syntax, relationship pre-fetching, on-demand object and relationship faulting, object inheritance, database auto-detection, and generic persistent objects. Cayenne can scale up or down to virtually any project size.


## Quick Links

* [Getting Started DB-First](https://cayenne.apache.org/docs/5.0/getting-started-db-first/)
* [Getting Started](https://cayenne.apache.org/docs/5.0/getting-started-guide/)
* [Documentation](https://cayenne.apache.org/docs/5.0/cayenne-guide/)
* [Upgrading from Older Cayenne](https://github.com/apache/cayenne/blob/master/UPGRADE.md)
* [Bug/Feature Tracker](https://issues.apache.org/jira/browse/CAY)

## Quick Start

### Create XML Mapping

Downloaded Cayenne Modeler from https://cayenne.apache.org/download/, start it and create a Cayenne project.

### Include Cayenne in a Project

Maven
```xml
<dependencies>
    <dependency>
        <groupId>org.apache.cayenne</groupId>
        <artifactId>cayenne-server</artifactId>
        <version>5.0-M2</version>
    </dependency>
</dependencies>
```

Gradle
```gradle
compile group: 'org.apache.cayenne', name: 'cayenne-server', version: '5.0-M1'
 
// or, if Gradle plugin is used
compile cayenne.dependency('server')
```

#### Create Cayenne Runtime

```java
CayenneRuntime cayenneRuntime = CayenneRuntime.builder()
    .addConfig("cayenne-demo.xml")
    .dataSource(CayenneDataSource
             .of("jdbc:mysql://localhost:3306/cayenne_demo")
             .userName("username")
             .password("password")
             .build())
    .build();
```

### Create New Objects

```java
ObjectContext context = cayenneRuntime.newContext();

Artist picasso = context.newObject(Artist.class);
picasso.setName("Pablo Picasso");
picasso.setDateOfBirth(LocalDate.of(1881, 10, 25));

Gallery metropolitan = context.newObject(Gallery.class);
metropolitan.setName("Metropolitan Museum of Art");

Painting girl = context.newObject(Painting.class);
girl.setName("Girl Reading at a Table");

Painting stein = context.newObject(Painting.class);
stein.setName("Gertrude Stein");

picasso.addToPaintings(girl);
picasso.addToPaintings(stein);

girl.setGallery(metropolitan);
stein.setGallery(metropolitan);

context.commitChanges();
```

### Queries

#### Select Objects

```java
List<Painting> paintings = ObjectSelect.query(Painting.class)
        .where(Painting.ARTIST.dot(Artist.DATE_OF_BIRTH).year().lt(1900))
        .prefetch(Painting.ARTIST.joint())
        .select(context);
```

#### Aggregate Functions

```java
// this is artificial property signaling that we want to get full object
Property<Artist> artistProperty = Property.createSelf(Artist.class);

List<Object[]> artistAndPaintingCount = ObjectSelect.columnQuery(Artist.class, artistProperty, Artist.PAINTING_ARRAY.count())
    .where(Artist.ARTIST_NAME.like("a%"))
    .having(Artist.PAINTING_ARRAY.count().lt(5L))
    .orderBy(Artist.PAINTING_ARRAY.count().desc(), Artist.ARTIST_NAME.asc())
    .select(context);

for(Object[] next : artistAndPaintingCount) {
    Artist artist = (Artist)next[0];
    long paintingsCount = (Long)next[1];
    System.out.println(artist.getArtistName() + " has " + paintingsCount + " painting(s)");
}

```

#### Raw SQL Queries

```java
// Selecting objects
List<Painting> paintings = SQLSelect
    .query(Painting.class, "SELECT * FROM PAINTING WHERE PAINTING_TITLE LIKE #bind($title)")
    .params("title", "painting%")
    .upperColumnNames()
    .localCache()
    .limit(100)
    .select(context);

// Selecting scalar values
List<String> paintingNames = SQLSelect
    .scalarQuery(String.class, "SELECT PAINTING_TITLE FROM PAINTING WHERE ESTIMATED_PRICE > #bind($price)")
    .params("price", 100000)
    .select(context);

// Insert values
int inserted = SQLExec
    .query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))")
    .paramsArray(55, "Picasso")
    .update(context);

```

## Collaboration and Support
* [Mailing lists](https://cayenne.apache.org/mailing-lists.html)
* [Support](https://cayenne.apache.org/support.html)
* [Contributing](https://cayenne.apache.org/how-can-i-help.html)

