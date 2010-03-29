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

import org.apache.art.Artist;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextPaginatedQueryTest extends CayenneCase {

    public void testLocalCache() throws Exception {
        deleteTestData();
        createTestData("testLocalCache");

        DataContext context = createDataContext();

        SelectQuery query = new SelectQuery(Artist.class);
        query.addOrdering(Artist.ARTIST_NAME_PROPERTY, Ordering.ASC);
        query.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        query.setPageSize(5);

        List<?> results1 = context.performQuery(query);
        assertNotNull(results1);

        List<?> results2 = context.performQuery(query);
        assertNotNull(results2);
        assertSame(results1, results2);
        
        results1.get(1);
        List<?> results3 = context.performQuery(query);
        assertNotNull(results3);
        assertSame(results1, results3);
        
        results1.get(7);
        List<?> results4 = context.performQuery(query);
        assertNotNull(results4);
        assertSame(results1, results4);
    }
}
