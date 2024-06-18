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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.numeric_types.BigIntegerEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.NUMERIC_TYPES_PROJECT)
public class EJBQLQueryNumericIT extends RuntimeCase {

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

    protected void createBigIntegerEntitiesDataSet() throws Exception {
        tBigIntegerEntity.insert(44001, 744073709551715L);
    }

    @Test
    public void testLongParameter() throws Exception {
        createBigIntegerEntitiesDataSet();
        String ejbql = "SELECT bie FROM BigIntegerEntity bie WHERE bie.bigIntegerField > ?1";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter(1,744073709551615L);
        List<BigIntegerEntity> result = context.performQuery(query);
        assertEquals(1, result.size());
    }

    @Test
    public void testLongLiteral() throws Exception {
        createBigIntegerEntitiesDataSet();
        String ejbql = "SELECT bie FROM BigIntegerEntity bie WHERE bie.bigIntegerField > 744073709551615";
        EJBQLQuery query = new EJBQLQuery(ejbql);
        List<BigIntegerEntity> result = context.performQuery(query);
        assertEquals(1, result.size());
    }
}
