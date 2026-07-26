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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.annotation.PostAdd;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Exhibit;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testdo.testmap.annotations.Tag1;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
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
        DataNode n1 = new DataNode("n1");
        n1.addDataMap(m1);
        domain.addNode(n1);

        DataMap m2 = new DataMap("m2");
        DataNode n2 = new DataNode("n2");
        n2.addDataMap(m2);
        domain.addNode(n2);

        assertSame(n1, domain.lookupDataNode(m1));
        assertSame(n2, domain.lookupDataNode(m2));

        assertThrows(CayenneRuntimeException.class, () -> domain.lookupDataNode(new DataMap("m3")));
    }

    @Test
    public void lookupDataNode_Default() {

        DataDomain domain = env.runtime().getDataDomain();

        DataMap m1 = new DataMap("m1");
        DataNode n1 = new DataNode("n1");
        n1.addDataMap(m1);
        domain.setDefaultNode(n1);

        DataMap m2 = new DataMap("m2");
        DataNode n2 = new DataNode("n2");
        n2.addDataMap(m2);
        domain.addNode(n2);

        assertSame(n1, domain.lookupDataNode(m1));
        assertSame(n2, domain.lookupDataNode(m2));

        // must map to default
        assertSame(n1, domain.lookupDataNode(new DataMap("m3")));
    }

    @Test
    public void nodes() throws Exception {
        DataDomain domain = env.runtime().getDataDomain();
        assertEquals(1, domain.getDataNodes().size());
        DataNode node = new DataNode("1");
        domain.addNode(node);
        assertEquals(2, domain.getDataNodes().size());
        node = new DataNode("2");
        domain.addNode(node);
        assertEquals(3, domain.getDataNodes().size());
    }

    @Test
    public void nodeMaps() {
        DataDomain domain = env.runtime().getDataDomain();
        assertNull(domain.getDataMap("map"));

        DataNode node = new DataNode("1");
        node.addDataMap(new DataMap("map"));

        domain.addNode(node);
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

    @Test
    public void addListener() {

        DataDomain domain = env.runtime().getDataDomain();
        PostAddListener listener = new PostAddListener();
        domain.addListener(listener);

        ObjectContext context = env.runtime().newContext();

        context.newObject(Gallery.class);
        assertEquals("e:Gallery;", listener.getAndReset());

        context.newObject(Artist.class);
        assertEquals("a:Artist;", listener.getAndReset());

        context.newObject(Exhibit.class);
        assertEquals("", listener.getAndReset());

        context.newObject(Painting.class);
        assertEquals("e:Painting;", listener.getAndReset());
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

    class PostAddListener {

        StringBuilder callbackBuffer = new StringBuilder();

        @PostAdd({Gallery.class, Painting.class})
        void postAddEntities(Persistent object) {
            callbackBuffer.append("e:").append(object.getObjectId().getEntityName()).append(";");
        }

        @PostAdd(entityAnnotations = Tag1.class)
        void postAddAnnotated(Persistent object) {
            callbackBuffer.append("a:").append(object.getObjectId().getEntityName()).append(";");
        }

        String getAndReset() {
            String v = callbackBuffer.toString();
            callbackBuffer = new StringBuilder();
            return v;
        }
    }
}
