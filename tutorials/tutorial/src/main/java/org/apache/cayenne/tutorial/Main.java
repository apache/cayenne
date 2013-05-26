/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.tutorial;

import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.tutorial.persistent.Artist;
import org.apache.cayenne.tutorial.persistent.Gallery;
import org.apache.cayenne.tutorial.persistent.Painting;

public class Main {

    public static void main(String[] args) {

        // starting Cayenne
        ServerRuntime cayenneRuntime = new ServerRuntime("cayenne-project.xml");

        // getting a hold of ObjectContext
        ObjectContext context = cayenneRuntime.newContext();

        newObjectsTutorial(context);
        selectTutorial(context);
        deleteTutorial(context);
    }

    static void newObjectsTutorial(ObjectContext context) {

        // creating new Artist
        Artist picasso = context.newObject(Artist.class);
        picasso.setName("Pablo Picasso");
        picasso.setDateOfBirthString("18811025");

        // Creating other objects
        Gallery metropolitan = context.newObject(Gallery.class);
        metropolitan.setName("Metropolitan Museum of Art");

        Painting girl = context.newObject(Painting.class);
        girl.setName("Girl Reading at a Table");

        Painting stein = context.newObject(Painting.class);
        stein.setName("Gertrude Stein");

        // connecting objects together via relationships
        picasso.addToPaintings(girl);
        picasso.addToPaintings(stein);

        girl.setGallery(metropolitan);
        stein.setGallery(metropolitan);

        // saving all the changes above
        context.commitChanges();
    }

    static void selectTutorial(ObjectContext context) {
        // SelectQuery examples
        SelectQuery<Painting> select1 = SelectQuery.query(Painting.class, null);
        List<Painting> paintings1 = context.select(select1);

        Expression qualifier2 = Painting.NAME.likeInsensitive("gi%");
        SelectQuery<Painting> select2 = SelectQuery.query(Painting.class, qualifier2);
        List<Painting> paintings2 = context.select(select2);

        Calendar c = new GregorianCalendar();
        c.set(c.get(Calendar.YEAR) - 100, 0, 1, 0, 0, 0);

        Expression qualifier3 = Expression.fromString("artist.dateOfBirth < $date");
        qualifier3 = qualifier3.expWithParameters(Collections.singletonMap("date", c.getTime()));
        SelectQuery<Painting> select3 = SelectQuery.query(Painting.class, qualifier3);
        List<Painting> paintings3 = context.select(select3);
    }

    static void deleteTutorial(ObjectContext context) {
        // Delete object examples
        Expression qualifier = Artist.NAME.eq("Pablo Picasso");
        SelectQuery<Artist> selectToDelete = SelectQuery.query(Artist.class, qualifier);
        Artist picasso = (Artist) Cayenne.objectForQuery(context, selectToDelete);

        if (picasso != null) {
            context.deleteObjects(picasso);
            context.commitChanges();
        }
    }
}
