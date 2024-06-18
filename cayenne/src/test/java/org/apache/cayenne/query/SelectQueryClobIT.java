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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.LOB_PROJECT)
public class SelectQueryClobIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    private void createClobDataSet() throws Exception {
        TableHelper tClobTest = new TableHelper(dbHelper, "CLOB_TEST");
        tClobTest.setColumns("CLOB_TEST_ID", "CLOB_COL");

        tClobTest.deleteAll();

        tClobTest.insert(1, "clob1");
        tClobTest.insert(2, "clob2");
    }

    /**
     * Test how "like ignore case" works when using uppercase parameter.
     */
    @Test
    public void testSelectLikeIgnoreCaseClob() throws Exception {
        if (accessStackAdapter.supportsLobs()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.likeIgnoreCase("clob%"))
                    .select(context);
            assertEquals(2, objects.size());
        }
    }

    @Test
    public void testSelectFetchLimit_Offset_DistinctClob() throws Exception {
        if (accessStackAdapter.supportsLobs()) {
            createClobDataSet();

            // see CAY-1539... CLOB column causes suppression of DISTINCT in
            // SQL, and hence the offset processing is done in memory
            List<ClobTestEntity> objects = ObjectSelect.query(ClobTestEntity.class)
                    .orderBy("db:" + ClobTestEntity.CLOB_TEST_ID_PK_COLUMN, SortOrder.ASCENDING)
                    .limit(1)
                    .offset(1)
                    .select(context);
            assertEquals(1, objects.size());
            assertEquals(2, Cayenne.intPKForObject(objects.get(0)));
        }
    }

    @Test
    public void testSelectEqualsClob() throws Exception {
        if (accessStackAdapter.supportsLobComparisons()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.eq("clob1"))
                    .select(context);
            assertEquals(1, objects.size());
        }
    }

    @Test
    public void testSelectNotEqualsClob() throws Exception {
        if (accessStackAdapter.supportsLobComparisons()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.ne("clob1"))
                    .select(context);
            assertEquals(1, objects.size());
        }
    }

    @Test
    public void testSelectNotEqualsEmptyClob() throws Exception {
        if (accessStackAdapter.supportsLobComparisons()) {
            createClobDataSet();
            List<?> objects = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_COL.ne(""))
                    .select(context);
            assertEquals(2, objects.size());
        }
    }
}