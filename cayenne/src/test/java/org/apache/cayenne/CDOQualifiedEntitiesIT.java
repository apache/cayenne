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
package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.qualified.Qualified1;
import org.apache.cayenne.testdo.qualified.Qualified2;
import org.apache.cayenne.testdo.qualified.Qualified3;
import org.apache.cayenne.testdo.qualified.Qualified4;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Types;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.QUALIFIED_PROJECT)
public class CDOQualifiedEntitiesIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter accessStackAdapter;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tQualified1;
    private TableHelper tQualified2;
    private TableHelper tQualified3;
    private TableHelper tQualified4;

    @Before
    public void setUp() throws Exception {
        int bool = accessStackAdapter.supportsBoolean() ? Types.BOOLEAN : Types.INTEGER;

        tQualified1 = new TableHelper(dbHelper, "TEST_QUALIFIED1")
                .setColumns("ID", "NAME", "DELETED")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, bool);

        tQualified2 = new TableHelper(dbHelper, "TEST_QUALIFIED2")
                .setColumns("ID", "NAME", "DELETED", "QUALIFIED1_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, bool, Types.INTEGER);

        tQualified3 = new TableHelper(dbHelper, "TEST_QUALIFIED3")
                .setColumns("ID", "NAME", "DELETED")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, bool);

        tQualified4 = new TableHelper(dbHelper, "TEST_QUALIFIED4")
                .setColumns("ID", "NAME", "DELETED", "QUALIFIED3_ID")
                .setColumnTypes(Types.INTEGER, Types.VARCHAR, bool, Types.INTEGER);
    }

    private void createReadToManyDataSet() throws Exception {
        
        tQualified1.insert(1, "OX1", null);
        tQualified1.insert(2, "OX2", accessStackAdapter.supportsBoolean() ? true : 1);

        tQualified2.insert(1, "OY1", null, 1);
        tQualified2.insert(2, "OY2", accessStackAdapter.supportsBoolean() ? true : 1, 1);
        tQualified2.insert(3, "OY3", null, 2);
        tQualified2.insert(4, "OY4", accessStackAdapter.supportsBoolean() ? true : 1, 2);
    }

    private void createReadToOneDataSet() throws Exception {
        tQualified1.insert(1, "OX1", null);
        tQualified1.insert(2, "OX2", accessStackAdapter.supportsBoolean() ? true : 1);

        tQualified2.insert(1, "OY1", null, 2);
    }

    private void createJoinDataSet() throws Exception {
        tQualified3.insert(1, "O1", null);
        tQualified3.insert(2, "O2", accessStackAdapter.supportsBoolean() ? true : 1);

        tQualified4.insert(1, "SHOULD_SELECT", null, 1);
        tQualified4.insert(2, "SHOULD_NOT_SELECT", null, 2);
    }

    @Test
    public void testReadToMany() throws Exception {
        if (accessStackAdapter.supportsNullBoolean()) {

            createReadToManyDataSet();

            List<Qualified1> roots = ObjectSelect.query(Qualified1.class).select(context);

            assertEquals(1, roots.size());

            Qualified1 root = roots.get(0);

            assertEquals("OX1", root.getName());

            List<Qualified2> related = root.getQualified2s();
            assertEquals(1, related.size());

            Qualified2 r = related.get(0);
            assertEquals("OY1", r.getName());
        }
    }

    @Test
    public void testJointPrefetchToMany() throws Exception {
        if (accessStackAdapter.supportsNullBoolean()) {

            createReadToManyDataSet();

            List<Qualified1> roots = ObjectSelect.query(Qualified1.class)
                    .prefetch(Qualified1.QUALIFIED2S.joint())
                    .select(context);

            assertEquals(1, roots.size());

            Qualified1 root = roots.get(0);

            assertEquals("OX1", root.getName());

            List<Qualified2> related = root.getQualified2s();
            assertEquals(1, related.size());

            Qualified2 r = related.get(0);
            assertEquals("OY1", r.getName());
        }
    }

    @Test
    public void testDisjointPrefetchToMany() throws Exception {
        if (accessStackAdapter.supportsNullBoolean()) {

            createReadToManyDataSet();

            List<Qualified1> roots = ObjectSelect.query(Qualified1.class)
                    .prefetch(Qualified1.QUALIFIED2S.disjoint())
                    .select(context);

            assertEquals(1, roots.size());

            Qualified1 root = roots.get(0);

            assertEquals("OX1", root.getName());

            List<Qualified2> related = root.getQualified2s();
            assertEquals(1, related.size());

            Qualified2 r    = related.get(0);
            assertEquals("OY1", r.getName());
        }
    }

    @Test
    public void testDisjointByIdPrefetchToMany() throws Exception {
        if (accessStackAdapter.supportsNullBoolean()) {

            createReadToManyDataSet();

            List<Qualified1> roots = ObjectSelect.query(Qualified1.class)
                    .prefetch(Qualified1.QUALIFIED2S.disjointById())
                    .select(context);

            assertEquals(1, roots.size());

            Qualified1 root = roots.get(0);

            assertEquals("OX1", root.getName());

            List<Qualified2> related = root.getQualified2s();
            assertEquals(1, related.size());

            Qualified2 r = related.get(0);
            assertEquals("OY1", r.getName());
        }
    }

    @Test
    public void testReadToOne() throws Exception {
        if (accessStackAdapter.supportsNullBoolean()) {

            createReadToOneDataSet();

            List<Qualified2> roots = ObjectSelect.query(Qualified2.class).select(context);
            assertEquals(1, roots.size());

            Qualified2 root = roots.get(0);
            assertEquals("OY1", root.getName());

            Qualified1 target = root.getQualified1();
            assertNull(target);
        }
    }

    @Test
    public void joinWithQualifier() throws Exception {
        createJoinDataSet();

        List<Qualified4> result = ObjectSelect.query(Qualified4.class)
                .where(Qualified4.QUALIFIED3.dot(Qualified3.NAME).like("O%"))
                .select(context);

        assertEquals(1, result.size());
        assertEquals("SHOULD_SELECT", result.get(0).getName());
    }

    @Test
    public void joinWithCustomDbQualifier() throws Exception {
        createJoinDataSet();

        DbEntity entity1 = context.getEntityResolver().getDbEntity("TEST_QUALIFIED3");
        DbEntity entity2 = context.getEntityResolver().getDbEntity("TEST_QUALIFIED4");
        Expression oldExpression1 = entity1.getQualifier();
        Expression oldExpression2 = entity2.getQualifier();
        try {
            entity1.setQualifier(ExpressionFactory.matchDbExp("DELETED", null));
            entity2.setQualifier(ExpressionFactory.matchDbExp("DELETED", null));

            List<Qualified4> result = ObjectSelect.query(Qualified4.class)
                    .where(Qualified4.QUALIFIED3.dot(Qualified3.NAME).like("O%"))
                    .select(context);

            assertEquals(1, result.size());
            assertEquals("SHOULD_SELECT", result.get(0).getName());
        } finally {
            entity1.setQualifier(oldExpression1);
            entity2.setQualifier(oldExpression2);
        }
    }
}
