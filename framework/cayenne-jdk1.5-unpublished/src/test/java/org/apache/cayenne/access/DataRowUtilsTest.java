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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataRowUtilsTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE");
    }

    protected void createOneArtist() throws Exception {
        tArtist.insert(11, "artist2");
    }

    protected void createOneArtistAndOnePainting() throws Exception {
        tArtist.insert(11, "artist2");
        tPainting.insert(6, "p_artist2", 11, 1000);
    }

    public void testMerge() throws Exception {
        createOneArtist();

        String n1 = "changed";
        String n2 = "changed again";

        SelectQuery artistQ = new SelectQuery(Artist.class);
        Artist a1 = (Artist) context.performQuery(artistQ).get(0);
        a1.setArtistName(n1);

        DataRow s2 = new DataRow(2);
        s2.put("ARTIST_NAME", n2);
        s2.put("DATE_OF_BIRTH", new java.util.Date());

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Artist");
        DataRowUtils.mergeObjectWithSnapshot(context, d, a1, s2);

        // name was modified, so it should not change during merge
        assertEquals(n1, a1.getArtistName());

        // date of birth came from database, it should be updated during merge
        assertEquals(s2.get("DATE_OF_BIRTH"), a1.getDateOfBirth());
    }

    public void testIsToOneTargetModified() throws Exception {
        createOneArtist();

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Painting");
        ArcProperty toArtist = (ArcProperty) d.getProperty("toArtist");

        Artist artist2 = (Artist) context
                .performQuery(new SelectQuery(Artist.class))
                .get(0);
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("PX");
        painting.setToArtist(artist2);

        context.commitChanges();

        tArtist.insert(119, "artist3");
        SelectQuery query = new SelectQuery(Artist.class, ExpressionFactory.matchExp(
                Artist.ARTIST_NAME_PROPERTY,
                "artist3"));
        Artist artist3 = (Artist) context.performQuery(query).get(0);
        assertNotSame(artist3, painting.getToArtist());

        ObjectDiff diff = context.getObjectStore().registerDiff(
                painting.getObjectId(),
                null);

        assertFalse(DataRowUtils.isToOneTargetModified(toArtist, painting, diff));

        painting.setToArtist(artist3);
        assertTrue(DataRowUtils.isToOneTargetModified(toArtist, painting, diff));
    }

    public void testIsToOneTargetModifiedWithNewTarget() throws Exception {
        createOneArtistAndOnePainting();

        // add NEW gallery to painting
        List<Painting> paintings = context.performQuery(new SelectQuery(Painting.class));
        assertEquals(1, paintings.size());
        Painting p1 = paintings.get(0);

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Painting");
        ArcProperty toGallery = (ArcProperty) d.getProperty("toGallery");

        ObjectDiff diff = context.getObjectStore().registerDiff(p1.getObjectId(), null);
        assertFalse(DataRowUtils.isToOneTargetModified(toGallery, p1, diff));

        Gallery g1 = (Gallery) context.newObject("Gallery");
        g1.addToPaintingArray(p1);
        assertTrue(DataRowUtils.isToOneTargetModified(toGallery, p1, diff));
    }
}
