/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.MockDataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ObjectStoreTst extends CayenneTestCase {

    public void testRegisteredObjectsCount() throws Exception {
        DataContext context = createDataContext();
        assertEquals(0, context.getObjectStore().registeredObjectsCount());

        DataObject o1 = new MockDataObject();
        o1.setObjectId(new ObjectId("T", "key1", "v1"));
        context.getObjectStore().recordObjectCreated(o1);
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // test object with same id
        DataObject o2 = new MockDataObject();
        o2.setObjectId(new ObjectId("T", "key1", "v1"));
        context.getObjectStore().recordObjectCreated(o2);
        assertEquals(1, context.getObjectStore().registeredObjectsCount());

        // test new object
        DataObject o3 = new MockDataObject();
        o3.setObjectId(new ObjectId("T", "key3", "v3"));
        context.getObjectStore().recordObjectCreated(o3);
        assertEquals(2, context.getObjectStore().registeredObjectsCount());
    }

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

    public void testObjectsInvalidated() throws Exception {
        DataContext context = createDataContext();

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = object.getObjectId();

        // insert object into the ObjectStore
        context.getObjectStore().recordObjectCreated(object);

        assertSame(object, context.getObjectStore().getNode(oid));
        assertNotNull(context.getObjectStore().getCachedSnapshot(oid));

        context.getObjectStore().objectsInvalidated(Collections.singletonList(object));

        assertSame(oid, object.getObjectId());
        assertNull(context.getObjectStore().getCachedSnapshot(oid));
        assertSame(object, context.getObjectStore().getNode(oid));
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
        context.getObjectStore().recordObjectCreated(object);
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
        Gallery g = (Gallery) context.createAndRegisterNewObject(Gallery.class);
        g.setGalleryName("Test Gallery");
        
        // Create an artist in the same context.
        Artist a = (Artist) context.createAndRegisterNewObject(Artist.class);
        a.setArtistName("Test Artist");
        
        // Create a painting in the same context.
        Painting p = (Painting) context.createAndRegisterNewObject(Painting.class);
        p.setPaintingTitle("Test Painting");
        
        // Set the painting's gallery.
        p.setToGallery(g);
        assertEquals(g, p.getToGallery());
        
        // Unregister the painting from the context.
        context.unregisterObjects(Collections.singletonList(p));
        
        // Make sure that even though the painting has been removed from the context's
        // object graph that the reference to the gallery is the same.
        assertEquals(g, p.getToGallery());
        
        // Now, set the relationship between "p" & "a."  Since "p" is not registered with a
        // context, but "a" is, "p" should be auto-registered with the context of "a."
        p.setToArtist(a);
        
        // Now commit the gallery, artist, & painting.
        context.commitChanges();
        
        // Check one last time that the painting's gallery is set to what we expect.
        assertEquals(g, p.getToGallery());
        
        // Now, retrieve the same painting from the DB.  Note that the gallery relationship
        // is null even though according to our painting, that should not be the case; a NULL
        // value has been recorded to the DB for the painting's gallery_id field.
        //
        // The full object graph is not being re-registered during auto-registration
        // with the context.
        Painting newP = (Painting) DataObjectUtils.objectForPK(createDataContext(), p.getObjectId());
        assertNotNull(newP.getToGallery());
    }
}