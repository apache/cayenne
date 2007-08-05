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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLSubqueryTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testExists() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p FROM Painting p"
                + " WHERE EXISTS ("
                + " SELECT DISTINCT p1 FROM Painting p1"
                + " WHERE p1.paintingTitle = p.paintingTitle"
                + " AND p.estimatedPrice <> p1.estimatedPrice"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33001)));
        assertTrue(ids.contains(new Integer(33003)));
    }
    
    public void testAll() throws Exception {
        createTestData("prepare");

        String ejbql = "SELECT p FROM Painting p"
                + " WHERE p.estimatedPrice > ALL ("
                + " SELECT p1.estimatedPrice FROM Painting p1"
                + " WHERE p1.paintingTitle = 'P2'"
                + ")";

        EJBQLQuery query = new EJBQLQuery(ejbql);
        List objects = createDataContext().performQuery(query);
        assertEquals(2, objects.size());

        Set ids = new HashSet();
        Iterator it = objects.iterator();
        while (it.hasNext()) {
            Object id = DataObjectUtils.pkForObject((Persistent) it.next());
            ids.add(id);
        }

        assertTrue(ids.contains(new Integer(33003)));
        assertTrue(ids.contains(new Integer(33004)));
    }
}
