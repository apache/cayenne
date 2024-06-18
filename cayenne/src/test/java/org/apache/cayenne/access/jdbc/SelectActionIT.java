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

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.testdo.lob.ClobTestRelation;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore("Temporary ignore this test to debug GitHub Actions failure")
@UseCayenneRuntime(CayenneProjects.LOB_PROJECT)
public class SelectActionIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Test
    public void testFetchLimit_DistinctResultIterator() {
        if (accessStackAdapter.supportsLobs()) {

            insertClobDb();

            List<ClobTestEntity> resultRows = ObjectSelect.query(ClobTestEntity.class)
                    .where(ClobTestEntity.CLOB_VALUE.dot(ClobTestRelation.VALUE).eq(100))
                    .limit(25)
                    .select(context);

            assertNotNull(resultRows);
            assertEquals(25, resultRows.size());
        }
    }

    @Test
    public void testColumnSelect_DistinctResultIterator() {
        if (accessStackAdapter.supportsLobs()) {

            insertClobDb();

            List<String> result = ObjectSelect.query(ClobTestEntity.class)
                    .column(ClobTestEntity.CLOB_COL)
                    .where(ClobTestEntity.CLOB_VALUE.dot(ClobTestRelation.VALUE).eq(100))
                    .select(context);

            // this should be 80, but we got only single values and we forcing distinct on them
            // so here will be only 21 elements that are unique
            assertEquals(21, result.size());
        }
    }

    protected void insertClobDb() {
        for (int i = 0; i < 80; i++) {
            ClobTestEntity obj = context.newObject(ClobTestEntity.class);
            if (i < 20) {
                obj.setClobCol("a1" + i);
            } else {
                obj.setClobCol("a2");
            }
            insertClobRel(obj);
        }
        context.commitChanges();
    }

    protected void insertClobRel(ClobTestEntity clobId) {
        for (int i = 0; i < 20; i++) {
            ClobTestRelation obj = context.newObject(ClobTestRelation.class);
            obj.setValue(100);
            obj.setClobId(clobId);
        }
    }
}
