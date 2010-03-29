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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DeleteObjectTest extends CayenneCase {

    private DataContext context;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testDeleteObject() throws Exception {
        createTestData("testDeleteObject");

        Artist artist = DataObjectUtils.objectForPK(context, Artist.class, 1);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        context.deleteObject(artist);
        assertEquals(PersistenceState.DELETED, artist.getPersistenceState());
        context.commitChanges();
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        assertNull(artist.getObjectContext());
    }

    public void testDeleteObjects1() throws Exception {
        createTestData("testDeleteObjects");

        List artists = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(2, artists.size());

        Iterator it = artists.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        context.deleteObjects(artists);
        it = artists.iterator();

        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }
    }

    // Similar to testDeleteObjects2, but extract ObjectContext instead of DataContext.
    public void testDeleteObjects2() throws Exception {
        createTestData("testDeleteObjects");

        List<Artist> artists = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(2, artists.size());

        for (Artist object : artists)
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());

        artists.get(0).getObjectContext().deleteObjects(artists);

        for (Artist object : artists)
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());

        artists.get(0).getObjectContext().commitChanges();

        for (Artist object : artists)
            assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());
    }

    public void testDeleteObjectsRelationshipCollection() throws Exception {
        createTestData("testDeleteObjectsRelationshipCollection");

        Artist artist = DataObjectUtils.objectForPK(context, Artist.class, 1);
        List paintings = artist.getPaintingArray();

        assertEquals(3, paintings.size());

        // create a clone to be able to inspect paintings after deletion
        List paintingsClone = new ArrayList(paintings);

        Iterator it = paintingsClone.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        context.deleteObjects(paintings);

        // as Painting -> Artist has Nullify rule, relationship list has to be cleaned up,
        // and no exceptions thrown on concurrent modification...
        ObjRelationship r = (ObjRelationship) context
                .getEntityResolver()
                .lookupObjEntity(Painting.class)
                .getRelationship("toArtist");
        assertEquals(DeleteRule.NULLIFY, r.getDeleteRule());
        assertEquals(0, paintings.size());

        it = paintingsClone.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }
    }

    public void testDeleteObjectInIterator() throws Exception {
        createTestData("testDeleteObjectsRelationshipCollection");

        Artist artist = DataObjectUtils.objectForPK(context, Artist.class, 1);
        List paintings = artist.getPaintingArray();

        assertEquals(3, paintings.size());

        // create a clone to be able to inspect paintings after deletion
        List paintingsClone = new ArrayList(paintings);

        Iterator it = paintingsClone.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        }

        Iterator deleteIt = paintings.iterator();
        while (deleteIt.hasNext()) {
            DataObject object = (DataObject) deleteIt.next();
            deleteIt.remove();
            context.deleteObject(object);
        }

        // relationship list has to be cleaned up,
        // and no exceptions thrown on concurrent modification...
        assertEquals(0, paintings.size());

        it = paintingsClone.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();
            assertEquals(PersistenceState.DELETED, object.getPersistenceState());
        }
    }

    public void testDeleteHollow() throws Exception {
        createTestData("testDeleteHollow");

        List paintings = context.performQuery(new SelectQuery(Painting.class));

        Painting p = (Painting) paintings.get(0);
        Artist a = p.getToArtist();

        assertEquals(PersistenceState.HOLLOW, a.getPersistenceState());
        context.deleteObject(a);
        assertEquals(PersistenceState.DELETED, a.getPersistenceState());
    }

    public void testDeleteNew() throws Exception {
        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("a");

        assertEquals(PersistenceState.NEW, artist.getPersistenceState());
        context.deleteObject(artist);
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.rollbackChanges();
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
    }

}
