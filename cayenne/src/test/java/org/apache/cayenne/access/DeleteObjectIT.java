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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DeleteObjectIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected DataContext context;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    
    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");

        tPainting = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID");
    }

    protected void createHollowDataSet() throws Exception {
        tArtist.insert(1, "artist1");
        tPainting.insert(1, "painting1", 1);
    }

    protected void createObjectDataSet() throws Exception {
        tArtist.insert(1, "artist1");
    }

    protected void createObjectsDataSet() throws Exception {
        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");
    }

    protected void createObjectsRelationshipCollectionDataSet() throws Exception {
        tArtist.insert(1, "artist1");
        tPainting.insert(1, "painting1", 1);
        tPainting.insert(2, "painting2", 1);
        tPainting.insert(3, "painting3", 1);
    }

    @Test
    public void deleteObject() throws Exception {
        createObjectDataSet();

        Artist artist = Cayenne.objectForPK(context, Artist.class, 1);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        context.deleteObject(artist);
        assertEquals(PersistenceState.DELETED, artist.getPersistenceState());
        context.commitChanges();
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        assertNull(artist.getObjectContext());
    }

    @Test
    public void deleteObjects1() throws Exception {
        createObjectsDataSet();

        List<Artist> artists = ObjectSelect.query(Artist.class).select(context);
        assertEquals(2, artists.size());

        for (Artist object : artists) {
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        context.deleteObjects(artists);

        for (Artist object : artists) {
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }
    }

    // Similar to testDeleteObjects2, but extract ObjectContext instead of
    // DataContext.
    @Test
    public void deleteObjects2() throws Exception {
        createObjectsDataSet();

        List<Artist> artists = ObjectSelect.query(Artist.class).select(context);
        assertEquals(2, artists.size());

        for (Artist object : artists) {
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        artists.get(0).getObjectContext().deleteObjects(artists);

        for (Artist object : artists) {
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }

        artists.get(0).getObjectContext().commitChanges();

        for (Artist object : artists) {
            assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());
        }
    }

    @Test
    public void deleteObjectsRelationshipCollection() throws Exception {
        createObjectsRelationshipCollectionDataSet();

        Artist artist = Cayenne.objectForPK(context, Artist.class, 1);
        List<Painting> paintings = artist.getPaintingArray();

        assertEquals(3, paintings.size());

        // create a clone to be able to inspect paintings after deletion
        List<Painting> paintingsClone = new ArrayList<Painting>(paintings);

        for (Painting object : paintingsClone) {
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        context.deleteObjects(paintings);

        // as Painting -> Artist has Nullify rule, relationship list has to be
        // cleaned up,
        // and no exceptions thrown on concurrent modification...
        ObjRelationship r = context.getEntityResolver().getObjEntity(Painting.class).getRelationship("toArtist");
        assertEquals(DeleteRule.NULLIFY, r.getDeleteRule());
        assertEquals(0, paintings.size());

        for (Painting object : paintingsClone) {
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }
    }

    @Test
    public void deleteObjectInIterator() throws Exception {
        createObjectsRelationshipCollectionDataSet();

        Artist artist = Cayenne.objectForPK(context, Artist.class, 1);
        List<Painting> paintings = artist.getPaintingArray();

        assertEquals(3, paintings.size());

        // create a clone to be able to inspect paintings after deletion
        List<Painting> paintingsClone = new ArrayList<Painting>(paintings);

        for (Painting object : paintingsClone) {
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        Iterator<Painting> deleteIt = paintings.iterator();
        while (deleteIt.hasNext()) {
            Painting object = deleteIt.next();
            deleteIt.remove();
            context.deleteObjects(object);
        }

        // relationship list has to be cleaned up,
        // and no exceptions thrown on concurrent modification...
        assertEquals(0, paintings.size());

        for (Painting object : paintingsClone) {
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }
    }

    @Test
    public void deleteHollow() throws Exception {
        createHollowDataSet();

        List<Painting> paintings = ObjectSelect.query(Painting.class).select(context);

        Painting p = paintings.get(0);
        Artist a = p.getToArtist();

        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());
        context.deleteObjects(a);
        assertEquals(PersistenceState.DELETED, a.getPersistenceState());
    }

    @Test
    public void deleteNew() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("a");

        assertEquals(PersistenceState.NEW, artist.getPersistenceState());
        context.deleteObjects(artist);
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.rollbackChanges();
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
    }

}
