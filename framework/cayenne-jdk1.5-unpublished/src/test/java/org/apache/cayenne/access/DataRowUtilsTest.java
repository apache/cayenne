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
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataRowUtilsTest extends CayenneCase {

    protected DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testMerge() throws Exception {
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        String n1 = "changed";
        String n2 = "changed again";

        SelectQuery artistQ = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'artist1'"));
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
        getAccessStack().createTestData(DataContextCase.class, "testArtists", null);

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Painting");
        ArcProperty toArtist = (ArcProperty) d.getProperty("toArtist");

        SelectQuery artistQ = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'artist2'"));
        Artist anotherArtist = (Artist) context.performQuery(artistQ).get(0);
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("PX");
        painting.setToArtist(anotherArtist);

        context.commitChanges();

        artistQ = new SelectQuery(Artist.class, Expression
                .fromString("artistName = 'artist1'"));
        Artist artist = (Artist) context.performQuery(artistQ).get(0);
        assertNotSame(artist, painting.getToArtist());

        ObjectDiff diff = context.getObjectStore().registerDiff(
                painting.getObjectId(),
                null);

        assertFalse(DataRowUtils.isToOneTargetModified(toArtist, painting, diff));

        painting.setToArtist(artist);
        assertTrue(DataRowUtils.isToOneTargetModified(toArtist, painting, diff));
    }

    public void testIsToOneTargetModifiedWithNewTarget() throws Exception {
        createTestData("testIsToOneTargetModifiedWithNewTarget");

        // add NEW gallery to painting
        List paintings = context.performQuery(new SelectQuery(Painting.class));
        assertEquals(1, paintings.size());
        Painting p1 = (Painting) paintings.get(0);

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Painting");
        ArcProperty toGallery = (ArcProperty) d.getProperty("toGallery");

        ObjectDiff diff = context.getObjectStore().registerDiff(p1.getObjectId(), null);
        assertFalse(DataRowUtils.isToOneTargetModified(toGallery, p1, diff));

        Gallery g1 = (Gallery) context.newObject("Gallery");
        g1.addToPaintingArray(p1);
        assertTrue(DataRowUtils.isToOneTargetModified(toGallery, p1, diff));
    }
}
