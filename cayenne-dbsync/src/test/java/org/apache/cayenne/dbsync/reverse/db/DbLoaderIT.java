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

package org.apache.cayenne.dbsync.reverse.db;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DetectedDbEntity;
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

import java.sql.Connection;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    private Connection connection;

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

    @Before
    public void before() throws Exception {
        this.connection = dataSourceFactory.getSharedDataSource().getConnection();
    }

    private DbLoader createDbLoader(boolean meaningfulPK, boolean meaningfulFK) {
        return new DbLoader(connection, adapter, null, new DefaultObjectNameGenerator());
    }

    @After
    public void after() throws Exception {
        connection.close();
    }

    @Test
    public void testGetTables() throws Exception {

        String tableLabel = adapter.tableTypeForTable();

        DbLoader loader = createDbLoader(false, false);

        List<DetectedDbEntity> tables = loader.createTableLoader(null, null, TableFilter.everything())
                .getDbEntities(TableFilter.everything(), new String[]{tableLabel});

        assertNotNull(tables);

        boolean foundArtist = false;

        for (DetectedDbEntity table : tables) {
            if ("ARTIST".equalsIgnoreCase(table.getName())) {
                foundArtist = true;
                break;
            }
        }

        assertTrue("'ARTIST' is missing from the table list: " + tables, foundArtist);
    }

    @Test
    public void testGetTablesWithWrongCatalog() throws Exception {

        DbLoader loader = createDbLoader(false, false);

        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(
                FiltersConfig.create("WRONG", null, TableFilter.everything(), PatternFilter.INCLUDE_NOTHING));
        List<DetectedDbEntity> tables = loader
                .createTableLoader("WRONG", null, TableFilter.everything())
                .getDbEntities(TableFilter.everything(), new String[]{adapter.tableTypeForTable()});

        assertNotNull(tables);
        assertTrue(tables.isEmpty());
    }

    @Test
    public void testGetTablesWithWrongSchema() throws Exception {

        DbLoader loader = createDbLoader(false, false);

        DbLoaderConfiguration config = new DbLoaderConfiguration();
        config.setFiltersConfig(
                FiltersConfig.create(null, "WRONG", TableFilter.everything(), PatternFilter.INCLUDE_NOTHING));
        List<DetectedDbEntity> tables = loader
                .createTableLoader(null, "WRONG", TableFilter.everything())
                .getDbEntities(TableFilter.everything(), new String[]{adapter.tableTypeForTable()});

        assertNotNull(tables);
        assertTrue(tables.isEmpty());
    }

    /**
     * DataMap loading is in one big test method, since breaking it in
     * individual tests would require multiple reads of metatdata which is
     * extremely slow on some RDBMS (Sybase).
     */
    @Test
    public void testLoad() throws Exception {
        DbLoader loader = createDbLoader(false, false);

        boolean supportsUnique = runtime.getDataDomain().getDataNodes().iterator().next().getAdapter()
                .supportsUniqueConstraints();
        boolean supportsLobs = accessStackAdapter.supportsLobs();
        boolean supportsFK = accessStackAdapter.supportsFKConstraints();

        DataMap map = new DataMap();
        map.setDefaultPackage("foo.x");

        String tableLabel = adapter.tableTypeForTable();

        // *** TESTING THIS ***
        List<DbEntity> entities = loader
                .createTableLoader(null, null, TableFilter.everything())
                .loadDbEntities(map, CONFIG, new String[]{adapter.tableTypeForTable()});


        assertDbEntities(map);

        if (supportsLobs) {
            assertLobDbEntities(map);
        }

        // *** TESTING THIS ***
        loader.loadDbRelationships(CONFIG, null, null, entities);

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

        // now when the map is loaded, test
        // various things selectively check how different types were processed
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

        DbAttribute a = getDbAttribute(dae, "ARTIST_ID");
        assertNotNull(a);
        assertTrue(a.isPrimaryKey());
        assertFalse(a.isGenerated());

        if (adapter.supportsGeneratedKeys()) {
            DbEntity bag = getDbEntity(map, "GENERATED_COLUMN_TEST");
            DbAttribute id = getDbAttribute(bag, "GENERATED_COLUMN");
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

/*
        DbEntity nclobEnt = getDbEntity(map, "NCLOB_TEST");
        assertNotNull(nclobEnt);
        DbAttribute nclobAttr = getDbAttribute(nclobEnt, "NCLOB_COL");
        assertNotNull(nclobAttr);
        assertTrue(msgForTypeMismatch(Types.NCLOB, nclobAttr), Types.NCLOB == nclobAttr.getType()
                || Types.LONGVARCHAR == nclobAttr.getType());
*/
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


        ObjEntity nclobEnt = map.getObjEntity("NclobTest");
        assertNotNull(nclobEnt);
        // CLOBs should be mapped as Strings by default
        ObjAttribute nclobAttr = nclobEnt.getAttribute("nclobCol");
        assertNotNull(nclobAttr);
        assertEquals(String.class.getName(), nclobAttr.getType());
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
}
