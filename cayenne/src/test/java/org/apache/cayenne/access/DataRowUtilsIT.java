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

import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataRowUtilsIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected DataContext context;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    
    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING", "PAINTING_ID",
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

    @Test
    public void isTargetDeterminedByFK() {
        EntityResolver resolver = context.getEntityResolver();
        ObjEntity painting = resolver.getObjEntity("Painting");
        ObjEntity artist = resolver.getObjEntity("Artist");
        ObjEntity paintingInfo = resolver.getObjEntity("PaintingInfo");

        // referential integrity guarantees the target of an FK on the source
        assertTrue(DataRowUtils.isTargetDeterminedByFK(painting.getRelationship("artist"), resolver));
        assertTrue(DataRowUtils.isTargetDeterminedByFK(paintingInfo.getRelationship("painting"), resolver));

        // a dependent row, to-many targets and flattened targets may be missing
        assertFalse(DataRowUtils.isTargetDeterminedByFK(painting.getRelationship("paintingInfo"), resolver));
        assertFalse(DataRowUtils.isTargetDeterminedByFK(artist.getRelationship("paintings"), resolver));
        assertFalse(DataRowUtils.isTargetDeterminedByFK(artist.getRelationship("groups"), resolver));
    }

    @Test
    public void merge() throws Exception {
        createOneArtist();

        String n1 = "changed";
        String n2 = "changed again";

        Artist a1 = ObjectSelect.query(Artist.class)
                .select(context)
                .get(0);
        a1.setArtistName(n1);

        DataRow s2 = DataRow.of("Artist", DataRowLayout.of("ARTIST_NAME", "DATE_OF_BIRTH"),
                new Object[]{n2, new java.util.Date()}, SnapshotVersion.next());

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Artist");
        DataRowUtils.mergeObjectWithSnapshot(context, a1, s2, MergePlan.of(d));

        // name was modified, so it should not change during merge
        assertEquals(n1, a1.getArtistName());

        // date of birth came from database, it should be updated during merge
        assertEquals(s2.get("DATE_OF_BIRTH"), a1.getDateOfBirth());
    }

    @Test
    public void isToOneTargetModified() throws Exception {
        createOneArtist();

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Painting");
        ArcProperty artist = (ArcProperty) d.getProperty("artist");

        Artist artist2 = ObjectSelect.query(Artist.class).selectFirst(context);
        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("PX");
        painting.setArtist(artist2);

        context.commitChanges();

        tArtist.insert(119, "artist3");
        Artist artist3 = ObjectSelect.query(Artist.class)
                .where(Artist.ARTIST_NAME.eq("artist3"))
                .selectFirst(context);
        assertNotSame(artist3, painting.getArtist());

        ObjectDiff diff = context.getObjectStore().registerDiff(
                painting.getObjectId(),
                null);

        assertFalse(DataRowUtils.isToOneTargetModified(artist, painting, diff));

        painting.setArtist(artist3);
        assertTrue(DataRowUtils.isToOneTargetModified(artist, painting, diff));
    }

    @Test
    public void isToOneTargetModifiedWithNewTarget() throws Exception {
        createOneArtistAndOnePainting();

        // add NEW gallery to painting
        List<Painting> paintings = ObjectSelect.query(Painting.class).select(context);
        assertEquals(1, paintings.size());
        Painting p1 = paintings.get(0);

        ClassDescriptor d = context.getEntityResolver().getClassDescriptor("Painting");
        ArcProperty gallery = (ArcProperty) d.getProperty("gallery");

        ObjectDiff diff = context.getObjectStore().registerDiff(p1.getObjectId(), null);
        assertFalse(DataRowUtils.isToOneTargetModified(gallery, p1, diff));

        Gallery g1 = (Gallery) context.newObject("Gallery");
        g1.addToPaintings(p1);
        assertTrue(DataRowUtils.isToOneTargetModified(gallery, p1, diff));
    }
}
