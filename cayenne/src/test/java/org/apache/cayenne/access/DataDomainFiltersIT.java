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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelQueryFilterChain;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.ListResponse;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataDomainFiltersIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testDefaultFilters() {
        // There is a default TransactionFilter
        DataDomain domain = runtime.getDataDomain();
        assertEquals(0, domain.queryFilters.size());
        assertEquals(1, domain.syncFilters.size());
    }

    @Test
    public void testOnQuery_FilterOrdering() {

        DataDomain domain = runtime.getDataDomain();
        List<String> results = new ArrayList<>();

        DataChannelQueryFilter f1 = (originatingContext, query, filterChain) -> {
            results.add("f1start");
            QueryResponse response = filterChain.onQuery(originatingContext, query);
            results.add("f1end");
            return response;
        };

        DataChannelQueryFilter f2 = (originatingContext, query, filterChain) -> {
            results.add("f2start");
            QueryResponse response = filterChain.onQuery(originatingContext, query);
            results.add("f2end");
            return response;
        };

        domain.queryFilters.add(f1);
        domain.queryFilters.add(f2);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
        QueryResponse response = domain.onQuery(context, query);
        assertNotNull(response);
        assertEquals(4, results.size());
        assertEquals("f2start", results.get(0));
        assertEquals("f1start", results.get(1));
        assertEquals("f1end", results.get(2));
        assertEquals("f2end", results.get(3));
    }

    @Test
    public void testOnSync_FilterOrdering() {

        DataDomain domain = runtime.getDataDomain();
        List<String> results = new ArrayList<>();

        DataChannelSyncFilter f1 = (originatingContext, changes, syncType, filterChain) -> {
            results.add("f1start");
            GraphDiff response = filterChain.onSync(originatingContext, changes, syncType);
            results.add("f1end");
            return response;
        };

        DataChannelSyncFilter f2 = (originatingContext, changes, syncType, filterChain) -> {
            results.add("f2start");
            GraphDiff response = filterChain.onSync(originatingContext, changes, syncType);
            results.add("f2end");
            return response;
        };

        domain.syncFilters.add(f1);
        domain.syncFilters.add(f2);

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");

        // testing domain.onSync indirectly
        context.commitChanges();

        assertEquals(4, results.size());
        assertEquals("f2start", results.get(0));
        assertEquals("f1start", results.get(1));
        assertEquals("f1end", results.get(2));
        assertEquals("f2end", results.get(3));
    }

    @Test
    public void testOnQuery_Blocking() {

        DataDomain domain = runtime.getDataDomain();

        QueryResponse r1 = new ListResponse();
        QueryResponse r2 = new ListResponse();

        DataChannelQueryFilter f1 = (originatingContext, query, filterChain) -> r1;
        DataChannelQueryFilter f2 = (originatingContext, query, filterChain) -> r2;

        domain.queryFilters.add(f1);
        domain.queryFilters.add(f2);

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);
        QueryResponse response = domain.onQuery(context, query);

        assertSame(r2, response);
    }

    @Test
    public void testSyncAndQueryFilter() {
        ComplexFilter complexFilter = new ComplexFilter();
        DataDomain domain = runtime.getDataDomain();

        domain.addQueryFilter(complexFilter);
        domain.addSyncFilter(complexFilter);

        Artist a = context.newObject(Artist.class);
        a.setArtistName("AAA");

        // testing domain.onSync indirectly
        context.commitChanges();

        assertEquals(2, complexFilter.results.size());
        assertEquals("onSync", complexFilter.results.get(0));
        assertEquals("postPersist", complexFilter.results.get(1));
    }

    private static class ComplexFilter implements DataChannelQueryFilter, DataChannelSyncFilter {

        private List<String> results = new ArrayList<>();

        @Override
        public QueryResponse onQuery(ObjectContext originatingContext, Query query, DataChannelQueryFilterChain filterChain) {
            results.add("onQuery");
            return filterChain.onQuery(originatingContext, query);
        }

        @Override
        public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType, DataChannelSyncFilterChain filterChain) {
            results.add("onSync");
            return filterChain.onSync(originatingContext, changes, syncType);
        }

        @PostPersist
        public void postPersist(Object object) {
            results.add("postPersist");
        }
    }
}
