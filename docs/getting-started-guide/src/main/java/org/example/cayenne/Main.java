/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
// The parts of this class are included in the tutorial docs. "main-empty" and "main-runtime" tags assemble the versions
// of this class at the different tutorial steps
// tag::main-runtime[]
// tag::main-empty[]
package org.example.cayenne;

// end::main-empty[]
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.datasource.CayenneDataSource;
import org.apache.cayenne.runtime.CayenneRuntime;

import javax.sql.DataSource;
// end::main-runtime[]
import org.apache.cayenne.query.ObjectSelect;
import org.example.cayenne.persistent.Artist;
import org.example.cayenne.persistent.Gallery;
import org.example.cayenne.persistent.Painting;

import java.time.LocalDate;
import java.util.List;
// tag::main-runtime[]

// tag::main-empty[]
public class Main {

    public static void main(String[] args) {
        // end::main-empty[]

        // an in-memory Derby database
        DataSource dataSource = CayenneDataSource.of("jdbc:derby:memory:testdb;create=true")
                .pool(1, 1)
                .build();

        // let Cayenne create the DB schema from the mapping, if it is not there yet
        DataNodeDescriptor dataNode = DataNodeDescriptor.of("tutorial")
                .dataSource(dataSource)
                .createSchemaIfNeeded()
                .build();

        CayenneRuntime cayenneRuntime = CayenneRuntime.of()
                .addConfig("cayenne-project.xml")
                .defaultDataNode(dataNode)
                .build();

        ObjectContext context = cayenneRuntime.newContext();
        // end::main-runtime[]

        newObjectsTutorial(context);
        selectTutorial(context);
        deleteTutorial(context);
        // tag::main-runtime[]
        // tag::main-empty[]
    }
    // end::main-empty[]
    // end::main-runtime[]

    static void newObjectsTutorial(ObjectContext context) {

        // creating new Artist
        // tag::new-artist[]
        Artist picasso = context.newObject(Artist.class);
        picasso.setName("Pablo Picasso");
        picasso.setDateOfBirthString("18811025");
        // end::new-artist[]

        // Creating other objects
        // tag::new-painting[]
        Gallery metropolitan = context.newObject(Gallery.class);
        metropolitan.setName("Metropolitan Museum of Art");
        Painting girl = context.newObject(Painting.class);
        girl.setName("Girl Reading at a Table");
        Painting stein = context.newObject(Painting.class);
        stein.setName("Gertrude Stein");
        // end::new-painting[]

        // connecting objects together via relationships
        // tag::link-objects[]
        picasso.addToPaintings(girl);
        picasso.addToPaintings(stein);
        girl.setGallery(metropolitan);
        stein.setGallery(metropolitan);
        // end::link-objects[]

        // saving all the changes above
        // tag::commit[]
        context.commitChanges();
        // end::commit[]
    }

    static void selectTutorial(ObjectContext context) {
        // ObjectSelect examples
        // tag::select-all[]
        List<Painting> paintings1 = ObjectSelect.query(Painting.class).select(context);
        // end::select-all[]

        // tag::select-like[]
        List<Painting> paintings2 = ObjectSelect.query(Painting.class)
                .where(Painting.NAME.likeIgnoreCase("gi%")).select(context);
        // end::select-like[]

        // tag::select-path[]
        List<Painting> paintings3 = ObjectSelect.query(Painting.class)
                .where(Painting.ARTIST.dot(Artist.DATE_OF_BIRTH).lt(LocalDate.of(1900, 1, 1)))
                .select(context);
        // end::select-path[]
    }

    static void deleteTutorial(ObjectContext context) {
        // Delete object examples
        // tag::delete-select[]
        Artist picasso = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.eq("Pablo Picasso")).selectOne(context);
        // end::delete-select[]

        // tag::delete[]
        if (picasso != null) {
            context.deleteObject(picasso);
            context.commitChanges();
        }
        // end::delete[]
    }
// tag::main-runtime[]
// tag::main-empty[]
}
// end::main-empty[]
// end::main-runtime[]
