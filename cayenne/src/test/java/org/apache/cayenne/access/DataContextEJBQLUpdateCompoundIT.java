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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class DataContextEJBQLUpdateCompoundIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tCompoundPk;
    private TableHelper tCompoundFk;

    @Before
    public void setUp() throws Exception {
        tCompoundPk = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPk.setColumns("KEY1", "KEY2");

        tCompoundFk = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFk.setColumns("PKEY", "F_KEY1", "F_KEY2");
    }

    private void createTwoCompoundPKTwoFK() throws Exception {
        tCompoundPk.insert("a1", "a2");
        tCompoundPk.insert("b1", "b2");
        tCompoundFk.insert(33001, "a1", "a2");
        tCompoundFk.insert(33002, "b1", "b2");
    }

    @Test
    public void testUpdateNoQualifierToOneCompoundPK() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity object = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                key1);

        EJBQLQuery check = new EJBQLQuery(
                "select count(e) from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param");
        check.setParameter("param", object);

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(1L, notUpdated);

        String ejbql = "UPDATE CompoundFkTestEntity e SET e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", object);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(0L, notUpdated);
    }

}
