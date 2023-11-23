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
package org.apache.cayenne.tutorial;

import java.time.LocalDate;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.tutorial.persistent.Artist;
import org.apache.cayenne.tutorial.persistent.Gallery;
import org.apache.cayenne.tutorial.persistent.Painting;

public class Main {

    public static void main(String[] args) {

        // starting Cayenne
        CayenneRuntime cayenneRuntime = CayenneRuntime.builder()
                .addConfig("cayenne-project.xml")
                .build();

        // getting a hold of ObjectContext
        ObjectContext context = cayenneRuntime.newContext();

        newObjectsTutorial(context);
        selectTutorial(context);
        deleteTutorial(context);
    }

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
        // SelectQuery examples
        List<Painting> paintings1 = ObjectSelect.query(Painting.class).select(context);

        List<Painting> paintings2 = ObjectSelect.query(Painting.class)
                .where(Painting.NAME.likeIgnoreCase("gi%")).select(context);

        List<Painting> paintings3 = ObjectSelect.query(Painting.class)
                .where(Painting.ARTIST.dot(Artist.DATE_OF_BIRTH)
                        .lt(LocalDate.of(1900,1,1))).select(context);
    }

    static void deleteTutorial(ObjectContext context) {
        // Delete object examples
        Artist picasso = ObjectSelect.query(Artist.class)
                .where(Artist.NAME.eq("Pablo Picasso")).selectOne(context);

        if (picasso != null) {
            context.deleteObjects(picasso);
            context.commitChanges();
        }
    }
}
