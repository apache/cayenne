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

package org.apache.cayenne.dbsync.merge.token.db;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.MergeCase;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test synchronization of generated keys to DB (SetGeneratedFlagToDB merge token)
 *
 * As there are not many DBMS that fully support create/alter/drop for generated columns
 * this test has several actual paths of execution.
 *
 * 1. If DB has no support for generated values at all
 * this test will check that no meaningful tokens are created in sync process
 *
 * 2. If DB can create generated columns but can't alter them
 * this test will check that proper exception it thrown when applying token
 *
 * 3. If DB can alter generated columns then full check will be performed
 * (here is actually two variants as some DB can only drop whereas some can only add generated attribute)
 *
 * @see DbAdapter#supportsGeneratedKeys()
 * @see UnitDbAdapter#supportsGeneratedKeys()
 * @see UnitDbAdapter#supportsGeneratedKeysAdd()
 * @see UnitDbAdapter#supportsGeneratedKeysDrop()
 *
 * @since 4.0
 */
public class SetGeneratedFlagToDbIT extends MergeCase {

    @Inject
    UnitDbAdapter dbAdapter;

    @Test
    public void setGeneratedFlag() throws Exception {
        DbEntity dbEntity = createTestTable(false);
        assertNotNull(dbEntity);

        DbAttribute attribute = dbEntity.getAttribute("ID");
        assertNotNull(attribute);
        assertFalse(attribute.isGenerated());

        attribute.setGenerated(true);

        List<MergerToken> tokens = createMergeTokens();
        if(!dbAdapter.supportsGeneratedKeys()) {
            assertEquals(0, tokens.size());
            return;
        }

        assertEquals(1, tokens.size());
        MergerToken token = tokens.get(0);
        assertTrue(token instanceof SetGeneratedFlagToDb);

        try {
            execute(token);
            if(!dbAdapter.supportsGeneratedKeysAdd()) {
                fail("SetGeneratedFlagToDb should fail on current DB");
            }
        } catch (UnsupportedOperationException ignored) {
            return;
        }

        assertTokensAndExecute(0, 0);
    }

    @Test
    public void dropGeneratedFlag() throws Exception {

        DbEntity dbEntity = createTestTable(true);
        assertNotNull(dbEntity);

        DbAttribute attribute = dbEntity.getAttribute("ID");
        assertNotNull(attribute);
        assertTrue(attribute.isGenerated());

        attribute.setGenerated(false);

        List<MergerToken> tokens = createMergeTokens();
        if(!dbAdapter.supportsGeneratedKeys()) {
            assertEquals(0, tokens.size());
            return;
        }

        assertEquals(1, tokens.size());

        MergerToken token = tokens.get(0);
        assertTrue(token instanceof SetGeneratedFlagToDb);

        try {
            execute(token);
            if(!dbAdapter.supportsGeneratedKeysDrop()) {
                fail("SetGeneratedFlagToDb should fail on current DB");
            }
        } catch (UnsupportedOperationException ignored) {
            return;
        }

        assertTokensAndExecute(0, 0);
    }

    private DbEntity createTestTable(boolean generated) throws Exception {
        dropTestTables();

        DbEntity withGenKey = new DbEntity("NEW_TABLE");
        DbAttribute attribute = new DbAttribute("ID", Types.INTEGER, withGenKey);
        attribute.setMandatory(true);
        attribute.setPrimaryKey(true);
        attribute.setGenerated(generated);
        withGenKey.addAttribute(attribute);
        map.addDbEntity(withGenKey);

        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);

        return withGenKey;
    }

    @After
    public void dropTestTables() throws Exception {
        if(map.getDbEntity("NEW_TABLE") != null) {
            map.removeDbEntity("NEW_TABLE");
        }
        dropTableIfPresent("NEW_TABLE");
        assertTokensAndExecute(0, 0);
    }
}
