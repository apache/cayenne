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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SelectQueryClobIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LOB_PROJECT);

    private void createClobDataSet() throws Exception {
        TableHelper tClobTest = env.table("CLOB_TEST", "CLOB_TEST_ID", "CLOB_COL");

        tClobTest.deleteAll();

        tClobTest.insert(1, "clob1");
        tClobTest.insert(2, "clob2");
    }

    /**
     * Test how "like ignore case" works when using uppercase parameter.
     */
    @Test
    public void selectLikeIgnoreCaseClob() throws Exception {
        if (env.testDbAdapter().supportsLobs()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.likeIgnoreCase("clob%"))
                    .select(env.context());
            assertEquals(2, objects.size());
        }
    }

    @Test
    public void selectFetchLimit_Offset_DistinctClob() throws Exception {
        if (env.testDbAdapter().supportsLobs()) {
            createClobDataSet();

            // see CAY-1539... CLOB column causes suppression of DISTINCT in
            // SQL, and hence the offset processing is done in memory
            List<ClobTestEntity> objects = ObjectSelect.query(ClobTestEntity.class)
                    .orderBy("db:" + ClobTestEntity.CLOB_TEST_ID_PK_COLUMN, SortOrder.ASCENDING)
                    .limit(1)
                    .offset(1)
                    .select(env.context());
            assertEquals(1, objects.size());
            assertEquals(2, Cayenne.intPKForObject(objects.get(0)));
        }
    }

    @Test
    public void selectEqualsClob() throws Exception {
        if (env.testDbAdapter().supportsLobComparisons()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.eq("clob1"))
                    .select(env.context());
            assertEquals(1, objects.size());
        }
    }

    @Test
    public void selectNotEqualsClob() throws Exception {
        if (env.testDbAdapter().supportsLobComparisons()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.ne("clob1"))
                    .select(env.context());
            assertEquals(1, objects.size());
        }
    }

    @Test
    public void selectNotEqualsEmptyClob() throws Exception {
        if (env.testDbAdapter().supportsLobComparisons()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.ne(""))
                    .select(env.context());
            assertEquals(2, objects.size());
        }
    }
}