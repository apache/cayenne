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

import java.util.List;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLOrderByTest extends CayenneCase {

    public void testOrderByDefault() throws Exception {
        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results1 = createDataContext().performQuery(query1);
        assertEquals(3, results1.size());

        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results1.get(2)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.estimatedPrice";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List results2 = createDataContext().performQuery(query2);
        assertEquals(3, results2.size());

        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results2.get(1)));
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results2.get(2)));
    }

    public void testOrderByAsc() throws Exception {
        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle ASC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results1 = createDataContext().performQuery(query1);
        assertEquals(3, results1.size());

        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results1.get(2)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.estimatedPrice ASC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List results2 = createDataContext().performQuery(query2);
        assertEquals(3, results2.size());

        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results2.get(1)));
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results2.get(2)));
    }

    public void testOrderByDesc() throws Exception {
        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle DESC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results1 = createDataContext().performQuery(query1);
        assertEquals(3, results1.size());

        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results1.get(2)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.estimatedPrice DESC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List results2 = createDataContext().performQuery(query2);
        assertEquals(3, results2.size());

        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results2.get(1)));
        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results2.get(2)));
    }

    public void testOrderByQualified() throws Exception {
        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice > 1000 ORDER BY p.paintingTitle ASC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results1 = createDataContext().performQuery(query1);
        assertEquals(2, results1.size());

        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results1.get(1)));

        String ejbql2 = "SELECT p FROM Painting p WHERE p.estimatedPrice > 1000 ORDER BY p.estimatedPrice ASC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List results2 = createDataContext().performQuery(query2);
        assertEquals(2, results2.size());

        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results2.get(1)));
    }

    public void testOrderByMultiple() throws Exception {
        deleteTestData();
        createTestData("testOrderByMultiple");

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.paintingTitle DESC, p.estimatedPrice DESC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results1 = createDataContext().performQuery(query1);
        assertEquals(4, results1.size());

        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33004, DataObjectUtils.intPKForObject((Persistent) results1.get(1)));
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results1.get(2)));
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results1.get(3)));
    }
    
    public void testOrderByPath() throws Exception {
        deleteTestData();
        createTestData("testOrderByPath");

        String ejbql1 = "SELECT p FROM Painting p ORDER BY p.toArtist.artistName ASC";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results1 = createDataContext().performQuery(query1);
        assertEquals(2, results1.size());

        assertEquals(33005, DataObjectUtils.intPKForObject((Persistent) results1.get(0)));
        assertEquals(33006, DataObjectUtils.intPKForObject((Persistent) results1.get(1)));

        String ejbql2 = "SELECT p FROM Painting p ORDER BY p.toArtist.artistName DESC";
        EJBQLQuery query2 = new EJBQLQuery(ejbql2);

        List results2 = createDataContext().performQuery(query2);
        assertEquals(2, results2.size());

        assertEquals(33006, DataObjectUtils.intPKForObject((Persistent) results2.get(0)));
        assertEquals(33005, DataObjectUtils.intPKForObject((Persistent) results2.get(1)));
    }
}
