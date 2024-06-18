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
package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectIdQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextObjectIdQueryIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testRefreshNullifiedValuesNew() {

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");
        a.setDateOfBirth(new Date());

        context.commitChanges();

        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "UPDATE ARTIST SET DATE_OF_BIRTH = NULL"));

        long id = Cayenne.longPKForObject(a);
        ObjectIdQuery query = new ObjectIdQuery(ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                id), false, ObjectIdQuery.CACHE_REFRESH);

        Artist a1 = (Artist) Cayenne.objectForQuery(context, query);
        assertNull(a1.getDateOfBirth());
        assertEquals("X", a1.getArtistName());
    }

    @Test
    public void testNoRefreshValuesNew() {

        Artist a = context.newObject(Artist.class);
        a.setArtistName("X");

        context.commitChanges();

        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = 'Y'"));

        long id = Cayenne.longPKForObject(a);
        ObjectIdQuery query = new ObjectIdQuery(ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                id), false, ObjectIdQuery.CACHE);

        Artist a1 = (Artist) Cayenne.objectForQuery(context, query);
        assertEquals("X", a1.getArtistName());
    }

    @Test
    public void testRefreshNullifiedValuesExisting() {

        SQLTemplate insert = new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, DATE_OF_BIRTH) VALUES (44, 'X', #bind($date 'DATE'))");
        insert.setParameters(Collections.singletonMap("date", new Date()));

        context.performGenericQuery(insert);

        Artist a = Cayenne.objectForPK(context, Artist.class, 44L);
        assertNotNull(a.getDateOfBirth());
        assertEquals("X", a.getArtistName());

        context.performGenericQuery(new SQLTemplate(
                Artist.class,
                "UPDATE ARTIST SET DATE_OF_BIRTH = NULL"));

        ObjectIdQuery query = new ObjectIdQuery(ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                44L), false, ObjectIdQuery.CACHE_REFRESH);

        Artist a1 = (Artist) Cayenne.objectForQuery(context, query);
        assertNull(a1.getDateOfBirth());
        assertEquals("X", a1.getArtistName());
    }
}
