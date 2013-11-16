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

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.DataChannelFilter;
import org.apache.cayenne.DataChannelFilterChain;
import org.apache.cayenne.MockDataChannelFilter;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.ListResponse;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataDomainFiltersTest extends ServerCase {

    @Inject
    private ObjectContext context;

    @Inject
    private ServerRuntime runtime;

    public void testDefaultNoFilters() {

        DataDomain domain = runtime.getDataDomain();
        assertEquals(0, domain.filters.size());
    }

    public void testOnQuery_FilterOrdering() {

        DataDomain domain = runtime.getDataDomain();

        final List<String> results = new ArrayList<String>();

        DataChannelFilter f1 = new MockDataChannelFilter() {

            @Override
            public QueryResponse onQuery(
                    ObjectContext originatingContext,
                    Query query,
                    DataChannelFilterChain filterChain) {

                results.add("f1start");
                QueryResponse response = filterChain.onQuery(originatingContext, query);
                results.add("f1end");
                return response;
            }
        };

        DataChannelFilter f2 = new MockDataChannelFilter() {

            @Override
            public QueryResponse onQuery(
                    ObjectContext originatingContext,
                    Query query,
                    DataChannelFilterChain filterChain) {

                results.add("f2start");
                QueryResponse response = filterChain.onQuery(originatingContext, query);
                results.add("f2end");
                return response;
            }
        };

        domain.filters.add(f1);
        domain.filters.add(f2);

        SelectQuery query = new SelectQuery(Artist.class);
        QueryResponse response = domain.onQuery(context, query);
        assertNotNull(response);
        assertEquals(4, results.size());
        assertEquals("f2start", results.get(0));
        assertEquals("f1start", results.get(1));
        assertEquals("f1end", results.get(2));
        assertEquals("f2end", results.get(3));
    }

    public void testOnSync_FilterOrdering() {

        DataDomain domain = runtime.getDataDomain();

        final List<String> results = new ArrayList<String>();

        DataChannelFilter f1 = new MockDataChannelFilter() {

            @Override
            public GraphDiff onSync(
                    ObjectContext originatingContext,
                    GraphDiff changes,
                    int syncType,
                    DataChannelFilterChain filterChain) {

                results.add("f1start");
                GraphDiff response = filterChain.onSync(
                        originatingContext,
                        changes,
                        syncType);
                results.add("f1end");
                return response;
            }
        };

        DataChannelFilter f2 = new MockDataChannelFilter() {

            @Override
            public GraphDiff onSync(
                    ObjectContext originatingContext,
                    GraphDiff changes,
                    int syncType,
                    DataChannelFilterChain filterChain) {

                results.add("f2start");
                GraphDiff response = filterChain.onSync(
                        originatingContext,
                        changes,
                        syncType);
                results.add("f2end");
                return response;
            }
        };

        domain.filters.add(f1);
        domain.filters.add(f2);

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

    public void testOnQuery_Blocking() {

        DataDomain domain = runtime.getDataDomain();

        final QueryResponse r1 = new ListResponse();
        final QueryResponse r2 = new ListResponse();

        DataChannelFilter f1 = new MockDataChannelFilter() {

            @Override
            public QueryResponse onQuery(
                    ObjectContext originatingContext,
                    Query query,
                    DataChannelFilterChain filterChain) {

                return r1;
            }
        };

        DataChannelFilter f2 = new MockDataChannelFilter() {

            @Override
            public QueryResponse onQuery(
                    ObjectContext originatingContext,
                    Query query,
                    DataChannelFilterChain filterChain) {

                return r2;
            }
        };

        domain.filters.add(f1);
        domain.filters.add(f2);

        SelectQuery query = new SelectQuery(Artist.class);
        QueryResponse response = domain.onQuery(context, query);
        assertSame(r2, response);
    }
}
