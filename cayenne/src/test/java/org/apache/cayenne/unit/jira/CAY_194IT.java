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

package org.apache.cayenne.unit.jira;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationships.ReflexiveAndToOne;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Testing qualifier translator correctness on reflexive relationships.
 */
// TODO: this is really a qualifier translator general test...
// need to find an appropriate place in unit tests..
public class CAY_194IT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.RELATIONSHIPS_PROJECT);

    protected DataContext context;
    private DbHelper dbHelper;

    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        dbHelper = env.dbHelper();
        TableHelper tReflexive = new TableHelper(dbHelper, "REFLEXIVE_AND_TO_ONE", "REFLEXIVE_AND_TO_ONE_ID", "PARENT_ID");

        tReflexive.update().set("PARENT_ID", null, Types.INTEGER).execute();

        dbHelper.deleteAll("REFLEXIVE_AND_TO_ONE");
    }

    @Test
    public void qualifyOnToMany() {

        ReflexiveAndToOne ox = context.newObject(ReflexiveAndToOne.class);
        ox.setName("ox");
        ReflexiveAndToOne o1 = context.newObject(ReflexiveAndToOne.class);
        o1.setName("o1");

        ReflexiveAndToOne o2 = context.newObject(ReflexiveAndToOne.class);
        o2.setName("o2");
        o2.setToParent(o1);

        context.commitChanges();

        List<?> parents = ObjectSelect.query(ReflexiveAndToOne.class, ReflexiveAndToOne.CHILDREN.contains(o2)).select(context);
        assertEquals(1, parents.size());
        assertSame(o1, parents.get(0));

        parents = ObjectSelect.query(ReflexiveAndToOne.class, ReflexiveAndToOne.CHILDREN.contains(o1)).select(context);
        assertEquals(0, parents.size());
    }

    @Test
    public void qualifyOnToOne() {

        ReflexiveAndToOne ox = context.newObject(ReflexiveAndToOne.class);
        ox.setName("ox");
        ReflexiveAndToOne o1 = context.newObject(ReflexiveAndToOne.class);
        o1.setName("o1");

        ReflexiveAndToOne o2 = context.newObject(ReflexiveAndToOne.class);
        o2.setName("o2");
        o2.setToParent(o1);

        context.commitChanges();

        List<ReflexiveAndToOne> children = ObjectSelect.query(ReflexiveAndToOne.class, ReflexiveAndToOne.TO_PARENT.eq(o1)).select(context);
        assertEquals(1, children.size());
        assertSame(o2, children.get(0));

        o2.setToParent(null);
        context.commitChanges();
    }
}
