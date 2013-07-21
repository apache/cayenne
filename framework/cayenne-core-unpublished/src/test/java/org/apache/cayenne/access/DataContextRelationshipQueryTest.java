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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextRelationshipQueryTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tArtist;
    private TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("PAINTING1");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns("PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID");
    }

    private void createOneArtistOnePaintingDataSet() throws Exception {

        tArtist.insert(1, "a1");
        tPainting.insert(1, "p1", 1);
    }

    public void testUnrefreshingToOne() throws Exception {

        createOneArtistOnePaintingDataSet();

        Painting p = Cayenne.objectForPK(context, Painting.class, 1);

        // resolve artist once before running non-refreshing query, to check that we do
        // not refresh the object

        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        long v = a.getSnapshotVersion();
        int writeCalls = a.getPropertyWrittenDirectly();
        assertEquals("a1", a.getArtistName());

        assertEquals(1, tArtist
                .update()
                .set("ARTIST_NAME", "a2")
                .where("ARTIST_ID", 1)
                .execute());

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

    public void testRefreshingToOne() throws Exception {

        createOneArtistOnePaintingDataSet();

        Painting p = Cayenne.objectForPK(context, Painting.class, 1);

        // resolve artist once before running non-refreshing query, to check that we do
        // not refresh the object

        Artist a = Cayenne.objectForPK(context, Artist.class, 1);
        long v = a.getSnapshotVersion();
        int writeCalls = a.getPropertyWrittenDirectly();
        assertEquals("a1", a.getArtistName());

        assertEquals(1, tArtist
                .update()
                .set("ARTIST_NAME", "a2")
                .where("ARTIST_ID", 1)
                .execute());

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
