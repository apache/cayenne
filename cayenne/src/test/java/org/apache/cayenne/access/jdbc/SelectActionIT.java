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
package org.apache.cayenne.access.jdbc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.testdo.lob.ClobTestRelation;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SelectActionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LOB_PROJECT);

    @Test
    public void fetchLimit_DistinctResultIterator() {
        if (env.testDbAdapter().supportsLobs()) {

            insertClobDb();

            List<ClobTestEntity> resultRows = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_VALUE.dot(ClobTestRelation.VALUE).eq(100))
                    .limit(25)
                    .select(env.context());

            assertNotNull(resultRows);
            assertEquals(25, resultRows.size());
        }
    }

    @Test
    public void columnSelect_DistinctResultIterator() {
        if (env.testDbAdapter().supportsLobs()) {

            insertClobDb();

            List<String> result = ObjectSelect.query(ClobTestEntity.class)
                    .column(ClobTestEntity.CLOB_COL)
                    .where(ClobTestEntity.CLOB_VALUE.dot(ClobTestRelation.VALUE).eq(100))
                    .select(env.context());

            // this should be 80, but we got only single values and we forcing distinct on them
            // so here will be only 21 elements that are unique
            assertEquals(21, result.size());
            assertEquals(expectedClobValues(), new HashSet<>(result));
        }
    }

    @Test
    public void columnSelectWithoutJoin_DistinctResultIterator() {
        if (env.testDbAdapter().supportsLobs()) {

            insertClobDb();

            List<String> result = ObjectSelect.query(ClobTestEntity.class)
                    .column(ClobTestEntity.CLOB_COL)
                    .distinct()
                    .select(env.context());

            assertEquals(21, result.size());
            assertEquals(expectedClobValues(), new HashSet<>(result));
        }
    }

    protected Set<String> expectedClobValues() {
        Set<String> expected = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            expected.add("a1" + i);
        }
        expected.add("a2");
        return expected;
    }

    protected void insertClobDb() {
        for (int i = 0; i < 80; i++) {
            ClobTestEntity obj = env.context().newObject(ClobTestEntity.class);
            if (i < 20) {
                obj.setClobCol("a1" + i);
            } else {
                obj.setClobCol("a2");
            }
            insertClobRel(obj);
        }
        env.context().commitChanges();

        // sanity check
        assertEquals(80L, ObjectSelect.query(ClobTestEntity.class).selectCount(env.context()));
        assertEquals(1600L, ObjectSelect.query(ClobTestRelation.class).selectCount(env.context()));
    }

    protected void insertClobRel(ClobTestEntity clobId) {
        for (int i = 0; i < 20; i++) {
            ClobTestRelation obj = env.context().newObject(ClobTestRelation.class);
            obj.setValue(100);
            obj.setClobId(clobId);
        }
    }
}
