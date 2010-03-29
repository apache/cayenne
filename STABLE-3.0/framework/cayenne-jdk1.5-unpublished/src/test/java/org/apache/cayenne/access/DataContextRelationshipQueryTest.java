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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextRelationshipQueryTest extends CayenneCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testUnrefreshingToOne() {

        ObjectContext context = createDataContext();

        QueryChain chain = new QueryChain();
        chain.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a1')"));
        chain
                .addQuery(new SQLTemplate(
                        Painting.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (1, 1, 'p1')"));

        context.performQuery(chain);

        Painting p = DataObjectUtils.objectForPK(context, Painting.class, 1);

        // resolve artist once before running non-refreshing query, to check that we do
        // not refresh the object

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 1);
        long v = a.getSnapshotVersion();
        int writeCalls = a.getPropertyWrittenDirectly();
        assertEquals("a1", a.getArtistName());

        context.performQuery(new SQLTemplate(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = 'a2' WHERE ARTIST_ID = 1"));

        RelationshipQuery toOne = new RelationshipQuery(
                p.getObjectId(),
                Painting.TO_ARTIST_PROPERTY,
                false);

        List<Artist> related = context.performQuery(toOne);
        assertEquals(1, related.size());
        assertTrue(related.contains(a));
        assertEquals("a1", a.getArtistName());
        assertEquals(v, a.getSnapshotVersion());
        assertEquals(
                "Looks like relationship query caused snapshot refresh",
                writeCalls,
                a.getPropertyWrittenDirectly());
    }

    public void testRefreshingToOne() {

        ObjectContext context = createDataContext();

        QueryChain chain = new QueryChain();
        chain.addQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a1')"));
        chain
                .addQuery(new SQLTemplate(
                        Painting.class,
                        "INSERT INTO PAINTING (PAINTING_ID, ARTIST_ID, PAINTING_TITLE) VALUES (1, 1, 'p1')"));

        context.performQuery(chain);

        Painting p = DataObjectUtils.objectForPK(context, Painting.class, 1);

        // resolve artist once before running non-refreshing query, to check that we do
        // not refresh the object

        Artist a = DataObjectUtils.objectForPK(context, Artist.class, 1);
        long v = a.getSnapshotVersion();
        int writeCalls = a.getPropertyWrittenDirectly();
        assertEquals("a1", a.getArtistName());

        context.performQuery(new SQLTemplate(
                Artist.class,
                "UPDATE ARTIST SET ARTIST_NAME = 'a2' WHERE ARTIST_ID = 1"));

        RelationshipQuery toOne = new RelationshipQuery(
                p.getObjectId(),
                Painting.TO_ARTIST_PROPERTY,
                true);

        List<Artist> related = context.performQuery(toOne);
        assertEquals(1, related.size());
        assertTrue(related.contains(a));
        assertEquals("a2", a.getArtistName());
        assertTrue("Looks like relationship query didn't cause a snapshot refresh", v < a
                .getSnapshotVersion());
        assertTrue(
                "Looks like relationship query didn't cause a snapshot refresh",
                writeCalls < a.getPropertyWrittenDirectly());
    }
}
