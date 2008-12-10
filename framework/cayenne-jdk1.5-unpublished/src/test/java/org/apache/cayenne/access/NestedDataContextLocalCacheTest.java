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
import org.apache.cayenne.BaseContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class NestedDataContextLocalCacheTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testLocalCacheStaysLocal() {

        SelectQuery query = new SelectQuery(Artist.class);
        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        DataContext context = createDataContext();
        ObjectContext child1 = context.createChildContext();

        assertNull(((BaseContext) child1).getQueryCache().get(
                query.getMetaData(child1.getEntityResolver())));

        assertNull(context.getQueryCache().get(
                query.getMetaData(context.getEntityResolver())));

        List<?> results = child1.performQuery(query);
        assertSame(results, ((BaseContext) child1).getQueryCache().get(
                query.getMetaData(child1.getEntityResolver())));

        assertNull(context.getQueryCache().get(
                query.getMetaData(context.getEntityResolver())));
    }
}
