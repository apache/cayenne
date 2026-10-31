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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests the deprecated {@link SQLSelect}. Its functionality is tested via {@link SQLQuery} in {@link SQLQueryIT}.
 */
@SuppressWarnings("removal")
public class SQLSelectIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @BeforeEach
    public void before() throws Exception {
        TableHelper tPainting = env.table("PAINTING")
                .setColumns("PAINTING_ID", "PAINTING_TITLE", "ESTIMATED_PRICE")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, Types.DECIMAL);
        for (int i = 1; i <= 3; i++) {
            tPainting.insert(i, "painting" + i, 10000. * i);
        }
    }

    @Test
    public void dataRowQuery() {
        List<DataRow> result = SQLSelect.dataRowQuery("SELECT * FROM PAINTING").select(env.context());
        assertEquals(3, result.size());
    }

    @Test
    public void dataRowQuery_DataMapNameRoot() {
        List<DataRow> result = SQLSelect.dataRowQuery("testmap", "SELECT * FROM PAINTING").select(env.context());
        assertEquals(3, result.size());
    }

    @Test
    public void query_Objects() {
        List<Painting> result = SQLSelect.query(Painting.class, "SELECT * FROM PAINTING")
                .upperColumnNames()
                .select(env.context());
        assertEquals(3, result.size());
        assertInstanceOf(Painting.class, result.getFirst());
    }

    @Test
    public void scalarQuery() {
        Integer count = SQLSelect.scalarQuery("SELECT COUNT(*) FROM PAINTING", Integer.class)
                .selectOne(env.context());
        assertEquals(3, count);
    }

    @Test
    public void columnQuery() {
        List<Object[]> result = SQLSelect
                .columnQuery("SELECT PAINTING_ID, PAINTING_TITLE FROM PAINTING", Integer.class, String.class)
                .select(env.context());
        assertEquals(3, result.size());
        assertEquals(2, result.getFirst().length);
    }

    @Test
    public void fluentChain_KeepsType() {
        // the chained assignment of the pre-6.0 code must still compile
        SQLSelect<DataRow> query = SQLSelect.dataRowQuery("SELECT * FROM PAINTING")
                .localCache()
                .limit(2)
                .upperColumnNames()
                .dataNode(env.dataNode().getName());
        assertEquals(2, query.select(env.context()).size());
    }
}
