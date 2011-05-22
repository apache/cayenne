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

package org.apache.cayenne.unit.jira;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.ReflexiveAndToOne;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * Testing qualifier translator correctness on reflexive relationships.
 */
// TODO: this is really a qualifier translator general test... need to
// find an appropriate place in unit tests..
@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class CAY_194Test extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        TableHelper tReflexive = new TableHelper(dbHelper, "REFLEXIVE_AND_TO_ONE");
        tReflexive.setColumns("REFLEXIVE_AND_TO_ONE_ID", "PARENT_ID");

        tReflexive.update().set("PARENT_ID", null, Types.INTEGER).execute();

        dbHelper.deleteAll("REFLEXIVE_AND_TO_ONE");
        dbHelper.deleteAll("TO_ONEFK1");
    }

    public void testQualifyOnToMany() {

        ReflexiveAndToOne ox = context.newObject(ReflexiveAndToOne.class);
        ox.setName("ox");
        ReflexiveAndToOne o1 = context.newObject(ReflexiveAndToOne.class);
        o1.setName("o1");

        ReflexiveAndToOne o2 = context.newObject(ReflexiveAndToOne.class);
        o2.setName("o2");
        o2.setToParent(o1);

        context.commitChanges();

        Expression qualifier = ExpressionFactory.matchExp("children", o2);
        List<?> parents = context.performQuery(new SelectQuery(
                ReflexiveAndToOne.class,
                qualifier));
        assertEquals(1, parents.size());
        assertSame(o1, parents.get(0));

        qualifier = ExpressionFactory.matchExp("children", o1);
        parents = context
                .performQuery(new SelectQuery(ReflexiveAndToOne.class, qualifier));
        assertEquals(0, parents.size());
    }

    public void testQualifyOnToOne() {

        ReflexiveAndToOne ox = context.newObject(ReflexiveAndToOne.class);
        ox.setName("ox");
        ReflexiveAndToOne o1 = context.newObject(ReflexiveAndToOne.class);
        o1.setName("o1");

        ReflexiveAndToOne o2 = context.newObject(ReflexiveAndToOne.class);
        o2.setName("o2");
        o2.setToParent(o1);

        context.commitChanges();

        Expression qualifier = ExpressionFactory.matchExp("toParent", o1);
        List<?> children = context.performQuery(new SelectQuery(
                ReflexiveAndToOne.class,
                qualifier));
        assertEquals(1, children.size());
        assertSame(o2, children.get(0));
    }
}
