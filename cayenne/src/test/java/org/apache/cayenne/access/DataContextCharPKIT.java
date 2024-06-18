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

package org.apache.cayenne.access;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.compound.CharPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class DataContextCharPKIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
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

    @Test
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

    @Test
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
