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
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DataContextEJBQLUpdateCompoundIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.COMPOUND_PROJECT);

    private TableHelper tCompoundPk;
    private TableHelper tCompoundFk;

    
    @BeforeEach
    public void setUp() throws Exception {
        tCompoundPk = env.table("COMPOUND_PK_TEST", "KEY1", "KEY2");

        tCompoundFk = env.table("COMPOUND_FK_TEST", "PKEY", "F_KEY1", "F_KEY2");
    }

    private void createTwoCompoundPKTwoFK() throws Exception {
        tCompoundPk.insert("a1", "a2");
        tCompoundPk.insert("b1", "b2");
        tCompoundFk.insert(33001, "a1", "a2");
        tCompoundFk.insert(33002, "b1", "b2");
    }

    @Test
    public void updateNoQualifierToOneCompoundPK() throws Exception {
        createTwoCompoundPKTwoFK();

        Map<String, String> key1 = new HashMap<>();
        key1.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "b1");
        key1.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "b2");
        CompoundPkTestEntity object = Cayenne.objectForPK(
                env.context(),
                CompoundPkTestEntity.class,
                key1);

        EJBQLQuery check = new EJBQLQuery(
                "select count(e) from CompoundFkTestEntity e WHERE e.toCompoundPk <> :param");
        check.setParameter("param", object);

        Object notUpdated = Cayenne.objectForQuery(env.context(), check);
        assertEquals(1L, notUpdated);

        String ejbql = "UPDATE CompoundFkTestEntity e SET e.toCompoundPk = :param";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("param", object);

        QueryResponse result = env.context().performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = Cayenne.objectForQuery(env.context(), check);
        assertEquals(0L, notUpdated);
    }

}
