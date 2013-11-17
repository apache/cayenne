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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.CharPkTestEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextCharPKTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("CHAR_FK_TEST");
        dbHelper.deleteAll("CHAR_PK_TEST");
    }

    public void testInsert() throws Exception {
        CharPkTestEntity object = context.newObject(CharPkTestEntity.class);
        object.setOtherCol("object-XYZ");
        object.setPkCol("PK1");
        context.commitChanges();

        SQLTemplate q = new SQLTemplate(
                CharPkTestEntity.class,
                "SELECT * FROM CHAR_PK_TEST");

        q.setFetchingDataRows(true);

        List<?> rows = context.performQuery(q);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = (DataRow) rows.get(0);

        Object val = row.get("OTHER_COL");
        if (val == null) {
            val = row.get("other_col");
        }
        assertEquals("object-XYZ", val);

        val = row.get("PK_COL");
        if (val == null) {
            val = row.get("pk_col");
        }
        assertEquals("PK1", val);
    }

    public void testDelete() throws Exception {
        CharPkTestEntity object = context.newObject(CharPkTestEntity.class);
        object.setOtherCol("object-XYZ");
        object.setPkCol("PK1");
        context.commitChanges();

        context.deleteObjects(object);
        context.commitChanges();

        SQLTemplate q = new SQLTemplate(
                CharPkTestEntity.class,
                "SELECT * FROM CHAR_PK_TEST");
        q.setFetchingDataRows(true);

        List<?> rows = context.performQuery(q);
        assertNotNull(rows);
        assertEquals(0, rows.size());
    }

    public void testUpdate() throws Exception {
        CharPkTestEntity object = context.newObject(CharPkTestEntity.class);
        object.setOtherCol("object-XYZ");
        object.setPkCol("PK1");
        context.commitChanges();

        object.setOtherCol("UPDATED");
        context.commitChanges();

        SQLTemplate q = new SQLTemplate(
                CharPkTestEntity.class,
                "SELECT * FROM CHAR_PK_TEST");
        q.setFetchingDataRows(true);

        List<?> rows = context.performQuery(q);
        assertNotNull(rows);
        assertEquals(1, rows.size());
        DataRow row = (DataRow) rows.get(0);
        Object val = row.get("OTHER_COL");
        if (val == null) {
            val = row.get("other_col");
        }
        assertEquals("UPDATED", val);
    }
}
