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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

/**
 * "Lightweight" test cases for DataContext. These tests do not require any additional
 * database setup.
 *
 */
public class DataContextExtrasTest extends CayenneCase {

    public void testManualIdProcessingOnCommit() throws Exception {
        deleteTestData();
        DataContext context = createDataContext();

        Artist object = context.newObject(Artist.class);
        object.setArtistName("ABC");
        assertEquals(PersistenceState.NEW, object.getPersistenceState());

        // do a manual ID substitution
        ObjectId manualId = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 77777);
        object.setObjectId(manualId);

        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        assertSame(object, context.getGraphManager().getNode(manualId));
        assertEquals(manualId, object.getObjectId());
    }

    public void testResolveFault() {
        DataContext context = createDataContext();

        Artist o1 = context.newObject(Artist.class);
        o1.setArtistName("a");
        context.commitChanges();

        context.invalidateObjects(Collections.singleton(o1));
        assertEquals(PersistenceState.HOLLOW, o1.getPersistenceState());
        assertNull(o1.readPropertyDirectly("artistName"));

        context.prepareForAccess(o1, null, false);
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
        assertEquals("a", o1.readPropertyDirectly("artistName"));
    }

    public void testResolveFaultFailure() {
        DataContext context = createDataContext();

        Persistent o1 = context.localObject(new ObjectId(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                new Integer(234)), null);

        try {
            context.prepareForAccess(o1, null, false);
            fail("Must blow on non-existing fault.");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testUserProperties() {
        DataContext context = createDataContext();

        assertNull(context.getUserProperty("ABC"));
        Object object = new Object();

        context.setUserProperty("ABC", object);
        assertSame(object, context.getUserProperty("ABC"));
    }

    public void testHasChangesNew() {
        DataContext context = createDataContext();
        assertTrue("No changes expected in context", !context.hasChanges());
        context.newObject("Artist");
        assertTrue("Object added to context, expected to report changes", context
                .hasChanges());
    }

    public void testNewObject() {
        DataContext context = createDataContext();
        Artist a1 = (Artist) context.newObject("Artist");
        assertTrue(context.getGraphManager().registeredNodes().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    public void testNewObjectWithClass() {
        DataContext context = createDataContext();
        Artist a1 = context.newObject(Artist.class);
        assertTrue(context.getGraphManager().registeredNodes().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    public void testIdObjectFromDataRow() {
        DataContext context = createDataContext();
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(100000));
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        assertNotNull(obj);
        assertTrue(context.getGraphManager().registeredNodes().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());

        assertNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
    }

    public void testPartialObjectFromDataRow() {
        DataContext context = createDataContext();
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(100001));
        row.put("ARTIST_NAME", "ArtistXYZ");
        DataObject obj = context.objectFromDataRow(Artist.class, row, false);
        assertNotNull(obj);
        assertTrue(context.getGraphManager().registeredNodes().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
    }

    public void testFullObjectFromDataRow() {
        DataContext context = createDataContext();
        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", new Integer(123456));
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        Artist obj = context.objectFromDataRow(Artist.class, row, false);

        assertTrue(context.getGraphManager().registeredNodes().contains(obj));
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertNotNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
        assertEquals("ArtistXYZ", obj.getArtistName());
    }

    public void testCommitChangesError() {
        DataContext context = createDataContext();

        // setup mockup PK generator that will blow on PK request
        // to emulate an exception
        PkGenerator newGenerator = new JdbcPkGenerator() {

            @Override
            public Object generatePkForDbEntity(DataNode node, DbEntity ent)
                    throws Exception {
                throw new CayenneRuntimeException("Synthetic error....");
            }

            @Override
            public Object generatePk(DataNode node, DbAttribute pk)
                    throws Exception {
                throw new CayenneRuntimeException("Synthetic error....");
            }
        };

        PkGenerator oldGenerator = getNode().getAdapter().getPkGenerator();
        JdbcAdapter adapter = (JdbcAdapter) getNode().getAdapter();

        adapter.setPkGenerator(newGenerator);
        try {
            Artist newArtist = context.newObject(Artist.class);
            newArtist.setArtistName("aaa");
            context.commitChanges();
            fail("Exception expected but not thrown due to missing PK generation routine.");
        }
        catch (CayenneRuntimeException ex) {
            // exception expected
        }
        finally {
            adapter.setPkGenerator(oldGenerator);
        }
    }

    /**
     * Testing behavior of Cayenne when a database exception is thrown in SELECT query.
     */
    public void testSelectException() {
        DataContext context = createDataContext();

        SQLTemplate q = new SQLTemplate(Artist.class, "SELECT * FROM NON_EXISTENT_TABLE");

        try {
            context.performGenericQuery(q);
            fail("Query was invalid and was supposed to fail.");
        }
        catch (RuntimeException ex) {
            // exception expected
        }

    }

    public void testEntityResolver() {
        DataContext context = createDataContext();
        assertNotNull(context.getEntityResolver());
    }

    public void testPhantomModificationsValidate() throws Exception {
        deleteTestData();
        createTestData("testPhantomModification");
        DataContext context = createDataContext();

        List objects = context.performQuery(new SelectQuery(Artist.class));
        Artist a1 = (Artist) objects.get(0);
        Artist a2 = (Artist) objects.get(1);

        a1.setArtistName(a1.getArtistName());
        a1.resetValidationFlags();
        a2.resetValidationFlags();
        context.commitChanges();

        assertFalse(a1.isValidateForSaveCalled());
        assertFalse(a2.isValidateForSaveCalled());

        // "phantom" modification - the property is really unchanged
        a1.setArtistName(a1.getArtistName());

        // some other unrelated object modification caused phantom modification to be
        // committed as well...
        // (see CAY-355)
        a2.setArtistName(a2.getArtistName() + "_x");

        a1.resetValidationFlags();
        a2.resetValidationFlags();
        context.commitChanges();

        assertTrue(a2.isValidateForSaveCalled());
        assertFalse(a1.isValidateForSaveCalled());
    }

    public void testPhantomModificationsValidateToOne() throws Exception {
        deleteTestData();
        createTestData("testPhantomModificationsValidateToOne");
        DataContext context = createDataContext();

        List objects = context.performQuery(new SelectQuery(Painting.class));
        Painting p1 = (Painting) objects.get(0);

        p1.setPaintingTitle(p1.getPaintingTitle());
        p1.resetValidationFlags();
        context.commitChanges();

        assertFalse("To-one relationship presence caused incorrect validation call.", p1
                .isValidateForSaveCalled());
    }

    public void testValidateOnToManyChange() throws Exception {
        deleteTestData();
        createTestData("testValidateOnToManyChange");
        DataContext context = createDataContext();

        List objects = context.performQuery(new SelectQuery(Artist.class));
        Artist a1 = (Artist) objects.get(0);

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("XXX");
        a1.addToPaintingArray(p1);
        a1.resetValidationFlags();
        context.commitChanges();

        assertFalse(a1.isValidateForSaveCalled());
    }

    public void testPhantomAttributeModificationCommit() throws Exception {
        deleteTestData();
        createTestData("testPhantomModification");
        DataContext context = createDataContext();

        List objects = context.performQuery(new SelectQuery(Artist.class));
        Artist a1 = (Artist) objects.get(0);

        String oldName = a1.getArtistName();

        a1.setArtistName(oldName + ".mod");
        a1.setArtistName(oldName);

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testPhantomRelationshipModificationCommit() throws Exception {
        deleteTestData();
        createTestData("testPhantomRelationshipModificationCommit");
        DataContext context = createDataContext();

        SelectQuery query = new SelectQuery(Painting.class);
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p1 = (Painting) objects.get(0);

        Artist oldArtist = p1.getToArtist();
        Artist newArtist = DataObjectUtils.objectForPK(
                context,
                Artist.class,
                33002);

        assertNotSame(oldArtist, newArtist);

        p1.setToArtist(newArtist);
        p1.setToArtist(oldArtist);

        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, oldArtist.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, newArtist.getPersistenceState());
    }

    public void testPhantomRelationshipModificationValidate() throws Exception {
        deleteTestData();
        createTestData("testPhantomRelationshipModificationCommit");
        DataContext context = createDataContext();

        SelectQuery query = new SelectQuery(Painting.class);
        List objects = context.performQuery(query);
        assertEquals(1, objects.size());

        Painting p1 = (Painting) objects.get(0);

        Artist oldArtist = p1.getToArtist();
        Artist newArtist = DataObjectUtils.objectForPK(
                context,
                Artist.class,
                33002);

        assertNotSame(oldArtist, newArtist);

        p1.setToArtist(newArtist);
        p1.setToArtist(oldArtist);

        p1.resetValidationFlags();
        context.commitChanges();
        assertFalse(p1.isValidateForSaveCalled());
    }
}
