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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextExtrasIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected JdbcEventLogger logger;

    @Inject
    protected AdhocObjectFactory objectFactory;

    protected TableHelper tArtist;
    protected TableHelper tPainting;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "ARTIST_ID",
                "PAINTING_TITLE",
                "ESTIMATED_PRICE").setColumnTypes(
                Types.INTEGER,
                Types.BIGINT,
                Types.VARCHAR,
                Types.DECIMAL);
    }

    protected void createPhantomModificationDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
    }

    protected void createPhantomModificationsValidateToOneDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tPainting.insert(33001, 33001, "P1", 3000);
    }

    protected void createValidateOnToManyChangeDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
    }

    protected void createPhantomRelationshipModificationCommitDataSet() throws Exception {
        tArtist.insert(33001, "artist1");
        tArtist.insert(33002, "artist2");
        tPainting.insert(33001, 33001, "P1", 3000);
    }

    @Test
    public void testManualIdProcessingOnCommit() throws Exception {

        Artist object = context.newObject(Artist.class);
        object.setArtistName("ABC");
        assertEquals(PersistenceState.NEW, object.getPersistenceState());

        // do a manual ID substitution
        ObjectId manualId = ObjectId.of("Artist", Artist.ARTIST_ID_PK_COLUMN, 77777);
        object.setObjectId(manualId);

        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, object.getPersistenceState());
        assertSame(object, context.getGraphManager().getNode(manualId));
        assertEquals(manualId, object.getObjectId());
    }

    @Test
    public void testResolveFault() throws Exception {

        Artist o1 = context.newObject(Artist.class);
        o1.setArtistName("a");
        context.commitChanges();

        context.invalidateObjects(o1);
        assertEquals(PersistenceState.HOLLOW, o1.getPersistenceState());

        // NOTE: Map-based data objects clear their state, while field-based do not,
        // but all we really care is that HOLLOW object transparently updates it's state.
        // Here can be two variants depending on what type of data object is used:
        // assertNull(o1.readPropertyDirectly("artistName")); // map-based
        // assertEquals("a", o1.readPropertyDirectly("artistName")); // field-based

        // update table bypassing Cayenne
        int count = tArtist.update().set("ARTIST_NAME", "b").execute();
        assertTrue(count > 0);

        context.prepareForAccess(o1, null, false);
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
        assertEquals("b", o1.readPropertyDirectly("artistName"));
    }

    @Test
    public void testResolveFaultFailure() {

        Persistent o1 = context.findOrCreateObject(ObjectId.of(
                "Artist",
                Artist.ARTIST_ID_PK_COLUMN,
                234));

        try {
            context.prepareForAccess(o1, null, false);
            fail("Must blow on non-existing fault.");
        } catch (CayenneRuntimeException ignored) {
        }
    }

    @Test
    public void testUserProperties() {

        assertNull(context.getUserProperty("ABC"));
        Object object = new Object();

        context.setUserProperty("ABC", object);
        assertSame(object, context.getUserProperty("ABC"));
    }

    @Test
    public void testUserPropertiesRemove() {
        Object object = new Object();

        context.setUserProperty("ABC", object);
        assertSame(object, context.getUserProperty("ABC"));

        context.removeUserProperty("ABC");
        assertNull(context.getUserProperty("ABC"));

        context.setUserProperty("CBA", object);
        context.setUserProperty("BCA", object);
        assertSame(object, context.getUserProperty("CBA"));
        assertSame(object, context.getUserProperty("BCA"));

        context.clearUserProperties();
        assertNull(context.getUserProperty("CBA"));
        assertNull(context.getUserProperty("BCA"));
    }

    @Test
    public void testHasChangesNew() {

        assertTrue("No changes expected in context", !context.hasChanges());
        context.newObject("Artist");
        assertTrue(
                "Object added to context, expected to report changes",
                context.hasChanges());
    }

    @Test
    public void testNewObject() {

        Artist a1 = (Artist) context.newObject("Artist");
        assertTrue(context.getGraphManager().registeredNodes().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    @Test
    public void testNewObjectWithClass() {

        Artist a1 = context.newObject(Artist.class);
        assertTrue(context.getGraphManager().registeredNodes().contains(a1));
        assertTrue(context.newObjects().contains(a1));
    }

    @Test
    public void testIdObjectFromDataRow() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", 100000);
        Persistent obj = context.objectFromDataRow(Artist.class, row);
        assertNotNull(obj);
        assertTrue(context.getGraphManager().registeredNodes().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());

        assertNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
    }

    @Test
    public void testPartialObjectFromDataRow() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", 100001);
        row.put("ARTIST_NAME", "ArtistXYZ");
        Persistent obj = context.objectFromDataRow(Artist.class, row);
        assertNotNull(obj);
        assertTrue(context.getGraphManager().registeredNodes().contains(obj));
        assertEquals(PersistenceState.HOLLOW, obj.getPersistenceState());
        assertNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
    }

    @Test
    public void testFullObjectFromDataRow() {

        DataRow row = new DataRow(10);
        row.put("ARTIST_ID", 123456);
        row.put("ARTIST_NAME", "ArtistXYZ");
        row.put("DATE_OF_BIRTH", new Date());
        Artist obj = context.objectFromDataRow(Artist.class, row);

        assertTrue(context.getGraphManager().registeredNodes().contains(obj));
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
        assertNotNull(context.getObjectStore().getCachedSnapshot(obj.getObjectId()));
        assertEquals("ArtistXYZ", obj.getArtistName());
    }

    @Test
    public void testCommitChangesError() {

        DataDomain domain = context.getParentDataDomain();

        // setup mockup PK generator that will blow on PK request
        // to emulate an exception
        JdbcAdapter jdbcAdapter = objectFactory.newInstance(
                JdbcAdapter.class,
                JdbcAdapter.class.getName());
        PkGenerator newGenerator = new JdbcPkGenerator(jdbcAdapter) {

            @Override
            public Object generatePk(DataNode node, DbAttribute pk) throws Exception {
                throw new CayenneRuntimeException("Intentional");
            }
        };

        PkGenerator oldGenerator = domain
                .getDataNodes()
                .iterator()
                .next()
                .getAdapter()
                .getPkGenerator();
        JdbcAdapter adapter = (JdbcAdapter) domain
                .getDataNodes()
                .iterator()
                .next()
                .getAdapter();

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
    @Test
    public void testSelectException() {

        SQLTemplate q = new SQLTemplate(Artist.class, "SELECT * FROM NON_EXISTENT_TABLE");

        try {
            context.performGenericQuery(q);
            fail("Query was invalid and was supposed to fail.");
        }
        catch (RuntimeException ex) {
            // exception expected
        }

    }

    @Test
    public void testEntityResolver() {
        assertNotNull(context.getEntityResolver());
    }

    @Test
    public void testPhantomModificationsValidate() throws Exception {

        createPhantomModificationDataSet();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(context);
        Artist a1 = objects.get(0);
        Artist a2 = objects.get(1);

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

    @Test
    public void testPhantomModificationsValidateToOne() throws Exception {

        createPhantomModificationsValidateToOneDataSet();

        List<Painting> objects = ObjectSelect.query(Painting.class).select(context);
        Painting p1 = objects.get(0);

        p1.setPaintingTitle(p1.getPaintingTitle());
        p1.resetValidationFlags();
        context.commitChanges();

        assertFalse(
                "To-one relationship presence caused incorrect validation call.",
                p1.isValidateForSaveCalled());
    }

    @Test
    public void testValidateOnToManyChange() throws Exception {

        createValidateOnToManyChangeDataSet();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(context);
        Artist a1 = objects.get(0);

        Painting p1 = context.newObject(Painting.class);
        p1.setPaintingTitle("XXX");
        a1.addToPaintingArray(p1);
        a1.resetValidationFlags();
        context.commitChanges();

        assertFalse(a1.isValidateForSaveCalled());
    }

    @Test
    public void testPhantomAttributeModificationCommit() throws Exception {

        createPhantomModificationDataSet();

        List<Artist> objects = ObjectSelect.query(Artist.class).select(context);
        Artist a1 = objects.get(0);

        String oldName = a1.getArtistName();

        a1.setArtistName(oldName + ".mod");
        a1.setArtistName(oldName);

        context.commitChanges();
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    @Test
    public void testPhantomRelationshipModificationCommit() throws Exception {

        createPhantomRelationshipModificationCommitDataSet();

        List<Painting> objects = ObjectSelect.query(Painting.class).select(context);
        assertEquals(1, objects.size());

        Painting p1 = objects.get(0);

        Artist oldArtist = p1.getToArtist();
        Artist newArtist = Cayenne.objectForPK(context, Artist.class, 33002);

        assertNotSame(oldArtist, newArtist);

        p1.setToArtist(newArtist);
        p1.setToArtist(oldArtist);

        context.commitChanges();

        assertEquals(PersistenceState.COMMITTED, p1.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, oldArtist.getPersistenceState());
        assertEquals(PersistenceState.COMMITTED, newArtist.getPersistenceState());
    }

    @Test
    public void testPhantomRelationshipModificationValidate() throws Exception {

        createPhantomRelationshipModificationCommitDataSet();

        List<Painting> objects = ObjectSelect.query(Painting.class).select(context);
        assertEquals(1, objects.size());

        Painting p1 = objects.get(0);

        Artist oldArtist = p1.getToArtist();
        Artist newArtist = Cayenne.objectForPK(context, Artist.class, 33002);

        assertNotSame(oldArtist, newArtist);

        p1.setToArtist(newArtist);
        p1.setToArtist(oldArtist);

        p1.resetValidationFlags();
        context.commitChanges();
        assertFalse(p1.isValidateForSaveCalled());
    }
}
