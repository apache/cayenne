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

import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneCase;

public class DbLoaderTest extends CayenneCase {

    protected DbLoader loader;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        loader = new DbLoader(getConnection(), getNode().getAdapter(), null);
    }

    public void testGetTableTypes() throws Exception {
        try {
            List tableTypes = loader.getTableTypes();

            assertNotNull(tableTypes);

            String tableLabel = getNode().getAdapter().tableTypeForTable();
            if (tableLabel != null) {
                assertTrue(
                        "Missing type for table '" + tableLabel + "' - " + tableTypes,
                        tableTypes.contains(tableLabel));
            }

            String viewLabel = getNode().getAdapter().tableTypeForView();
            if (viewLabel != null) {
                assertTrue(
                        "Missing type for view '" + viewLabel + "' - " + tableTypes,
                        tableTypes.contains(viewLabel));
            }
        }
        finally {
            loader.getConnection().close();
        }
    }

    public void testGetTables() throws Exception {
        try {

            String tableLabel = getNode().getAdapter().tableTypeForTable();

            List<DbEntity> tables = loader.getTables(null, null, "%", new String[] {
                tableLabel
            });

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
        finally {
            loader.getConnection().close();
        }
    }

    public void testLoadWithMeaningfulPK() throws Exception {
        try {

            DataMap map = new DataMap();
            String tableLabel = getNode().getAdapter().tableTypeForTable();

            loader.setCreatingMeaningfulPK(true);
            
            List<DbEntity> testLoader = loader.getTables(
                    null,
                    null,
                    "artist",
                    new String[] {
                        tableLabel
                    });
            if(testLoader.size()==0){
                testLoader = loader.getTables(
                        null,
                        null,
                        "ARTIST",
                        new String[] {
                            tableLabel
                        });
            }
            
            loader.loadDbEntities(map, testLoader);

            loader.loadObjEntities(map);
            ObjEntity artist = map.getObjEntity("Artist");
            assertNotNull(artist);
            ObjAttribute id = (ObjAttribute) artist.getAttribute("artistId");
            assertNotNull(id);
        }
        finally {
            loader.getConnection().close();
        }
    }

    /**
     * DataMap loading is in one big test method, since breaking it in individual tests
     * would require multiple reads of metatdata which is extremely slow on some RDBMS
     * (Sybase).
     */
    public void testLoad() throws Exception {
        try {
            boolean supportsUnique = getNode().getAdapter().supportsUniqueConstraints();
            boolean supportsLobs = getAccessStackAdapter().supportsLobs();
            boolean supportsFK = getAccessStackAdapter().supportsFKConstraints();

            DataMap map = new DataMap();
            String tableLabel = getNode().getAdapter().tableTypeForTable();

            // *** TESTING THIS ***
            loader.loadDbEntities(map, loader.getTables(null, null, "%", new String[] {
                tableLabel
            }));

            assertDbEntities(map);

            if (supportsLobs) {
                assertLobDbEntities(map);
            }

            // *** TESTING THIS ***
            loader.loadDbRelationships(map);

            if (supportsFK) {
                Collection rels = getDbEntity(map, "ARTIST").getRelationships();
                assertNotNull(rels);
                assertTrue(rels.size() > 0);

                // test one-to-one
                rels = getDbEntity(map, "PAINTING").getRelationships();
                assertNotNull(rels);

                // find relationship to PAINTING_INFO
                DbRelationship oneToOne = null;
                Iterator it = rels.iterator();
                while (it.hasNext()) {
                    DbRelationship rel = (DbRelationship) it.next();
                    if ("PAINTING_INFO".equalsIgnoreCase(rel.getTargetEntityName())) {
                        oneToOne = rel;
                        break;
                    }
                }

                assertNotNull("No relationship to PAINTING_INFO", oneToOne);
                assertFalse("Relationship to PAINTING_INFO must be to-one", oneToOne
                        .isToMany());
                assertTrue("Relationship to PAINTING_INFO must be to-one", oneToOne
                        .isToDependentPK());

                // test UNIQUE only if FK is supported...
                if (supportsUnique) {
                    assertUniqueConstraintsInRelationships(map);
                }
            }

            // *** TESTING THIS ***
            loader.setCreatingMeaningfulPK(false);
            loader.loadObjEntities(map);

            ObjEntity ae = map.getObjEntity("Artist");
            assertNotNull(ae);
            assertEquals("Artist", ae.getName());
            // assert primary key is not an attribute
            assertNull(ae.getAttribute("artistId"));
            if (supportsLobs) {
                assertLobObjEntities(map);
            }

            if (supportsFK) {
                Collection rels1 = ae.getRelationships();
                assertNotNull(rels1);
                assertTrue(rels1.size() > 0);
            }

            // now when the map is loaded, test
            // various things
            // selectively check how different types were processed
            if (getAccessStackAdapter().supportsColumnTypeReengineering()) {
                checkTypes(map);
            }
        }
        finally {
            loader.getConnection().close();
        }
    }

    private void assertUniqueConstraintsInRelationships(DataMap map) {
        // unfortunately JDBC metadata doesn't provide info for UNIQUE constraints....
        // cant reengineer them...

        // find rel to TO_ONEFK1
        /*
         * Iterator it = getDbEntity(map, "TO_ONEFK2").getRelationships().iterator();
         * DbRelationship rel = (DbRelationship) it.next(); assertEquals("TO_ONEFK1",
         * rel.getTargetEntityName()); assertFalse("UNIQUE constraint was ignored...",
         * rel.isToMany());
         */
    }

    private void assertDbEntities(DataMap map) {
        DbEntity dae = getDbEntity(map, "ARTIST");
        assertNotNull(
                "Null 'ARTIST' entity, other DbEntities: " + map.getDbEntityMap(),
                dae);
        assertEquals("ARTIST", dae.getName().toUpperCase());
        DbAttribute a = getDbAttribute(dae, "ARTIST_ID");
        assertNotNull(a);
        assertTrue(a.isPrimaryKey());
    }

    private void assertLobDbEntities(DataMap map) {
        DbEntity blobEnt = getDbEntity(map, "BLOB_TEST");
        assertNotNull(blobEnt);
        DbAttribute blobAttr = getDbAttribute(blobEnt, "BLOB_COL");
        assertNotNull(blobAttr);
        assertTrue(msgForTypeMismatch(Types.BLOB, blobAttr), Types.BLOB == blobAttr
                .getType()
                || Types.LONGVARBINARY == blobAttr.getType());
        DbEntity clobEnt = getDbEntity(map, "CLOB_TEST");
        assertNotNull(clobEnt);
        DbAttribute clobAttr = getDbAttribute(clobEnt, "CLOB_COL");
        assertNotNull(clobAttr);
        assertTrue(msgForTypeMismatch(Types.CLOB, clobAttr), Types.CLOB == clobAttr
                .getType()
                || Types.LONGVARCHAR == clobAttr.getType());
    }

    private void assertLobObjEntities(DataMap map) {
        ObjEntity blobEnt = map.getObjEntity("BlobTest");
        assertNotNull(blobEnt);
        // BLOBs should be mapped as byte[]
        ObjAttribute blobAttr = (ObjAttribute) blobEnt.getAttribute("blobCol");
        assertNotNull("BlobTest.blobCol failed to load", blobAttr);
        assertEquals("byte[]", blobAttr.getType());
        ObjEntity clobEnt = map.getObjEntity("ClobTest");
        assertNotNull(clobEnt);
        // CLOBs should be mapped as Strings by default
        ObjAttribute clobAttr = (ObjAttribute) clobEnt.getAttribute("clobCol");
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
        DbAttribute da = (DbAttribute) ent.getAttribute(name);
        // sometimes table names get converted to lowercase
        if (da == null) {
            da = (DbAttribute) ent.getAttribute(name.toLowerCase());
        }

        return da;
    }

    private DataMap originalMap() {
        return getNode().getDataMaps().iterator().next();
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
        assertTrue(
                msgForTypeMismatch(Types.DECIMAL, decimalAttr),
                Types.DECIMAL == decimalAttr.getType()
                        || Types.NUMERIC == decimalAttr.getType());
        assertEquals(2, decimalAttr.getScale());

        // check varchar
        assertEquals(
                msgForTypeMismatch(Types.VARCHAR, varcharAttr),
                Types.VARCHAR,
                varcharAttr.getType());
        assertEquals(255, varcharAttr.getMaxLength());
        // check integer
        assertEquals(
                msgForTypeMismatch(Types.INTEGER, integerAttr),
                Types.INTEGER,
                integerAttr.getType());
        // check float
        assertTrue(msgForTypeMismatch(Types.FLOAT, floatAttr), Types.FLOAT == floatAttr
                .getType()
                || Types.DOUBLE == floatAttr.getType()
                || Types.REAL == floatAttr.getType());

        // check smallint
        assertTrue(
                msgForTypeMismatch(Types.SMALLINT, smallintAttr),
                Types.SMALLINT == smallintAttr.getType()
                        || Types.INTEGER == smallintAttr.getType());
    }

    public void checkAllDBEntities(DataMap map) {

        for (DbEntity origEnt : originalMap().getDbEntities()) {
            DbEntity newEnt = map.getDbEntity(origEnt.getName());
            for (DbAttribute origAttr : origEnt.getAttributes()) {
                DbAttribute newAttr = (DbAttribute) newEnt.getAttribute(origAttr
                        .getName());
                assertNotNull(
                        "No matching DbAttribute for '" + origAttr.getName(),
                        newAttr);
                assertEquals(
                        msgForTypeMismatch(origAttr, newAttr),
                        origAttr.getType(),
                        newAttr.getType());
                // length and precision doesn't have to be the same
                // it must be greater or equal
                assertTrue(origAttr.getMaxLength() <= newAttr.getMaxLength());
                assertTrue(origAttr.getScale() <= newAttr.getScale());
            }
        }
    }

    private String msgForTypeMismatch(DbAttribute origAttr, DbAttribute newAttr) {
        return msgForTypeMismatch(origAttr.getType(), newAttr);
    }

    private String msgForTypeMismatch(int origType, DbAttribute newAttr) {
        String nt = TypesMapping.getSqlNameByType(newAttr.getType());
        String ot = TypesMapping.getSqlNameByType(origType);
        return attrMismatch(newAttr.getName(), "expected type: <"
                + ot
                + ">, but was <"
                + nt
                + ">");
    }

    private String attrMismatch(String attrName, String msg) {
        StringBuffer buf = new StringBuffer();
        buf.append("[Error loading attribute '").append(attrName).append("': ").append(
                msg).append("]");
        return buf.toString();
    }
}
