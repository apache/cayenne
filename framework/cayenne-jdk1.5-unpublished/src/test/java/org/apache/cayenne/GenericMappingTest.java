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

package org.apache.cayenne;

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.DEFAULT_PROJECT)
public class GenericMappingTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("GENERIC2");
        dbHelper.deleteAll("GENERIC1");
    }

    public void testInsertSingle() {
        DataObject g1 = (DataObject) context.newObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        context.commitChanges();
    }

    public void testInsertRelated() {
        DataObject g1 = (DataObject) context.newObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        DataObject g2 = (DataObject) context.newObject("Generic2");
        g2.writeProperty("name", "G2 Name");
        g2.setToOneTarget("toGeneric1", g1, true);

        context.commitChanges();
    }

    public void testSelect() {
        context.performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC1 (ID, NAME) VALUES (1, 'AAAA')"));
        context.performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC1 (ID, NAME) VALUES (2, 'BBBB')"));
        context.performNonSelectingQuery(new SQLTemplate(
                "Generic1",
                "INSERT INTO GENERIC2 (GENERIC1_ID, ID, NAME) VALUES (1, 1, 'CCCCC')"));

        Expression qual = ExpressionFactory.matchExp("name", "AAAA");
        SelectQuery q = new SelectQuery("Generic1", qual);

        List<?> result = context.performQuery(q);
        assertEquals(1, result.size());
    }

    public void testUpdateRelated() {
        DataObject g1 = (DataObject) context.newObject("Generic1");
        g1.writeProperty("name", "G1 Name");

        DataObject g2 = (DataObject) context.newObject("Generic2");
        g2.writeProperty("name", "G2 Name");
        g2.setToOneTarget("toGeneric1", g1, true);

        context.commitChanges();

        List<?> r1 = (List<?>) g1.readProperty("generic2s");
        assertTrue(r1.contains(g2));

        DataObject g11 = (DataObject) context.newObject("Generic1");
        g11.writeProperty("name", "G11 Name");
        g2.setToOneTarget("toGeneric1", g11, true);

        context.commitChanges();

        List<?> r11 = (List<?>) g11.readProperty("generic2s");
        assertTrue(r11.contains(g2));

        List<?> r1_1 = (List<?>) g1.readProperty("generic2s");
        assertFalse(r1_1.contains(g2));
    }
}
