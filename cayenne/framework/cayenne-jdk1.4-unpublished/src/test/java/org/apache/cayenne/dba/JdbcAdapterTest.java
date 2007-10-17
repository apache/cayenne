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

package org.apache.cayenne.dba;

import java.sql.Types;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DerivedDbEntity;
import org.apache.cayenne.unit.CayenneCase;

public class JdbcAdapterTest extends CayenneCase {

    protected JdbcAdapter adapter;

    protected void setUp() throws java.lang.Exception {
        adapter = new JdbcAdapter();
    }

    public void testExternalTypesForJdbcType() throws Exception {
        // check a few types
        checkType(Types.BLOB);
        checkType(Types.ARRAY);
        checkType(Types.DATE);
        checkType(Types.VARCHAR);
    }

    public void testCreateTable() throws Exception {
        DbEntity e = getDbEntity("ARTIST_ASSETS");
        assertNotNull(e);
        assertTrue(e instanceof DerivedDbEntity);

        // an attempt to create a derived table must generate an exception
        try {
            adapter.createTable(e);
            fail("Derived tables shouldn't be allowed in 'create'.");
        }
        catch (Exception ex) {
            // exception expected
        }
    }

    private void checkType(int type) throws java.lang.Exception {
        String[] types = adapter.externalTypesForJdbcType(type);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals(TypesMapping.getSqlNameByType(type), types[0]);
    }
}
