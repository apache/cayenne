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

import org.apache.art.Artist;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLArrayResultTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testSQLResultSetMappingScalar() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT count(p) FROM Painting p JOIN p.toArtist a";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Object o1 = objects.get(0);
        assertEquals(new Long(2), o1);
    }

    public void testSQLResultSetMappingScalars() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT count(p), sum(p.estimatedPrice) FROM Painting p JOIN p.toArtist a";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List objects = createDataContext().performQuery(query);
        assertEquals(1, objects.size());

        Object o1 = objects.get(0);
        assertTrue("Expected Object[]: " + o1, o1 instanceof Object[]);
        Object[] array1 = (Object[]) o1;
        assertEquals(2, array1.length);

        assertEquals(new Long(2), array1[0]);
        assertEquals(0, new BigDecimal(8000).compareTo((BigDecimal) array1[1]));
    }

    public void testSQLResultSetMappingMixed() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT count(p), a, sum(p.estimatedPrice) "
                + "FROM Artist a LEFT JOIN a.paintingArray p "
                + "GROUP BY a ORDER BY a.artistName";

        EJBQLQuery query = new EJBQLQuery(ejbql);

        List objects = createDataContext().performQuery(query);
        assertEquals(4, objects.size());

        Object o1 = objects.get(0);
        assertTrue("Expected Object[]: " + o1, o1 instanceof Object[]);
        Object[] array1 = (Object[]) o1;
        assertEquals(3, array1.length);

        assertEquals(new Long(1), array1[0]);
        assertTrue("Expected Artist, got: " + array1[1], array1[1] instanceof Artist);
        assertEquals(0, new BigDecimal(3000).compareTo((BigDecimal) array1[2]));
    }
}
