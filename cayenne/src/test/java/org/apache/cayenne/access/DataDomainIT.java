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
import org.apache.cayenne.map.compiled.TestMappings;
import org.apache.cayenne.query.resolved.ResolvedSQL;
import org.apache.cayenne.query.resolved.ResolvedQueries;
import org.apache.cayenne.query.SQLQuery;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.compiled.CompiledMapping;

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

        DataNode n1 = DataNodes.named("n1");
        domain.addNode(n1, List.of("m1"));

        DataNode n2 = DataNodes.named("n2");
        domain.addNode(n2, List.of("m2"));

        assertSame(n1, domain.lookupDataNode("m1"));
        assertSame(n2, domain.lookupDataNode("m2"));

        assertThrows(CayenneRuntimeException.class, () -> domain.lookupDataNode("m3"));
    }

    @Test
    public void lookupDataNode_Default() {

        DataDomain domain = env.runtime().getDataDomain();

        DataNode n1 = DataNodes.named("n1");
        domain.addNode(n1, List.of("m1"));
        domain.setDefaultNode(n1);

        DataNode n2 = DataNodes.named("n2");
        domain.addNode(n2, List.of("m2"));

        assertSame(n1, domain.lookupDataNode("m1"));
        assertSame(n2, domain.lookupDataNode("m2"));

        // must map to default
        assertSame(n1, domain.lookupDataNode("m3"));
    }

    @Test
    public void lookupDataNodeForQuery() {

        DataDomain domain = env.runtime().getDataDomain();
        domain.setDefaultNode(null);

        DataMap m1 = new DataMap("m1");
        DbEntity e1 = new DbEntity("E1");
        m1.addDbEntity(e1);
        DataNode n1 = DataNodes.named("n1");
        domain.addNode(n1, List.of("m1"));

        DataNode n2 = DataNodes.named("n2");
        domain.addNode(n2, List.of());

        // a query with a root is routed by its DataMap
        CompiledMapping mapping = TestMappings.compile(m1);
        assertSame(n1, domain.lookupDataNode(ResolvedQueries.select(null, mapping.schema().table("E1"))));

        // a rootless SQL statement by its node name, or else to the default node
        assertSame(n2, domain.lookupDataNode(SQLQuery.query("SELECT 1").dataNode("n2").resolve(mapping)));
        assertThrows(CayenneRuntimeException.class,
                () -> domain.lookupDataNode(SQLQuery.query("SELECT 1").dataNode("n3").resolve(mapping)));
        assertThrows(CayenneRuntimeException.class, () -> domain.lookupDataNode(ResolvedSQL.raw("SELECT 1")));

        domain.setDefaultNode(n1);
        assertSame(n1, domain.lookupDataNode(ResolvedSQL.raw("SELECT 1")));
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
        domain.setDefaultNode(null);
        assertThrows(CayenneRuntimeException.class, () -> domain.lookupDataNode("map"));

        DataNode node = DataNodes.named("1");
        domain.addNode(node, List.of("map"));
        assertSame(node, domain.lookupDataNode("map"));
    }

    @Test
    public void recompile() {
        DataDomain domain = env.runtime().getDataDomain();
        CompiledMapping mapping = domain.getMapping();
        assertNotNull(mapping);
        assertNull(mapping.entity("TestEntity"));

        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("TestEntity");
        map.addObjEntity(entity);

        domain.recompile(List.of(map));

        // the mapping is immutable, so the domain replaces it with a recompiled one
        assertNotSame(mapping, domain.getMapping());
        assertNotNull(domain.getMapping().entity("TestEntity"));
        assertEquals("TestEntity", domain.getMapping().entity("TestEntity").name());
    }

    @Test
    public void recompileUnroutesMissingMaps() {
        DataDomain domain = env.runtime().getDataDomain();
        domain.setDefaultNode(null);

        DataNode node = DataNodes.named("n1");
        domain.addNode(node, List.of("kept", "dropped"));
        assertSame(node, domain.lookupDataNode("dropped"));

        domain.recompile(List.of(new DataMap("kept")));
        assertSame(node, domain.lookupDataNode("kept"));
        assertThrows(CayenneRuntimeException.class, () -> domain.lookupDataNode("dropped"));
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
