<!--
	Licensed to the Apache Software Foundation (ASF) under one
	or more contributor license agreements.  See the NOTICE file
	distributed with this work for additional information
	regarding copyright ownership.  The ASF licenses this file
	to you under the Apache License, Version 2.0 (the
	"License"); you may not use this file except in compliance
	with the License.  You may obtain a copy of the License at
	
	http://www.apache.org/licenses/LICENSE-2.0
	
	Unless required by applicable law or agreed to in writing,
	software distributed under the License is distributed on an
	"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	KIND, either express or implied.  See the License for the
	specific language governing permissions and limitations
	under the License.   
-->
Apache Cayenne
==============

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.cayenne/cayenne-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.apache.cayenne/cayenne-server/)
[![Build Status](https://travis-ci.org/apache/cayenne.svg)](https://travis-ci.org/apache/cayenne)
<!-- [![Build Status](https://builds.apache.org/job/cayenne-master/badge/icon)](https://builds.apache.org/view/All/job/cayenne-master/) -->

<p align="center">
    <a href="https://cayenne.apache.org"><img src="https://cayenne.apache.org/img/cayenne_illustr3-30e8b8fa06.png" alt="Apache Cayenne Logo"/></a>
</p>

[Apache Cayenne](https://cayenne.apache.org) is an open source persistence framework licensed under the Apache License, providing object-relational mapping (ORM) and remoting services. 

Quick Start
----------------

#### Modeler GUI application

![Modeler](https://cayenne.apache.org/docs/4.0/getting-started-guide/images/modeler-deleterule.png)

See tutorial https://cayenne.apache.org/docs/4.1/getting-started-guide/ 

#### Include Cayenne into project

##### Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.apache.cayenne</groupId>
        <artifactId>cayenne-server</artifactId>
        <version>4.0.B2</version>
    </dependency>
    <dependency>
        <groupId>org.apache.cayenne</groupId>
        <artifactId>cayenne-java8</artifactId>
        <version>4.0.B2</version>
    </dependency>
</dependencies>
```

##### Gradle

```groovy
compile group: 'org.apache.cayenne', name: 'cayenne-server', version: '4.0.B2'
compile group: 'org.apache.cayenne', name: 'cayenne-java8', version: '4.0.B2'
```

#### Create Cayenne Runtime

```java
ServerRuntime cayenneRuntime = ServerRuntime.builder()
    .addConfig("cayenne-project.xml")
    .build();
```

#### Create New Objects

```java
ObjectContext context = cayenneRuntime.newContext();

Artist picasso = context.newObject(Artist.class);
picasso.setName("Pablo Picasso");
picasso.setDateOfBirth(LocalDate.of(1881, 10, 25));

Painting girl = context.newObject(Painting.class);
girl.setName("Girl Reading at a Table");
girl.setArtist(picasso);

context.commitChanges();
```

#### Select Objects

```java
List<Painting> paintings = ObjectSelect.query(Painting.class)
        .where(Painting.ARTIST.dot(Artist.DATE_OF_BIRTH).lt(LocalDate.of(1900, 1, 1)))
        .select(context);
```

Documentation
----------------

#### Getting Started

https://cayenne.apache.org/docs/4.0/getting-started-guide/

#### Full documentation

https://cayenne.apache.org/docs/4.0/cayenne-guide/

#### JavaDoc

https://cayenne.apache.org/docs/4.0/api/

About
-----

With a wealth of unique and powerful features, Cayenne can address a wide range of persistence needs. Cayenne seamlessly binds one or more database schemas directly to Java objects, managing atomic commit and rollbacks, SQL generation, joins, sequences, and more. With Cayenne's Remote Object Persistence, those Java objects can even be persisted out to clients via Web Services.

Cayenne is designed to be easy to use, without sacrificing flexibility or design. To that end, Cayenne supports database reverse engineering and generation, as well as a Velocity-based class generation engine. All of these functions can be controlled directly through the CayenneModeler, a fully functional GUI tool. No cryptic XML or annotation based configuration is required! An entire database schema can be mapped directly to Java objects within minutes, all from the comfort of the GUI-based CayenneModeler.

Cayenne supports numerous other features, including caching, a complete object query syntax, relationship pre-fetching, on-demand object and relationship faulting, object inheritance, database auto-detection, and generic persisted objects. Most importantly, Cayenne can scale up or down to virtually any project size. With a mature, 100% open source framework, an energetic user community, and a track record of solid performance in high-volume environments, Cayenne is an exceptional choice for persistence services.

License
---------
Cayenne is available as free and open source under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Collaboration
--------------

* [Bug/Feature Tracker](https://issues.apache.org/jira/browse/CAY)
* [Mailing lists](https://cayenne.apache.org/mailing-lists.html)
* [Support](https://cayenne.apache.org/support.html)
