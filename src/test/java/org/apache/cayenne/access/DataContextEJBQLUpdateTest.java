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

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLUpdateTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testUpdateNoQualifier() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.paintingTitle is NULL or p.paintingTitle <> 'XX'");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }
    
    public void testUpdateNoQualifierMultipleItems() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is NULL or p.estimatedPrice <> 1");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.paintingTitle = 'XX', p.estimatedPrice = 1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }
    
    public void testUpdateNoQualifierDecimal() throws Exception {
        createTestData("prepare");

        ObjectContext context = createDataContext();

        EJBQLQuery check = new EJBQLQuery("select count(p) from Painting p "
                + "WHERE p.estimatedPrice is NULL or p.estimatedPrice <> 1.1");

        Object notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(2l), notUpdated);

        String ejbql = "UPDATE Painting AS p SET p.estimatedPrice = 1.1";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = context.performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);

        notUpdated = DataObjectUtils.objectForQuery(context, check);
        assertEquals(new Long(0l), notUpdated);
    }
}
