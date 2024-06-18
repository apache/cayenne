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

import java.io.Serializable;
import java.util.List;

import org.apache.cayenne.cache.MockQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataDomainQueryActionIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private CayenneRuntime runtime;

    @After
    public void tearDown() {
        runtime.getDataDomain().resetProperties();
    }

    @Test
    public void testCachedQuery() {

        DataDomain domain = runtime.getDataDomain();

        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("sample");

        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_GALLERY.disjoint())
                .prefetch(Painting.TO_ARTIST.disjoint())
                .orderBy(Painting.PAINTING_TITLE.asc())
                .cacheStrategy(QueryCacheStrategy.SHARED_CACHE)
                .pageSize(5);

        QueryCache cache = domain.queryCache;

        domain.queryCache = new MockQueryCache() {

            @Override
            public List<?> get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
                Object results = factory.createObject();
                assertTrue(
                        "Query cache is not serializable.",
                        results instanceof Serializable);

                return null;
            }

            @SuppressWarnings("all")
            @Override
            public void put(QueryMetadata metadata, List results) {
                assertTrue(
                        "Query cache is not serializable.",
                        results instanceof Serializable);
            }
        };

        DataDomainQueryAction action = new DataDomainQueryAction(context, domain, query);
        action.execute();

        domain.queryCache = cache;
    }

}
