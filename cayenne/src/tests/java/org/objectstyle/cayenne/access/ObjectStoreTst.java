/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ObjectStoreTst extends CayenneTestCase {
    protected DataContext context;
    protected ObjectStore objectStore;

    protected void setUp() throws Exception {
        super.setUp();
        
        this.context = createDataContext();

        // create ObjectStore outside of this DataContext
        DataRowStore cache = new DataRowStore("test");
        this.objectStore = new ObjectStore(cache);

    }
    
    public void testCachedQueryResult() throws Exception {
        assertNull(objectStore.getCachedQueryResult("result"));
        
        List result = new ArrayList();
        objectStore.cacheQueryResult("result", result);
        assertSame(result, objectStore.getCachedQueryResult("result"));
        
        // test refreshing the cache
        List freshResult = new ArrayList();
        objectStore.cacheQueryResult("result", freshResult);
        assertSame(freshResult, objectStore.getCachedQueryResult("result"));
    }

    public void testObjectsInvalidated() throws Exception {
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = object.getObjectId();

        // insert object into the ObjectStore
        objectStore.addObject(object);
        objectStore.getDataRowCache().processSnapshotChanges(
            this,
            Collections.singletonMap(object.getObjectId(), row),
            Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        assertSame(object, objectStore.getObject(oid));
        assertNotNull(objectStore.getCachedSnapshot(oid));

        objectStore.objectsInvalidated(Collections.singletonList(object));

        assertSame(oid, object.getObjectId());
        assertNull(objectStore.getCachedSnapshot(oid));
        assertSame(object, objectStore.getObject(oid));
    }

    public void testObjectsUnregistered() throws Exception {
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(1));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        DataObject object = context.objectFromDataRow(Artist.class, row, false);
        ObjectId oid = object.getObjectId();

        // insert object into the ObjectStore
        objectStore.addObject(object);
        objectStore.getDataRowCache().processSnapshotChanges(
            this,
            Collections.singletonMap(object.getObjectId(), row),
            Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);
        assertSame(object, objectStore.getObject(oid));
        assertNotNull(objectStore.getCachedSnapshot(oid));

        objectStore.objectsUnregistered(Collections.singletonList(object));

        assertNull(object.getObjectId());
        assertNull(objectStore.getObject(oid));

        // in the future this may not be the case
        assertNull(objectStore.getCachedSnapshot(oid));
    }
}
