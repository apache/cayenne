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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.MockDataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ObjectStoreTest extends CayenneCase {

    public void testRegisteredObjectsCount() throws Exception {
        DataContext context = createDataContext();
        assertEquals(0, context.getObjectStore().registeredObjectsCount());

        DataObject o1 = new MockDataObject();
        o1.setObjectId(new ObjectId("T", "key1", "v1"));
        context.getObjectStore().registerNode(o1.getObjectId(), o1);
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // test object with same id
        DataObject o2 = new MockDataObject();
        o2.setObjectId(new ObjectId("T", "key1", "v1"));
        context.getObjectStore().registerNode(o2.getObjectId(), o2);
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // test new object
        DataObject o3 = new MockDataObject();
        o3.setObjectId(new ObjectId("T", "key3", "v3"));
        context.getObjectStore().registerNode(o3.getObjectId(), o3);
        assertEquals(2, context.getObjectStore().registeredObjectsCount());
    }

    /**
     * @deprecated since 3.0
     */
    public void testCachedQueriesCount() throws Exception {
        DataContext context = createDataContext();
        assertEquals(0, context.getObjectStore().cachedQueriesCount());

        context.getObjectStore().cacheQueryResult("result", new ArrayList());
        assertEquals(1, context.getObjectStore().cachedQueriesCount());

        // test refreshing the cache
        context.getObjectStore().cacheQueryResult("result", new ArrayList());
        assertEquals(1, context.getObjectStore().cachedQueriesCount());

        // test new entry
        context.getObjectStore().cacheQueryResult("result2", new ArrayList());
        assertEquals(2, context.getObjectStore().cachedQueriesCount());
    }

    /**
     * @deprecated since 3.0
     */
    public void testCachedQueryResult() throws Exception {
        DataContext context = createDataContext();
        assertNull(context.getObjectStore().getCachedQueryResult("result"));

        List result = new ArrayList();
        context.getObjectStore().cacheQueryResult("result", result);
        assertSame(result, context.getObjectStore().getCachedQueryResult("result"));

        // test refreshing the cache
        List freshResult = new ArrayList();
        context.getObjectStore().cacheQueryResult("result", freshResult);
        assertSame(freshResult, context.getObjectStore().getCachedQueryResult("result"));
    }

    public void testObjectsUnregistered() throws Exception {
        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = object.getObjectId();

        // insert object into the ObjectStore
        context.getObjectStore().registerNode(oid, object);
        assertSame(object, context.getObjectStore().getNode(oid));
        assertNotNull(context.getObjectStore().getCachedSnapshot(oid));

        context.getObjectStore().objectsUnregistered(Collections.singletonList(object));

        assertNull(object.getObjectId());
        assertNull(context.getObjectStore().getNode(oid));

        // in the future this may not be the case
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
    }

    public void testUnregisterThenRegister() throws Exception {
        DataContext context = createDataContext();

        // Create a gallery.
        Gallery g = context.newObject(Gallery.class);
        g.setGalleryName("Test Gallery");

        // Create an artist in the same context.
        Artist a = context.newObject(Artist.class);
        a.setArtistName("Test Artist");

        // Create a painting in the same context.
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("Test Painting");

        // Set the painting's gallery.
        p.setToGallery(g);
        assertEquals(g, p.getToGallery());

        // Unregister the painting from the context.
        context.unregisterObjects(Collections.singletonList(p));

        // Make sure that even though the painting has been removed from the context's
        // object graph that the reference to the gallery is the same.
        assertEquals(g, p.getToGallery());

        // Now, set the relationship between "p" & "a." Since "p" is not registered with a
        // context, but "a" is, "p" should be auto-registered with the context of "a."
        p.setToArtist(a);

        // Now commit the gallery, artist, & painting.
        context.commitChanges();

        // Check one last time that the painting's gallery is set to what we expect.
        assertEquals(g, p.getToGallery());

        // Now, retrieve the same painting from the DB. Note that the gallery relationship
        // is null even though according to our painting, that should not be the case; a
        // NULL
        // value has been recorded to the DB for the painting's gallery_id field.
        //
        // The full object graph is not being re-registered during auto-registration
        // with the context.
        Painting newP = (Painting) DataObjectUtils.objectForPK(createDataContext(), p
                .getObjectId());
        assertNotNull(newP.getToGallery());
    }
}
