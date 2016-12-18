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
package org.apache.cayenne.access.jdbc;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.testdo.lob.ClobTestRelation;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseServerRuntime(CayenneProjects.LOB_PROJECT)
public class SelectActionIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Test
    public void testFetchLimit_DistinctResultIterator() throws Exception {
        if (accessStackAdapter.supportsLobs()) {

            insertClobDb();

            Expression qual = ExpressionFactory.exp("clobValue.value = 100");
            SelectQuery select = new SelectQuery(ClobTestEntity.class, qual);
            select.setFetchLimit(25);
            List<DataRow> resultRows = context.performQuery(select);

            assertNotNull(resultRows);
            assertEquals(25, resultRows.size());
        }
    }

    protected void insertClobDb() {
        ClobTestEntity obj;
        for (int i = 0; i < 80; i++) {
            if (i < 20) {
                obj = (ClobTestEntity) context.newObject("ClobTestEntity");
                obj.setClobCol("a1" + i);
                insetrClobRel(obj);
            }
            else {
                obj = (ClobTestEntity) context.newObject("ClobTestEntity");
                obj.setClobCol("a2");
                insetrClobRel(obj);
            }
        }

        context.commitChanges();
    }

    protected void insetrClobRel(ClobTestEntity clobId) {
        ClobTestRelation obj;

        for (int i = 0; i < 20; i++) {
            obj = (ClobTestRelation) context.newObject("ClobTestRelation");
            obj.setValue(100);
            obj.setClobId(clobId);
        }
    }
}
