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

import org.apache.cayenne.CoreModule;
import org.apache.cayenne.DataChannelQueryFilter;
import org.apache.cayenne.DataChannelQueryFilterChain;
import org.apache.cayenne.DataChannelSyncFilter;
import org.apache.cayenne.DataChannelSyncFilterChain;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResult;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.access.lifecycle.LifecycleEventsManager;
import org.apache.cayenne.project.DataNodeDescriptor;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.CayenneRuntime;
import org.apache.cayenne.testdo.db1.CrossdbM1E1;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests a fully DI-initialized {@link DataDomain} produced by {@link DataDomainProvider}.
 */
public class DataDomainProviderIT {

    static final TestListener LISTENER = new TestListener();
    static final NoopQueryFilter QUERY_FILTER = new NoopQueryFilter();
    static final NoopSyncFilter SYNC_FILTER = new NoopSyncFilter();

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.DOMAIN_PROVIDER_PROJECT)
            .withExtraModules(b -> CoreModule.extend(b)
                    .addPostLoadListener(LISTENER::postLoadCallback)
                    .addPostAddListener(LISTENER::postAddCallback, CrossdbM1E1.class)
                    .addSyncFilter(ListeningFilter.class)
                    .addQueryFilter(QUERY_FILTER)
                    .addSyncFilter(SYNC_FILTER));

    private CayenneRuntime twoNodeRuntime;

    @AfterEach
    public void stopTwoNodeRuntime() {
        if (twoNodeRuntime != null) {
            twoNodeRuntime.shutdown();
            twoNodeRuntime = null;
        }
    }

    /**
     * A runtime with two explicitly linked DataNodes and no default node. DataNodes are no longer a part of the XML
     * mapping, so they can only be configured via the runtime builder.
     */
    private CayenneRuntime twoNodeRuntime() {
        DataSource dataSource = CayenneTestsEnv.COMMON_SCHEMA.dataSource();

        twoNodeRuntime = CayenneRuntime.of()
                .addConfig(CayenneProjects.DOMAIN_PROVIDER_PROJECT)
                .addDataNode(DataNodeDescriptor.of("node1")
                        .dataSource(dataSource)
                        .schemaUpdateStrategy(ThrowOnPartialOrCreateSchemaStrategy.class)
                        .build(), "map-db1")
                .addDataNode(DataNodeDescriptor.of("node2").dataSource(dataSource).build(), "map-db2")
                .build();

        return twoNodeRuntime;
    }

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
        DataDomain domain = twoNodeRuntime().getDataDomain();
        assertEquals(2, domain.getDataNodes().size());

        DataMap map1 = domain.getDataMap("map-db1");
        DataNode node1 = domain.getDataNode("node1");
        assertNotNull(node1);
        assertSame(node1, domain.lookupDataNode(map1));

        DataMap map2 = domain.getDataMap("map-db2");
        DataNode node2 = domain.getDataNode("node2");
        assertNotNull(node2);
        assertSame(node2, domain.lookupDataNode(map2));
    }

    @Test
    public void dataNodeConfig() {
        DataDomain domain = twoNodeRuntime().getDataDomain();

        for (DataNode node : domain.getDataNodes()) {
            assertNotNull(node.getDataSource());
            assertNotNull(node.getAdapter());
        }
    }

    @Test
    public void noDefaultNodeWhenNoneIsConfigured() {
        assertNull(twoNodeRuntime().getDataDomain().getDefaultNode());
    }

    @Test
    public void defaultNodeTakesOverUnlinkedMapsOnly() {
        DataSource dataSource = CayenneTestsEnv.COMMON_SCHEMA.dataSource();

        twoNodeRuntime = CayenneRuntime.of()
                .addConfig(CayenneProjects.DOMAIN_PROVIDER_PROJECT)
                .addDataNode(DataNodeDescriptor.of("node1").dataSource(dataSource).build(), "map-db1")
                .defaultDataNode(DataNodeDescriptor.of("default").dataSource(dataSource).build())
                .build();

        DataDomain domain = twoNodeRuntime.getDataDomain();
        DataMap linked = domain.getDataMap("map-db1");
        DataMap unlinked = domain.getDataMap("map-db2");

        DataNode node1 = domain.getDataNode("node1");
        DataNode defaultNode = domain.getDefaultNode();

        // the default node is a regular node of the domain, addressable by name
        assertEquals(2, domain.getDataNodes().size());
        assertSame(defaultNode, domain.getDataNode("default"));

        // "map-db1" stays with its own node, "map-db2" is taken over by the default node
        assertSame(node1, domain.lookupDataNode(linked));
        assertSame(defaultNode, domain.lookupDataNode(unlinked));
    }

    @Test
    public void nodeLinkedToMissingDataMapIsStillRegistered() {
        DataSource dataSource = CayenneTestsEnv.COMMON_SCHEMA.dataSource();

        twoNodeRuntime = CayenneRuntime.of()
                .addConfig(CayenneProjects.DOMAIN_PROVIDER_PROJECT)
                .addDataNode(DataNodeDescriptor.of("node1").dataSource(dataSource).build(), "no-such-map")
                .build();

        DataDomain domain = twoNodeRuntime.getDataDomain();

        // the missing DataMap is ignored, but the node itself is still a part of the domain
        assertNotNull(domain.getDataNode("node1"));
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
        domain.getLifecycleEventsManager()
                .postLoad().onEvent(List.of(env.context().newObject(CrossdbM1E1.class)));

        assertEquals(before + 1, LISTENER.counter.get(), "Should call postLoadCallback()");
    }

    @Test
    public void entityListenersFromDI() {
        int before = LISTENER.added.get();
        env.context().newObject(CrossdbM1E1.class);
        assertEquals(before + 1, LISTENER.added.get(), "Should call postAddCallback()");
    }

    @Test
    public void filterListenersFromDI() {
        DataDomain domain = env.runtime().getDataDomain();

        int before = ListeningFilter.removed.get();
        domain.getLifecycleEventsManager()
                .postRemove().onEvent(List.of(env.context().newObject(CrossdbM1E1.class)));

        assertEquals(before + 1, ListeningFilter.removed.get(), "Should call the listener registered by the filter");
    }

    @Test
    public void filtersFromDI() {
        DataDomain domain = env.runtime().getDataDomain();

        assertTrue(domain.getQueryFilters().contains(QUERY_FILTER));
        assertTrue(domain.getSyncFilters().contains(SYNC_FILTER));
    }

    static class TestListener {

        final AtomicInteger counter = new AtomicInteger();
        final AtomicInteger added = new AtomicInteger();

        public void postLoadCallback(Object object) {
            counter.incrementAndGet();
        }

        public void postAddCallback(CrossdbM1E1 object) {
            added.incrementAndGet();
        }
    }

    /**
     * A DI-created filter that registers its own listener when initialized by the DataDomain.
     */
    public static class ListeningFilter implements DataChannelSyncFilter {

        static final AtomicInteger removed = new AtomicInteger();

        @Override
        public void init(LifecycleEventsManager lifecycleEventsManager) {
            lifecycleEventsManager.postRemove().addListener(o -> removed.incrementAndGet());
        }

        @Override
        public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType,
                                DataChannelSyncFilterChain filterChain) {
            return filterChain.onSync(originatingContext, changes, syncType);
        }
    }

    static class NoopQueryFilter implements DataChannelQueryFilter {

        @Override
        public List<QueryResult> onQuery(OperationContext context, DataChannelQueryFilterChain filterChain) {
            return filterChain.onQuery(context);
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
