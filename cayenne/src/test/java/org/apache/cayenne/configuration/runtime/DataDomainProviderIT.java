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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelQueryFilterChain;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.testdo.db1.CrossdbM1E1;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests a fully DI-initialized {@link DataDomain} produced by {@link DataDomainProvider} off a two-node project.
 */
public class DataDomainProviderIT {

    static final TestListener LISTENER = new TestListener();
    static final NoopQueryFilter QUERY_FILTER = new NoopQueryFilter();
    static final NoopSyncFilter SYNC_FILTER = new NoopSyncFilter();

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.DOMAIN_PROVIDER_PROJECT)
            .withExtraModules(b -> CoreModule.extend(b)
                    .addListener(LISTENER)
                    .addQueryFilter(QUERY_FILTER)
                    .addSyncFilter(SYNC_FILTER));

    @Test
    public void dataMaps() {
        DataDomain domain = env.runtime().getDataDomain();

        assertEquals("domain-provider", domain.getName());
        assertEquals(2, domain.getDataMaps().size());
        assertNotNull(domain.getDataMap("map-db1"));
        assertNotNull(domain.getDataMap("map-db2"));
    }

    @Test
    public void eventManagerFromDI() {
        DataDomain domain = env.runtime().getDataDomain();

        assertSame(env.runtime().getInjector().getInstance(EventManager.class), domain.getEventManager());
    }

    @Test
    public void dataNodes() {
        DataDomain domain = env.runtime().getDataDomain();
        assertEquals(2, domain.getDataNodes().size());

        DataMap map1 = domain.getDataMap("map-db1");
        DataNode node1 = domain.getDataNode("node1");
        assertNotNull(node1);
        assertEquals(1, node1.getDataMaps().size());
        assertSame(map1, node1.getDataMaps().iterator().next());
        assertSame(node1, domain.lookupDataNode(map1));

        DataMap map2 = domain.getDataMap("map-db2");
        DataNode node2 = domain.getDataNode("node2");
        assertNotNull(node2);
        assertEquals(1, node2.getDataMaps().size());
        assertSame(map2, node2.getDataMaps().iterator().next());
        assertSame(node2, domain.lookupDataNode(map2));
    }

    @Test
    public void dataNodeConfig() {
        DataDomain domain = env.runtime().getDataDomain();

        for (DataNode node : domain.getDataNodes()) {
            assertEquals(XMLPoolingDataSourceFactory.class.getName(), node.getDataSourceFactory());
            assertNotNull(node.getDataSource());
            assertNotNull(node.getAdapter());
        }

        // "node1" declares an explicit strategy, "node2" gets the default
        assertEquals(
                ThrowOnPartialOrCreateSchemaStrategy.class,
                domain.getDataNode("node1").getSchemaUpdateStrategy().getClass());
        assertEquals(
                SkipSchemaUpdateStrategy.class,
                domain.getDataNode("node2").getSchemaUpdateStrategy().getClass());
    }

    @Test
    public void noDefaultNodeWithMultipleNodes() {
        // a default node is only inferred when the project declares exactly one node
        assertNull(env.runtime().getDataDomain().getDefaultNode());
    }

    @Test
    public void domainProperties() {
        DataDomain domain = env.runtime().getDataDomain();

        // "validatingObjectsOnCommit" is turned off by a <property> in the project file, shared cache is left at its
        // default
        assertFalse(domain.isValidatingObjectsOnCommit());
        assertTrue(domain.isSharedCacheEnabled());
        assertNotNull(domain.getSharedSnapshotCache());
    }

    @Test
    public void listenersFromDI() {
        DataDomain domain = env.runtime().getDataDomain();

        int before = LISTENER.counter.get();
        domain.getEntityResolver().getCallbackRegistry()
                .performCallbacks(LifecycleEvent.POST_LOAD, env.context().newObject(CrossdbM1E1.class));

        assertEquals(before + 1, LISTENER.counter.get(), "Should call postLoadCallback()");
    }

    @Test
    public void filtersFromDI() {
        DataDomain domain = env.runtime().getDataDomain();

        assertTrue(domain.getQueryFilters().contains(QUERY_FILTER));
        assertTrue(domain.getSyncFilters().contains(SYNC_FILTER));
    }

    static class TestListener {

        final AtomicInteger counter = new AtomicInteger();

        @PostLoad
        public void postLoadCallback(Object object) {
            counter.incrementAndGet();
        }
    }

    static class NoopQueryFilter implements DataChannelQueryFilter {

        @Override
        public QueryResponse onQuery(ObjectContext originatingContext, Query query,
                                     DataChannelQueryFilterChain filterChain) {
            return filterChain.onQuery(originatingContext, query);
        }
    }

    static class NoopSyncFilter implements DataChannelSyncFilter {

        @Override
        public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType,
                                DataChannelSyncFilterChain filterChain) {
            return filterChain.onSync(originatingContext, changes, syncType);
        }
    }
}
