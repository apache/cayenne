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

package org.apache.cayenne.query;

import java.util.List;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test join on compound primary key
 * @see <a href="https://issues.apache.org/jira/browse/CAY-2137">CAY-2137</a>
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class QueryWithCompoundJoinIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private CayenneRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tCompoundPk;
    private TableHelper tCompoundFk;

    @Before
    public void setUp() throws Exception {
        tCompoundPk = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPk.setColumns("KEY1", "KEY2", "NAME");

        tCompoundFk = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFk.setColumns("F_KEY1", "F_KEY2", "NAME", "PKEY");

        createDataSet();
    }

    private void createDataSet() throws Exception {
        tCompoundPk.insert("a", "b", "abc");
        tCompoundPk.insert("c", "d", "cde");

        tCompoundFk.insert("a", "b", "test", 1);
        tCompoundFk.insert("c", "d", "nottest", 2);
    }

    @Test
    public void testEJBQLCompoundJoin() throws Exception {
        EJBQLQuery query = new EJBQLQuery(
                "select f from CompoundFkTestEntity f inner join f.toCompoundPk p where p.name like 'a%'");
        List res = context.performQuery(query);
        assertEquals(1, res.size());
        assertTrue(res.get(0) instanceof CompoundFkTestEntity);
        assertEquals("test", ((CompoundFkTestEntity)res.get(0)).getName());
    }

    @Test
    public void testObjectSelectCompoundJoin() throws Exception {
        List<CompoundFkTestEntity> res = ObjectSelect.query(CompoundFkTestEntity.class)
                .where(CompoundFkTestEntity.TO_COMPOUND_PK.dot(CompoundPkTestEntity.NAME).like("a%"))
                .select(context);
        assertEquals(1, res.size());
        assertEquals("test", res.get(0).getName());
    }
}
