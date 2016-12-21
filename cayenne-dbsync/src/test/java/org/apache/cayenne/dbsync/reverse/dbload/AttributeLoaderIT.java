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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.sql.DatabaseMetaData;
import java.sql.Types;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AttributeLoaderIT extends BaseLoaderIT {

    @Test
    public void testAttributeLoad() throws Exception {
        createDbEntities();

        AttributeLoader loader = new AttributeLoader(adapter, EMPTY_CONFIG, new DefaultDbLoaderDelegate());
        loader.load(connection.getMetaData(), store);

        DbEntity artist = getDbEntity("ARTIST");
        DbAttribute a = getDbAttribute(artist, "ARTIST_ID");
        assertNotNull(a);
        assertEquals(Types.BIGINT, a.getType());
        assertTrue(a.isMandatory());
        assertFalse(a.isGenerated());

        a = getDbAttribute(artist, "ARTIST_NAME");
        assertNotNull(a);
        assertEquals(Types.CHAR, a.getType());
        assertEquals(254, a.getMaxLength());
        assertTrue(a.isMandatory());

        a = getDbAttribute(artist, "DATE_OF_BIRTH");
        assertNotNull(a);
        assertEquals(Types.DATE, a.getType());
        assertFalse(a.isMandatory());

        if(accessStackAdapter.supportsLobs()) {
            assertLobDbEntities();
        }

        if (adapter.supportsGeneratedKeys()) {
            assertGenerated();
        }
    }

    @Test
    public void testAttributeLoadTypes() throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        DbLoaderDelegate delegate = new DefaultDbLoaderDelegate();

        // We need all data to check relationships, so simply load it all
        EntityLoader entityLoader = new EntityLoader(adapter, EMPTY_CONFIG, delegate);
        AttributeLoader attributeLoader = new AttributeLoader(adapter, EMPTY_CONFIG, delegate);

        entityLoader.load(metaData, store);
        attributeLoader.load(metaData, store);

        DbEntity dbe = getDbEntity("PAINTING");
        DbEntity floatTest = getDbEntity("FLOAT_TEST");
        DbEntity smallintTest = getDbEntity("SMALLINT_TEST");
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

    private void assertGenerated() {
        DbEntity bag = getDbEntity("GENERATED_COLUMN_TEST");
        DbAttribute id = getDbAttribute(bag, "GENERATED_COLUMN");
        assertTrue(id.isGenerated());
    }

    private void assertLobDbEntities() {
        DbEntity blobEnt = getDbEntity("BLOB_TEST");
        assertNotNull(blobEnt);
        DbAttribute blobAttr = getDbAttribute(blobEnt, "BLOB_COL");
        assertNotNull(blobAttr);
        assertTrue(msgForTypeMismatch(Types.BLOB, blobAttr),
                Types.BLOB == blobAttr.getType()
                        || Types.VARBINARY == blobAttr.getType()
                        || Types.LONGVARBINARY == blobAttr.getType());

        DbEntity clobEnt = getDbEntity("CLOB_TEST");
        assertNotNull(clobEnt);
        DbAttribute clobAttr = getDbAttribute(clobEnt, "CLOB_COL");
        assertNotNull(clobAttr);
        assertTrue(msgForTypeMismatch(Types.CLOB, clobAttr),
                Types.CLOB == clobAttr.getType()
                        || Types.VARCHAR == clobAttr.getType()
                        || Types.LONGVARCHAR == clobAttr.getType());
    }



    private DbAttribute getDbAttribute(DbEntity ent, String name) {
        DbAttribute da = ent.getAttribute(name);
        // sometimes table names get converted to lowercase
        if (da == null) {
            da = ent.getAttribute(name.toLowerCase());
        }

        return da;
    }

    private static String msgForTypeMismatch(int origType, DbAttribute newAttr) {
        String nt = TypesMapping.getSqlNameByType(newAttr.getType());
        String ot = TypesMapping.getSqlNameByType(origType);
        return attrMismatch(newAttr.getName(), "expected type: <" + ot + ">, but was <" + nt + ">");
    }

    private static String attrMismatch(String attrName, String msg) {
        return "[Error loading attribute '" + attrName + "': " + msg + "]";
    }
}
