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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.DataNodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import org.junit.jupiter.api.extension.RegisterExtension;


import static org.junit.jupiter.api.Assertions.*;

public class DataDomainIT {

    @RegisterExtension
    final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT)
            .withExtraModules(b -> b.bind(DataRowStoreFactory.class).to(ShutdownTrackingRowStoreFactory.class));

    @Test
    public void lookupDataNode() {

        DataDomain domain = env.runtime().getDataDomain();

        // an unmapped DataMap falls back to the default node, so drop it to test the "no node" case below
        domain.setDefaultNode(null);

        DataMap m1 = new DataMap("m1");
        DataNode n1 = DataNodes.named("n1");
        domain.addNode(n1, List.of(m1));

        DataMap m2 = new DataMap("m2");
        DataNode n2 = DataNodes.named("n2");
        domain.addNode(n2, List.of(m2));

        assertSame(n1, domain.lookupDataNode(m1));
        assertSame(n2, domain.lookupDataNode(m2));

        assertThrows(CayenneRuntimeException.class, () -> domain.lookupDataNode(new DataMap("m3")));
    }

    @Test
    public void lookupDataNode_Default() {

        DataDomain domain = env.runtime().getDataDomain();

        DataMap m1 = new DataMap("m1");
        DataNode n1 = DataNodes.named("n1");
        domain.addNode(n1, List.of(m1));
        domain.setDefaultNode(n1);

        DataMap m2 = new DataMap("m2");
        DataNode n2 = DataNodes.named("n2");
        domain.addNode(n2, List.of(m2));

        assertSame(n1, domain.lookupDataNode(m1));
        assertSame(n2, domain.lookupDataNode(m2));

        // must map to default
        assertSame(n1, domain.lookupDataNode(new DataMap("m3")));
    }

    @Test
    public void nodes() throws Exception {
        DataDomain domain = env.runtime().getDataDomain();
        assertEquals(1, domain.getDataNodes().size());
        DataNode node = DataNodes.named("1");
        domain.addNode(node, List.of());
        assertEquals(2, domain.getDataNodes().size());
        node = DataNodes.named("2");
        domain.addNode(node, List.of());
        assertEquals(3, domain.getDataNodes().size());
    }

    @Test
    public void nodeMaps() {
        DataDomain domain = env.runtime().getDataDomain();
        assertNull(domain.getDataMap("map"));

        DataNode node = DataNodes.named("1");
        domain.addNode(node, List.of(new DataMap("map")));
        assertNotNull(domain.getDataMap("map"));
    }

    @Test
    public void maps() throws Exception {
        DataDomain d1 = env.runtime().getDataDomain();

        DataMap m1 = new DataMap("m1");
        d1.addDataMap(m1);
        assertSame(m1, d1.getDataMap(m1.getName()));

        d1.removeDataMap(m1.getName());
        assertNull(d1.getDataMap(m1.getName()));
    }

    @Test
    public void entityResolverRefresh() throws Exception {
        DataDomain domain = env.runtime().getDataDomain();
        EntityResolver resolver = domain.getEntityResolver();
        assertNotNull(resolver);

        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("TestEntity");
        map.addObjEntity(entity);

        domain.addDataMap(map);

        assertSame(entity, resolver.getObjEntity("TestEntity"));
    }

    @Test
    public void shutdownCache() {
        DataDomain domain = env.runtime().getDataDomain();

        ShutdownTrackingRowStore cache = (ShutdownTrackingRowStore) domain.getSharedSnapshotCache();
        assertFalse(cache.shutdown);

        domain.shutdown();
        assertTrue(cache.shutdown);
    }

    public static class ShutdownTrackingRowStoreFactory implements DataRowStoreFactory {

        private final EventManager eventManager;

        public ShutdownTrackingRowStoreFactory(@Inject EventManager eventManager) {
            this.eventManager = eventManager;
        }

        @Override
        public DataRowStore createDataRowStore(String name) {
            return new ShutdownTrackingRowStore(name, eventManager);
        }
    }

    public static class ShutdownTrackingRowStore extends DataRowStore {

        boolean shutdown;

        public ShutdownTrackingRowStore(String name, EventManager eventManager) {
            super(name, DefaultDataRowStoreFactory.SNAPSHOT_CACHE_SIZE_DEFAULT, eventManager);
        }

        @Override
        public void shutdown() {
            shutdown = true;
            super.shutdown();
        }
    }
}
