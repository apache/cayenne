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

import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.enum_test.Enum1;
import org.apache.cayenne.testdo.enum_test.EnumEntity;
import org.apache.cayenne.testdo.enum_test.EnumEntity2;
import org.apache.cayenne.testdo.enum_test.EnumEntity3;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.ENUM_PROJECT)
public class EnumIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private void createDataSet() throws Exception {
        TableHelper tEnumEntity = new TableHelper(dbHelper, "ENUM_ENTITY");
        tEnumEntity.setColumns("ID", "ENUM_ATTRIBUTE");

        tEnumEntity.insert(1, "two");
        tEnumEntity.insert(2, "one");
    }

    @Test
    public void testInsert() {
        EnumEntity e = context.newObject(EnumEntity.class);
        e.setEnumAttribute(Enum1.one);
        context.commitChanges();
    }

    @Test
    public void testObjectSelect() throws Exception {
        createDataSet();

        EnumEntity e = ObjectSelect.query(EnumEntity.class)
                .where(EnumEntity.ENUM_ATTRIBUTE.eq(Enum1.one))
                .selectOne(context);

        assertNotNull(e);
        assertSame(Enum1.one, e.getEnumAttribute());
    }

    @Test
    public void testSQLTemplate() throws Exception {
        createDataSet();

        SQLTemplate q = new SQLTemplate(
                EnumEntity.class,
                "SELECT * FROM ENUM_ENTITY WHERE ENUM_ATTRIBUTE = 'one'");
        q.setColumnNamesCapitalization(CapsStrategy.UPPER);

        EnumEntity e = (EnumEntity) Cayenne.objectForQuery(context, q);
        assertNotNull(e);
        assertSame(Enum1.one, e.getEnumAttribute());
    }

    @Test
    public void createObjectWithEnumQualifier() {
        EnumEntity2 test = context.newObject(EnumEntity2.class);
        context.commitChanges();

        assertEquals(Enum1.two, test.getEnumAttribute());
    }

    @Test
    public void testEnumMappedToChar() {
        EnumEntity3 enumEntity3 = context.newObject(EnumEntity3.class);
        enumEntity3.setEnumAttribute(Enum1.two);
        context.commitChanges();

        List<EnumEntity3> enumEntity3s = ObjectSelect.query(EnumEntity3.class)
                .select(context);
        assertEquals(1, enumEntity3s.size());
        assertEquals(Enum1.two, enumEntity3s.get(0).getEnumAttribute());
    }
}
