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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.numeric_types.BigDecimalEntity;
import org.apache.cayenne.testdo.numeric_types.BigIntegerEntity;
import org.apache.cayenne.testdo.numeric_types.BooleanTestEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.NUMERIC_TYPES_PROJECT)
public class DataContextEJBQLNumericalFunctionalIT extends RuntimeCase {

    @Inject
    protected DBHelper dbHelper;

    @Inject
    private ObjectContext context;

    private TableHelper tBigIntegerEntity;

    @Before
    public void setUp() throws Exception {
        tBigIntegerEntity = new TableHelper(dbHelper, "BIGINTEGER_ENTITY");
        tBigIntegerEntity.setColumns("ID", "BIG_INTEGER_FIELD");
    }

    @Test
    public void testABS() {

        BigDecimalEntity o1 = context.newObject(BigDecimalEntity.class);
        o1.setBigDecimalNumeric(new BigDecimal("4.1"));

        BigDecimalEntity o2 = context.newObject(BigDecimalEntity.class);
        o2.setBigDecimalNumeric(new BigDecimal("-5.1"));

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM BigDecimalEntity d WHERE ABS(d.bigDecimalNumeric) < 5.0");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }

    @Test
    public void testSQRT() {

        BigDecimalEntity o1 = context.newObject(BigDecimalEntity.class);
        o1.setBigDecimalNumeric(new BigDecimal("9"));

        BigDecimalEntity o2 = context.newObject(BigDecimalEntity.class);
        o2.setBigDecimalNumeric(new BigDecimal("16"));

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM BigDecimalEntity d WHERE SQRT(d.bigDecimalNumeric) > 3.1");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    @Test
    public void testMOD() {

        BigIntegerEntity o1 = context.newObject(BigIntegerEntity.class);
        o1.setBigIntegerField(new BigInteger("9"));

        BigIntegerEntity o2 = context.newObject(BigIntegerEntity.class);
        o2.setBigIntegerField(new BigInteger("10"));

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM BigIntegerEntity d WHERE MOD(d.bigIntegerField, 4) = 2");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    @Test
    public void testUpdateNoQualifierBoolean() throws Exception {

        BooleanTestEntity o1 = context.newObject(BooleanTestEntity.class);
        o1.setBooleanColumn(Boolean.TRUE);

        BooleanTestEntity o2 = context.newObject(BooleanTestEntity.class);
        o2.setBooleanColumn(Boolean.FALSE);

        BooleanTestEntity o3 = context.newObject(BooleanTestEntity.class);
        o3.setBooleanColumn(Boolean.FALSE);

        context.commitChanges();

        EJBQLQuery check = new EJBQLQuery("select count(p) from BooleanTestEntity p "
                + "WHERE p.booleanColumn = true");

        Object notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(1L, notUpdated);

        String ejbql = "UPDATE BooleanTestEntity AS p SET p.booleanColumn = true";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(3, count[0]);

        notUpdated = Cayenne.objectForQuery(context, check);
        assertEquals(3L, notUpdated);
    }
}
