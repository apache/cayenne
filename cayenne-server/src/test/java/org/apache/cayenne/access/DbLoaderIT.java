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

import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.loader.filters.DbPath;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DbLoaderIT extends ServerCase {

    public static final DbLoaderConfiguration CONFIG = new DbLoaderConfiguration();
    @Inject
    private ServerRuntime runtime;

    @Inject
    private DbAdapter adapter;

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    private DbLoader loader;

    @Before
    public void setUp() throws Exception {
        loader = new DbLoader(dataSourceFactory.getSharedDataSource().getConnection(), adapter, null);
    }

    @After
    public void tearDown() throws Exception {
        loader.getConnection().close();
    }

    @Test
    public void testGetTableTypes() throws Exception {

        List<?> tableTypes = loader.getTableTypes();

        assertNotNull(tableTypes);

        String tableLabel = adapter.tableTypeForTable();
        if (tableLabel != null) {
            assertTrue("Missing type for table '" + tableLabel + "' - " + tableTypes, tableTypes.contains(tableLabel));
        }

        String viewLabel = adapter.tableTypeForView();
        if (viewLabel != null) {
            assertTrue("Missing type for view '" + viewLabel + "' - " + tableTypes, tableTypes.contains(viewLabel));
        }
    }

    @Test
    public void testGetTables() throws Exception {

        String tableLabel = adapter.tableTypeForTable();

        Collection<DbEntity> tables = loader.getTables(new DbLoaderConfiguration(), new String[] { tableLabel })
                .values().iterator().next().values();

        assertNotNull(tables);

        boolean foundArtist = false;

        for (DbEntity table : tables) {
            if ("ARTIST".equalsIgnoreCase(table.getName())) {
                foundArtist = true;
                break;
            }
        }

        assertTrue("'ARTIST' is missing from the table list: " + tables, foundArtist);
    }

    @Test
    public void testLoadWithMeaningfulPK() throws Exception {

        DataMap map = new DataMap();
        String[] tableLabel = { adapter.tableTypeForTable() };

        loader.setCreatingMeaningfulPK(true);

        Map<DbPath, Map<String, DbEntity>> testLoader = loader.getTables(CONFIG, tableLabel);
        if (testLoader.isEmpty()) {
            testLoader = loader.getTables(CONFIG, tableLabel);
        }

        List<DbEntity> entities = loader.loadDbEntities(map, CONFIG, testLoader);
        loader.loadObjEntities(map, CONFIG, entities);

        ObjEntity artist = map.getObjEntity("Artist");
        assertNotNull(artist);

        ObjAttribute id = artist.getAttribute("artistId");
        assertNotNull(id);
    }

    /**
     * DataMap loading is in one big test method, since breaking it in
     * individual tests would require multiple reads of metatdata which is
     * extremely slow on some RDBMS (Sybase).
     */
    @Test
    public void testLoad() throws Exception {

        boolean supportsUnique = runtime.getDataDomain().getDataNodes().iterator().next().getAdapter()
                .supportsUniqueConstraints();
        boolean supportsLobs = accessStackAdapter.supportsLobs();
        boolean supportsFK = accessStackAdapter.supportsFKConstraints();

        DataMap map = new DataMap();
        map.setDefaultPackage("foo.x");

        String tableLabel = adapter.tableTypeForTable();

        // *** TESTING THIS ***
        List<DbEntity> entities = loader.loadDbEntities(map, CONFIG, loader.getTables(CONFIG, new String[]{tableLabel}));

        assertDbEntities(map);

        if (supportsLobs) {
            assertLobDbEntities(map);
        }

        // *** TESTING THIS ***
        HashMap<DbPath, Map<String, DbEntity>> tables = new HashMap<DbPath, Map<String, DbEntity>>();
        HashMap<String, DbEntity> value = new HashMap<String, DbEntity>();
        for (DbEntity e : entities) {
            value.put(e.getName(), e);
        }
        tables.put(new DbPath(), value);
        loader.loadDbRelationships(CONFIG, tables);

        if (supportsFK) {
            Collection<DbRelationship> rels = getDbEntity(map, "ARTIST").getRelationships();
            assertNotNull(rels);
            assertTrue(!rels.isEmpty());

            // test one-to-one
            rels = getDbEntity(map, "PAINTING").getRelationships();
            assertNotNull(rels);

            // find relationship to PAINTING_INFO
            DbRelationship oneToOne = null;
            for (DbRelationship rel : rels) {
                if ("PAINTING_INFO".equalsIgnoreCase(rel.getTargetEntityName())) {
                    oneToOne = rel;
                    break;
                }
            }

            assertNotNull("No relationship to PAINTING_INFO", oneToOne);
            assertFalse("Relationship to PAINTING_INFO must be to-one", oneToOne.isToMany());
            assertTrue("Relationship to PAINTING_INFO must be to-one", oneToOne.isToDependentPK());

            // test UNIQUE only if FK is supported...
            if (supportsUnique) {
                assertUniqueConstraintsInRelationships(map);
            }
        }

        // *** TESTING THIS ***
        loader.setCreatingMeaningfulPK(false);
        loader.loadObjEntities(map, CONFIG, entities);

        assertObjEntities(map);

        // now when the map is loaded, test
        // various things
        // selectively check how different types were processed
        if (accessStackAdapter.supportsColumnTypeReengineering()) {
            checkTypes(map);
        }
    }

    private void assertUniqueConstraintsInRelationships(DataMap map) {
        // unfortunately JDBC metadata doesn't provide info for UNIQUE
        // constraints....
        // cant reengineer them...

        // find rel to TO_ONEFK1
        /*
         * Iterator it = getDbEntity(map,
         * "TO_ONEFK2").getRelationships().iterator(); DbRelationship rel =
         * (DbRelationship) it.next(); assertEquals("TO_ONEFK1",
         * rel.getTargetEntityName());
         * assertFalse("UNIQUE constraint was ignored...", rel.isToMany());
         */
    }

    private void assertDbEntities(DataMap map) {
        DbEntity dae = getDbEntity(map, "ARTIST");
        assertNotNull("Null 'ARTIST' entity, other DbEntities: " + map.getDbEntityMap(), dae);
        assertEquals("ARTIST", dae.getName().toUpperCase());

        if (accessStackAdapter.supportsCatalogs()) {
            assertNotNull(dae.getCatalog());
            assertEquals("CAYENNE", dae.getCatalog().toUpperCase());
        }

        DbAttribute a = getDbAttribute(dae, "ARTIST_ID");
        assertNotNull(a);
        assertTrue(a.isPrimaryKey());
        assertFalse(a.isGenerated());

        if (adapter.supportsGeneratedKeys()) {
            DbEntity bag = getDbEntity(map, "BAG");
            DbAttribute id = bag.getAttribute("ID");
            assertTrue(id.isPrimaryKey());
            assertTrue(id.isGenerated());
        }
    }

    private void assertObjEntities(DataMap map) {

        boolean supportsLobs = accessStackAdapter.supportsLobs();
        boolean supportsFK = accessStackAdapter.supportsFKConstraints();

        ObjEntity ae = map.getObjEntity("Artist");
        assertNotNull(ae);
        assertEquals("Artist", ae.getName());

        // assert primary key is not an attribute
        assertNull(ae.getAttribute("artistId"));

        if (supportsLobs) {
            assertLobObjEntities(map);
        }

        if (supportsFK) {
            Collection<?> rels1 = ae.getRelationships();
            assertNotNull(rels1);
            assertTrue(rels1.size() > 0);
        }

        assertEquals("foo.x.Artist", ae.getClassName());
    }

    private void assertLobDbEntities(DataMap map) {
        DbEntity blobEnt = getDbEntity(map, "BLOB_TEST");
        assertNotNull(blobEnt);
        DbAttribute blobAttr = getDbAttribute(blobEnt, "BLOB_COL");
        assertNotNull(blobAttr);
        assertTrue(msgForTypeMismatch(Types.BLOB, blobAttr), Types.BLOB == blobAttr.getType()
                || Types.LONGVARBINARY == blobAttr.getType());
        DbEntity clobEnt = getDbEntity(map, "CLOB_TEST");
        assertNotNull(clobEnt);
        DbAttribute clobAttr = getDbAttribute(clobEnt, "CLOB_COL");
        assertNotNull(clobAttr);
        assertTrue(msgForTypeMismatch(Types.CLOB, clobAttr), Types.CLOB == clobAttr.getType()
                || Types.LONGVARCHAR == clobAttr.getType());
    }

    private void assertLobObjEntities(DataMap map) {
        ObjEntity blobEnt = map.getObjEntity("BlobTest");
        assertNotNull(blobEnt);
        // BLOBs should be mapped as byte[]
        ObjAttribute blobAttr = blobEnt.getAttribute("blobCol");
        assertNotNull("BlobTest.blobCol failed to doLoad", blobAttr);
        assertEquals("byte[]", blobAttr.getType());
        ObjEntity clobEnt = map.getObjEntity("ClobTest");
        assertNotNull(clobEnt);
        // CLOBs should be mapped as Strings by default
        ObjAttribute clobAttr = clobEnt.getAttribute("clobCol");
        assertNotNull(clobAttr);
        assertEquals(String.class.getName(), clobAttr.getType());
    }

    private DbEntity getDbEntity(DataMap map, String name) {
        DbEntity de = map.getDbEntity(name);
        // sometimes table names get converted to lowercase
        if (de == null) {
            de = map.getDbEntity(name.toLowerCase());
        }

        return de;
    }

    private DbAttribute getDbAttribute(DbEntity ent, String name) {
        DbAttribute da = ent.getAttribute(name);
        // sometimes table names get converted to lowercase
        if (da == null) {
            da = ent.getAttribute(name.toLowerCase());
        }

        return da;
    }

    private DataMap originalMap() {
        return runtime.getDataDomain().getDataNodes().iterator().next().getDataMaps().iterator().next();
    }

    /**
     * Selectively check how different types were processed.
     */
    public void checkTypes(DataMap map) {
        DbEntity dbe = getDbEntity(map, "PAINTING");
        DbEntity floatTest = getDbEntity(map, "FLOAT_TEST");
        DbEntity smallintTest = getDbEntity(map, "SMALLINT_TEST");
        DbAttribute integerAttr = getDbAttribute(dbe, "PAINTING_ID");
        DbAttribute decimalAttr = getDbAttribute(dbe, "ESTIMATED_PRICE");
        DbAttribute varcharAttr = getDbAttribute(dbe, "PAINTING_TITLE");
        DbAttribute floatAttr = getDbAttribute(floatTest, "FLOAT_COL");
        DbAttribute smallintAttr = getDbAttribute(smallintTest, "SMALLINT_COL");

        // check decimal
        assertTrue(msgForTypeMismatch(Types.DECIMAL, decimalAttr), Types.DECIMAL == decimalAttr.getType()
                || Types.NUMERIC == decimalAttr.getType());
        assertEquals(2, decimalAttr.getScale());

        // check varchar
        assertEquals(msgForTypeMismatch(Types.VARCHAR, varcharAttr), Types.VARCHAR, varcharAttr.getType());
        assertEquals(255, varcharAttr.getMaxLength());
        // check integer
        assertEquals(msgForTypeMismatch(Types.INTEGER, integerAttr), Types.INTEGER, integerAttr.getType());
        // check float
        assertTrue(msgForTypeMismatch(Types.FLOAT, floatAttr), Types.FLOAT == floatAttr.getType()
                || Types.DOUBLE == floatAttr.getType() || Types.REAL == floatAttr.getType());

        // check smallint
        assertTrue(msgForTypeMismatch(Types.SMALLINT, smallintAttr), Types.SMALLINT == smallintAttr.getType()
                || Types.INTEGER == smallintAttr.getType());
    }

    public void checkAllDBEntities(DataMap map) {

        for (DbEntity origEnt : originalMap().getDbEntities()) {
            DbEntity newEnt = map.getDbEntity(origEnt.getName());
            for (DbAttribute origAttr : origEnt.getAttributes()) {
                DbAttribute newAttr = newEnt.getAttribute(origAttr.getName());
                assertNotNull("No matching DbAttribute for '" + origAttr.getName(), newAttr);
                assertEquals(msgForTypeMismatch(origAttr, newAttr), origAttr.getType(), newAttr.getType());
                // length and precision doesn't have to be the same
                // it must be greater or equal
                assertTrue(origAttr.getMaxLength() <= newAttr.getMaxLength());
                assertTrue(origAttr.getScale() <= newAttr.getScale());
            }
        }
    }

    private static String msgForTypeMismatch(DbAttribute origAttr, DbAttribute newAttr) {
        return msgForTypeMismatch(origAttr.getType(), newAttr);
    }

    private static String msgForTypeMismatch(int origType, DbAttribute newAttr) {
        String nt = TypesMapping.getSqlNameByType(newAttr.getType());
        String ot = TypesMapping.getSqlNameByType(origType);
        return attrMismatch(newAttr.getName(), "expected type: <" + ot + ">, but was <" + nt + ">");
    }

    private static String attrMismatch(String attrName, String msg) {
        return "[Error loading attribute '" + attrName + "': " + msg + "]";
    }

/*
    TODO

    @Test
    public void testCreateLoader() throws Exception {

        DbLoader loader = parameters.createLoader(mock(DbAdapter.class), connection,
                mock(DbLoaderDelegate.class));
        assertNotNull(loader);
        assertSame(connection, loader.getConnection());

        assertTrue(loader.includeTableName("dummy"));
    }

    @Test
    public void testCreateLoader_IncludeExclude() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setIncludeTables("a,b,c*");

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        assertFalse(loader1.includeTableName("dummy"));
        assertFalse(loader1.includeTableName("ab"));
        assertTrue(loader1.includeTableName("a"));
        assertTrue(loader1.includeTableName("b"));
        assertTrue(loader1.includeTableName("cd"));

        parameters.setExcludeTables("cd");

        DbLoader loader2 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        assertFalse(loader2.includeTableName("dummy"));
        assertFalse(loader2.includeTableName("ab"));
        assertTrue(loader2.includeTableName("a"));
        assertTrue(loader2.includeTableName("b"));
        assertFalse(loader2.includeTableName("cd"));
        assertTrue(loader2.includeTableName("cx"));
    }


    @Test
    public void testCreateLoader_MeaningfulPk_Default() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        assertNull(parameters.getMeaningfulPkTables());

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute pk = new DbAttribute("pk", Types.INTEGER, e1);
        pk.setPrimaryKey(true);
        e1.addAttribute(pk);
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);
        entities.add(e1);

        loader1.loadObjEntities(map, entities);

        ObjEntity oe1 = map.getObjEntity("E1");
        assertEquals(1, oe1.getAttributes().size());
        assertNotNull(oe1.getAttribute("nonPk"));
    }

    @Test
    public void testCreateLoader_MeaningfulPk_Specified() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setMeaningfulPkTables("a*");

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        Collection<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute pk = new DbAttribute("pk", Types.INTEGER, e1);
        pk.setPrimaryKey(true);
        e1.addAttribute(pk);
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);
        entities.add(e1);

        DbEntity a1 = new DbEntity("a1");
        DbAttribute apk = new DbAttribute("pk", Types.INTEGER, a1);
        apk.setPrimaryKey(true);
        a1.addAttribute(apk);
        DbAttribute anonPk = new DbAttribute("nonPk", Types.INTEGER, a1);
        a1.addAttribute(anonPk);

        map.addDbEntity(a1);
        entities.add(a1);

        loader1.loadObjEntities(map, entities);

        ObjEntity oe1 = map.getObjEntity("E1");
        assertEquals(1, oe1.getAttributes().size());
        assertNotNull(oe1.getAttribute("nonPk"));

        ObjEntity oe2 = map.getObjEntity("A1");
        assertEquals(2, oe2.getAttributes().size());
        assertNotNull(oe2.getAttribute("nonPk"));
        assertNotNull(oe2.getAttribute("pk"));
    }

    @Test
    public void testCreateLoader_UsePrimitives_False() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setUsePrimitives(false);

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);
        entities.add(e1);

        loader1.loadObjEntities(map, entities);

        ObjEntity oe1 = map.getObjEntity("E1");

        ObjAttribute oa1 = oe1.getAttribute("nonPk");
        assertEquals("java.lang.Integer", oa1.getType());
    }

    @Test
    public void testCreateLoader_UsePrimitives_True() throws Exception {
        DbImportConfiguration parameters = new DbImportConfiguration();
        parameters.setUsePrimitives(true);

        DbLoader loader1 = parameters.createLoader(mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        DataMap map = new DataMap();

        DbEntity e1 = new DbEntity("e1");
        DbAttribute nonPk = new DbAttribute("nonPk", Types.INTEGER, e1);
        e1.addAttribute(nonPk);

        map.addDbEntity(e1);

        // DbLoader is so ugly and hard to test..
        Field dbEntityList = DbLoader.class.getDeclaredField("dbEntityList");
        dbEntityList.setAccessible(true);
        List<DbEntity> entities = (List<DbEntity>) dbEntityList.get(loader1);
        entities.add(e1);

        loader1.loadObjEntities(map, entities);

        ObjEntity oe1 = map.getObjEntity("E1");

        ObjAttribute oa1 = oe1.getAttribute("nonPk");
        assertEquals("int", oa1.getType());
    }
*/

}
