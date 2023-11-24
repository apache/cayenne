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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.compound.CompoundFkTestEntity;
import org.apache.cayenne.testdo.compound.CompoundPkTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.COMPOUND_PROJECT)
public class DataContextSQLTemplateCompoundIT extends RuntimeCase {

    @Inject
    protected DataContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tCompoundPkTest;
    protected TableHelper tCompoundFkTest;

    @Before
    public void setUp() throws Exception {
        tCompoundPkTest = new TableHelper(dbHelper, "COMPOUND_PK_TEST");
        tCompoundPkTest.setColumns("KEY1", "KEY2");

        tCompoundFkTest = new TableHelper(dbHelper, "COMPOUND_FK_TEST");
        tCompoundFkTest.setColumns("PKEY", "F_KEY1", "F_KEY2");
    }

    protected void createTwoCompoundPKsAndCompoundFKsDataSet() throws Exception {
        tCompoundPkTest.insert("a1", "a2");
        tCompoundPkTest.insert("b1", "b2");

        tCompoundFkTest.insert(6, "a1", "a2");
        tCompoundFkTest.insert(7, "b1", "b2");
    }

    @Test
    public void testBindObjectEqualCompound() throws Exception {
        createTwoCompoundPKsAndCompoundFKsDataSet();

        Map<String, String> pk = new HashMap<>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParams(Collections.singletonMap("a", a));

        List<CompoundFkTestEntity> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = objects.get(0);
        assertEquals(6, Cayenne.intPKForObject(p));
    }

    @Test
    public void testBindObjectNotEqualCompound() throws Exception {
        createTwoCompoundPKsAndCompoundFKsDataSet();

        Map<String, String> pk = new HashMap<>();
        pk.put(CompoundPkTestEntity.KEY1_PK_COLUMN, "a1");
        pk.put(CompoundPkTestEntity.KEY2_PK_COLUMN, "a2");

        CompoundPkTestEntity a = Cayenne.objectForPK(
                context,
                CompoundPkTestEntity.class,
                pk);

        String template = "SELECT * FROM COMPOUND_FK_TEST t0"
                + " WHERE #bindObjectNotEqual($a [ 't0.F_KEY1', 't0.F_KEY2' ] [ 'KEY1', 'KEY2' ] ) ORDER BY PKEY";
        SQLTemplate query = new SQLTemplate(CompoundFkTestEntity.class, template);
        query.setColumnNamesCapitalization(CapsStrategy.UPPER);
        query.setParams(Collections.singletonMap("a", a));

        List<CompoundFkTestEntity> objects = context.performQuery(query);
        assertEquals(1, objects.size());

        CompoundFkTestEntity p = objects.get(0);
        assertEquals(7, Cayenne.intPKForObject(p));
    }
}
