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

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class NestedDataContextLocalCacheIT extends RuntimeCase {

    @Inject
    protected CayenneRuntime runtime;

    @Inject
    private DataContext context;

    @Test
    public void testLocalCacheStaysLocal() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        ObjectContext child1 = runtime.newContext(context);

        assertNull(((DataContext) child1).getQueryCache().get(
                query.getMetaData(child1.getEntityResolver())));

        assertNull(context.getQueryCache().get(
                query.getMetaData(context.getEntityResolver())));

        List<?> results = child1.performQuery(query);
        assertSame(results, ((DataContext) child1).getQueryCache().get(
                query.getMetaData(child1.getEntityResolver())));

        assertNull(context.getQueryCache().get(
                query.getMetaData(context.getEntityResolver())));
    }
}
