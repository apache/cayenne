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

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SQLExecIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private DBHelper dbHelper;
    private UnitDbAdapter unitDbAdapter;

    @BeforeEach
    public void setUp() {
        context = env.dataContext();
        dbHelper = env.dbHelper();
        unitDbAdapter = env.getInstance(UnitDbAdapter.class);
    }

    @Test
    public void dataMapNameRoot() throws Exception {
        int inserted = SQLExec.query("testmap", "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')").update(
                context);
        assertEquals(1, inserted);
    }

    @Test
    public void defaultRoot() throws Exception {
        int inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')").update(context);
        assertEquals(1, inserted);
    }

    @Test
    public void returnGeneratedKeys() {
        if(unitDbAdapter.supportsGeneratedKeys()) {
            QueryResult response = SQLExec.query("testmap", "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Surikov')")
                    .returnGeneratedKeys(true)
                    .execute(context);
            assertEquals(2, response.size());

            QueryResult response1 = SQLExec.query("testmap", "INSERT INTO GENERATED_COLUMN (NAME) VALUES ('Sidorov')")
                    .returnGeneratedKeys(false)
                    .execute(context);
            assertEquals(1, response1.size());
        }
    }

    @Test
    public void paramsArray_Single() throws Exception {

        int inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, #bind($name))")
                .paramsArray("a3").update(context);

        assertEquals(1, inserted);
        assertEquals("a3", dbHelper.getString("ARTIST", "ARTIST_NAME").trim());
    }

    @Test
    public void executeSelect() throws Exception {
        int inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (1, 'a')").update(context);
        assertEquals(1, inserted);

        QueryResult result = SQLExec.query("SELECT * FROM ARTIST").execute(context);
        assertEquals(2, result.size());
        assertTrue(result.isList());
        assertEquals(1, result.firstList().size());

        DataRow row = (DataRow)result.firstList().get(0);
        if(unitDbAdapter.isLowerCaseNames()) {
            assertTrue(row.containsKey("artist_id"));
            assertEquals(1L, ((Number)row.get("artist_id")).longValue());
            assertEquals("a", row.get("artist_name"));
        } else {
            assertTrue(row.containsKey("ARTIST_ID"));
            assertEquals(1L, ((Number)row.get("ARTIST_ID")).longValue());
            assertEquals("a", row.get("ARTIST_NAME"));
        }
    }

    @Test
    public void paramsArray_Multiple() throws Exception {

        int inserted = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))")
                .paramsArray(55, "a3").update(context);

        assertEquals(1, inserted);
        assertEquals(55L, dbHelper.getLong("ARTIST", "ARTIST_ID"));
        assertEquals("a3", dbHelper.getString("ARTIST", "ARTIST_NAME").trim());
    }

    @Test
    public void execute_MultipleArrayBind() throws Exception {
        SQLExec inserter = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))");
        for(int i = 0; i < 2; i++) {
            QueryResult<?> result = inserter.paramsArray(i, "artist " + i).execute(context);
            assertEquals(1, result.firstUpdateCount());
        }
        assertEquals(2, dbHelper.getRowCount("ARTIST"));
    }

    @Test
    public void execute_MultipleMapBind() throws Exception {
        SQLExec inserter = SQLExec.query("INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (#bind($id), #bind($name))");
        for(int i = 0; i < 2; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("id", i);
            params.put("name", "artist " + i);
            QueryResult<?> result = inserter.params(params).execute(context);
            assertEquals(1, result.firstUpdateCount());
        }
        assertEquals(2, dbHelper.getRowCount("ARTIST"));
    }
}
