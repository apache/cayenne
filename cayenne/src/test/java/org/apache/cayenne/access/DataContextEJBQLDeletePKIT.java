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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.MEANINGFUL_PK_PROJECT)
public class DataContextEJBQLDeletePKIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tMeaningfulPKTest1Table;

    @Before
    public void setUp() throws Exception {
        tMeaningfulPKTest1Table = new TableHelper(dbHelper, "MEANINGFUL_PK_TEST1");
        tMeaningfulPKTest1Table.setColumns("PK_ATTRIBUTE", "DESCR", "INT_ATTRIBUTE");
    }

    protected void createMeaningfulPKDataSet() throws Exception {
        for (int i = 1; i <= 33; i++) {
            tMeaningfulPKTest1Table.insert(i, "a" + i, 0);
        }
    }

    @Test
    public void testDeleteIdVar() throws Exception {

        createMeaningfulPKDataSet();

        EJBQLQuery q = new EJBQLQuery("select m.pkAttribute from MeaningfulPKTest1 m");

        @SuppressWarnings("unchecked")
        List<Integer> id = (List<Integer>)context.performQuery(q);

        String ejbql = "delete from MeaningfulPKTest1 m WHERE m.pkAttribute in (:id)";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        query.setParameter("id", id);
        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(33, count[0]);
    }
}
