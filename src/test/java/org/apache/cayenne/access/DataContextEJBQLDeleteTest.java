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

import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextEJBQLDeleteTest extends CayenneCase {
    
    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testDeleteNoQualifier() throws Exception {
        createTestData("prepare");

        String ejbql = "delete from Painting AS p";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = createDataContext().performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(2, count[0]);
    }

    public void testDeleteSameEntityQualifier() throws Exception {
        createTestData("prepare");

        String ejbql = "delete from Painting AS p WHERE p.paintingTitle = 'P2'";
        EJBQLQuery query = new EJBQLQuery(ejbql);

        QueryResponse result = createDataContext().performGenericQuery(query);

        int[] count = result.firstUpdateCount();
        assertNotNull(count);
        assertEquals(1, count.length);
        assertEquals(1, count[0]);

        assertNotNull(DataObjectUtils
                .objectForPK(createDataContext(), Painting.class, 33001));
        assertNull(DataObjectUtils
                .objectForPK(createDataContext(), Painting.class, 33002));
    }
}
