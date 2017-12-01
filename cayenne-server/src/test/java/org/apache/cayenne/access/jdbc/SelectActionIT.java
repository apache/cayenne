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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.lob.ClobTestEntity;
import org.apache.cayenne.testdo.lob.ClobTestRelation;
import org.apache.cayenne.testdo.lob.DistEntity;
import org.apache.cayenne.testdo.lob.DistEntityRel;
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
    private ServerRuntime serverRuntime;

    @Inject
    private DBHelper dbHelper;

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

    @Test
    public void testColumnSelect_DistinctResultIterator() throws Exception {
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

    @Test
    public void  testAddingDistinctToQuery() throws Exception{
        if (accessStackAdapter.supportsLobs()){
//            DistEntity obj = context.newObject(DistEntity.class);
//            obj.setName("ABC");
//            obj.setField("test".getBytes());
//            DistEntity obj2 = context.newObject(DistEntity.class);
//            obj2.setName("ABC1");
//            obj2.setField("test".getBytes());
//
//            DistEntityRel objRel1 = context.newObject(DistEntityRel.class);
//            objRel1.setNum(5);
//            DistEntityRel objRel2 = context.newObject(DistEntityRel.class);
//            objRel2.setNum(6);
//            DistEntityRel objRel3 = context.newObject(DistEntityRel.class);
//            objRel3.setNum(7);
//            DistEntityRel objRel4 = context.newObject(DistEntityRel.class);
//            objRel4.setNum(5);
//
//            obj.addToDistRel(objRel1);
//            obj.addToDistRel(objRel2);
//            obj.addToDistRel(objRel3);
//            obj2.addToDistRel(objRel4);

//            context.commitChanges();

            TableHelper tDistEntity = new TableHelper(dbHelper, "DIST_ENTITY");
            tDistEntity.setColumns("ID", "NAME", "FIELD");

            for (int i = 1; i <= 3; i++) {
                tDistEntity.insert(i, "dist_entity" + i, ("dist_entity" + i).getBytes());
            }

            TableHelper tDistEntityRel = new TableHelper(dbHelper, "DIST_ENTITY_REL");
            tDistEntityRel.setColumns("ID", "DIST_ID", "NUM");

            for (int i = 1; i <= 10; i++) {
                if(i < 5) {
                    tDistEntityRel.insert(i, 1, i);
                }
                else{
                    tDistEntityRel.insert(i, 3, i);
                }
            }

            ObjectContext objectContext = serverRuntime.newContext();

            SQLTemplate select = new SQLTemplate(DistEntity.class, "SELECT t0.FIELD, t0.NAME, t0.ID FROM DIST_ENTITY t0 JOIN DIST_ENTITY_REL t1 ON (t0.ID = t1.DIST_ID) WHERE (t1.NUM > 0) AND (t0.NAME LIKE 'dist_entity1')");
            List<DistEntity> list1 = objectContext.performQuery(select);
            assertEquals(4, list1.size());

            List<DistEntity> list2 = ObjectSelect.query(DistEntity.class)
                    .where(DistEntity.DIST_REL.dot(DistEntityRel.NUM).gt(0))
                    .and(DistEntity.NAME.like("dist_entity1"))
                    .select(objectContext);

            assertEquals(1,list2.size());
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
