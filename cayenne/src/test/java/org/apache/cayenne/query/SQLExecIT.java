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
package org.apache.cayenne.query;

import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the deprecated {@link SQLExec}. Its functionality is tested via {@link SQLQuery} in {@link SQLQueryIT}.
 */
@SuppressWarnings("removal")
public class SQLExecIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void update() throws Exception {
        int inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')").update(env.context());
        assertEquals(1, inserted);
        assertEquals(1L, env.table("ARTIST").getLong("ARTIST_ID"));
    }

    @Test
    public void update_DataMapNameRoot() {
        int inserted = SQLExec.query("testmap", "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')")
                .update(env.context());
        assertEquals(1, inserted);
    }

    @Test
    public void updateBatch() {
        int[] inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')")
                .updateBatch(env.context());
        assertArrayEquals(new int[]{1}, inserted);
    }

    @Test
    public void params_NameValue() throws Exception {
        int inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))")
                .params("id", 55)
                .params("name", "a3")
                .update(env.context());

        assertEquals(1, inserted);
        assertEquals(55L, env.table("ARTIST").getLong("ARTIST_ID"));
        assertEquals("a3", env.table("ARTIST").getString("ARTIST_NAME").trim());
    }

    @Test
    public void fluentChain_KeepsType() {
        // the chained assignment of the pre-6.0 code must still compile
        SQLExec query = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))")
                .paramsArray(7, "a")
                .dataNode(env.dataNode().getName());
        assertEquals(1, query.update(env.context()));
    }
}
