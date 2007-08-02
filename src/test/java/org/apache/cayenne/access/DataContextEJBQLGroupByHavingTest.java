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
package org.apache.cayenne.access;

import java.math.BigDecimal;
import java.util.List;

import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLGroupByHavingTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testGroupBy() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " ORDER BY p.estimatedPrice";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(2, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(1), row0[0]);
        assertEquals(new Long(3), row0[1]);

        Object[] row1 = (Object[]) data.get(1);
        assertEquals(new BigDecimal(2), row1[0]);
        assertEquals(new Long(2), row1[1]);
    }

    public void testGroupByMultipleItems() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, p.paintingTitle, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice, p.paintingTitle"
                + " ORDER BY p.estimatedPrice, p.paintingTitle";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(3, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(1), row0[0]);
        assertEquals("PX", row0[1]);
        assertEquals(new Long(1), row0[2]);

        Object[] row1 = (Object[]) data.get(1);
        assertEquals(new BigDecimal(1), row1[0]);
        assertEquals("PZ", row1[1]);
        assertEquals(new Long(2), row1[2]);

        Object[] row2 = (Object[]) data.get(2);
        assertEquals(new BigDecimal(2), row2[0]);
        assertEquals("PY", row2[1]);
        assertEquals(new Long(2), row2[2]);
    }

    public void testGroupByIdVariable() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT count(p), p FROM Painting p GROUP BY p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(5, data.size());

        // TODO: andrus, 8/3/2007 the rest of the unit test fails as currently Cayenne
        // does not allow mixed object and scalar results (see CAY-839)

        // assertTrue(data.get(0) instanceof Object[]);
        //
        // for(int i = 0; i < data.size(); i++) {
        // Object[] row = (Object[]) data.get(i);
        // assertEquals(new Long(1), row[0]);
        // }
    }

    public void testGroupByHavingOnColumn() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " HAVING p.estimatedPrice > 1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        List data = createDataContext().performQuery(query);
        assertEquals(1, data.size());
        assertTrue(data.get(0) instanceof Object[]);

        Object[] row0 = (Object[]) data.get(0);
        assertEquals(new BigDecimal(2), row0[0]);
        assertEquals(new Long(2), row0[1]);
    }

    public void testGroupByHavingOnAggregate() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p.estimatedPrice, count(p) FROM Painting p"
                + " GROUP BY p.estimatedPrice"
                + " HAVING count(p) > 2";
        EJBQLQuery query = new EJBQLQuery(ejbql);

//        List data = createDataContext().performQuery(query);
//        assertEquals(1, data.size());
//        assertTrue(data.get(0) instanceof Object[]);
//
//        Object[] row0 = (Object[]) data.get(0);
//        assertEquals(new BigDecimal(1), row0[0]);
//        assertEquals(new Long(3), row0[1]);
    }
}
