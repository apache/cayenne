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

public class DataContextEJBQLIsNullTest extends CayenneCase {

    public void testCompareToNull() throws Exception {
        // the query below can blow up on FrontBase. See CAY-819 for details.

        if (!getAccessStackAdapter().supportsEqualNullSyntax()) {
            return;
        }

        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice = :x";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

        // unlike SelectQuery or SQLTemplate, EJBQL nulls are handled just like SQL.

        // note that some databases (notable Sybase) actually allow = NULL comparison,
        // most do not; per JPA spec the result is undefined.. so we can't make any
        // assertions about the result. Just making sure the query doesn't blow up
        createDataContext().performQuery(query1);
    }
    
    public void testCompareToNull2() throws Exception {
        if (!getAccessStackAdapter().supportsEqualNullSyntax()) {
            return;
        }

        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist.artistName = :x";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

         createDataContext().performQuery(query1);
    }
    
    public void testCompareToNull3() throws Exception {
        if (!getAccessStackAdapter().supportsEqualNullSyntax()) {
            return;
        }

        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p WHERE :x = p.toArtist.artistName";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);
        query1.setParameter("x", null);

         createDataContext().performQuery(query1);
    }

    
    public void testIsNull() throws Exception {
        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice IS NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results = createDataContext().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results.get(0)));
    }

    public void testIsNotNull() throws Exception {
        deleteTestData();
        createTestData("prepare");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.estimatedPrice IS NOT NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results = createDataContext().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33002, DataObjectUtils.intPKForObject((Persistent) results.get(0)));
    }

    public void testToOneIsNull() throws Exception {
        deleteTestData();
        createTestData("testToOneIsNull");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist IS NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results = createDataContext().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33001, DataObjectUtils.intPKForObject((Persistent) results.get(0)));
    }

    public void testToOneIsNotNull() throws Exception {
        deleteTestData();
        createTestData("testToOneIsNull");

        String ejbql1 = "SELECT p FROM Painting p WHERE p.toArtist IS NOT NULL";
        EJBQLQuery query1 = new EJBQLQuery(ejbql1);

        List results = createDataContext().performQuery(query1);
        assertEquals(1, results.size());
        assertEquals(33003, DataObjectUtils.intPKForObject((Persistent) results.get(0)));
    }
}
