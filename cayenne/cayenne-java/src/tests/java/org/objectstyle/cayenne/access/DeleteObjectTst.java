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
import java.util.Iterator;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DeleteObjectTst extends CayenneTestCase {

    private DataContext context;

    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        context = createDataContext();
    }

    public void testDeleteObject() throws Exception {
        createTestData("testDeleteObject");

        Artist artist = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 1);
        assertEquals(PersistenceState.COMMITTED, artist.getPersistenceState());
        context.deleteObject(artist);
        assertEquals(PersistenceState.DELETED, artist.getPersistenceState());
    }

    public void testDeleteObjects() throws Exception {
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

    public void testDeleteObjectsRelationshipCollection() throws Exception {
        createTestData("testDeleteObjectsRelationshipCollection");

        Artist artist = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 1);
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

        Artist artist = (Artist) DataObjectUtils.objectForPK(context, Artist.class, 1);
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
        Artist artist = (Artist) context.createAndRegisterNewObject(Artist.class);
        artist.setArtistName("a");

        assertEquals(PersistenceState.NEW, artist.getPersistenceState());
        context.deleteObject(artist);
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
        context.rollbackChanges();
        assertEquals(PersistenceState.TRANSIENT, artist.getPersistenceState());
    }

}