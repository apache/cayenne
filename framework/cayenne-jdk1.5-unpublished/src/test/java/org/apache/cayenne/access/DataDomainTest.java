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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataDomainTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private JdbcEventLogger logger;

    public void testName() throws Exception {
        DataDomain domain = new DataDomain("some name");
        assertEquals("some name", domain.getName());
        domain.setName("tst_name");
        assertEquals("tst_name", domain.getName());
    }

    public void testLookupDataNode() {

        DataDomain domain = new DataDomain("test");

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

        try {

            domain.lookupDataNode(new DataMap("m3"));
            fail("must have thrown on missing Map to Node maping");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }

    public void testLookupDataNode_Default() {

        DataDomain domain = new DataDomain("test");

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

    public void testNodes() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        assertEquals(0, domain.getDataNodes().size());
        DataNode node = new DataNode("1");
        node.setJdbcEventLogger(logger);
        domain.addNode(node);
        assertEquals(1, domain.getDataNodes().size());
        node = new DataNode("2");
        node.setJdbcEventLogger(logger);
        domain.addNode(node);
        assertEquals(2, domain.getDataNodes().size());
    }

    public void testNodeMaps() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        assertNull(domain.getDataMap("map"));

        DataNode node = new DataNode("1");
        node.setJdbcEventLogger(logger);
        node.addDataMap(new DataMap("map"));

        domain.addNode(node);
        assertNotNull(domain.getDataMap("map"));
    }

    public void testMaps() throws Exception {
        DataDomain d1 = new DataDomain("dom1");

        DataMap m1 = new DataMap("m1");
        d1.addDataMap(m1);
        assertSame(m1, d1.getDataMap(m1.getName()));

        d1.removeDataMap(m1.getName());
        assertNull(d1.getDataMap(m1.getName()));
    }

    @Deprecated
    public void testReindexNodes() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        DataMap map = new DataMap("map");
        DataNode node = new DataNode("1");
        node.setJdbcEventLogger(logger);

        domain.addNode(node);

        assertNull(domain.nodesByDataMapName.get("map"));
        node.addDataMap(map);
        assertNull(domain.nodesByDataMapName.get("map"));

        // testing this
        domain.reindexNodes();

        assertSame(node, domain.nodesByDataMapName.get("map"));
    }

    public void testEntityResolverRefresh() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        org.apache.cayenne.map.EntityResolver resolver = domain.getEntityResolver();
        assertNotNull(resolver);

        DataMap map = new DataMap("map");
        ObjEntity entity = new ObjEntity("TestEntity");
        map.addObjEntity(entity);

        domain.addDataMap(map);

        assertSame(entity, resolver.getObjEntity("TestEntity"));
    }

    public void testEntityResolver() {
        assertNotNull(runtime.getDataDomain().getEntityResolver());

        DataDomain domain = new DataDomain("dom1");
        assertNotNull(domain.getEntityResolver());
    }

    public void testInitDataDomainWithSharedCache() throws Exception {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(DataDomain.SHARED_CACHE_ENABLED_PROPERTY, Boolean.TRUE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isSharedCacheEnabled());
    }

    public void testInitDataDomainWithDedicatedCache() throws Exception {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties
                .put(DataDomain.SHARED_CACHE_ENABLED_PROPERTY, Boolean.FALSE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isSharedCacheEnabled());
    }

    public void testInitDataDomainValidation() throws Exception {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(
                DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                Boolean.TRUE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isValidatingObjectsOnCommit());
    }

    public void testInitDataDomainNoValidation() throws Exception {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(
                DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY,
                Boolean.FALSE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isValidatingObjectsOnCommit());
    }

    public void testDataDomainInternalTransactions() throws Exception {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(
                DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                Boolean.FALSE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isUsingExternalTransactions());

        Transaction transaction = domain.createTransaction();
        assertTrue(transaction instanceof InternalTransaction);
    }

    public void testDataDomainExternalTransactions() throws Exception {
        Map<Object, Object> properties = new HashMap<Object, Object>();
        properties.put(
                DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY,
                Boolean.TRUE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isUsingExternalTransactions());

        Transaction transaction = domain.createTransaction();
        assertTrue(transaction instanceof ExternalTransaction);
    }

    public void testShutdown() {
        DataDomain domain = new DataDomain("X");

        final boolean[] nodeShutdown = new boolean[2];

        DataNode n1 = new DataNode("N1") {

            @Override
            public synchronized void shutdown() {
                nodeShutdown[0] = true;
            }
        };

        DataNode n2 = new DataNode("N2") {

            @Override
            public synchronized void shutdown() {
                nodeShutdown[1] = true;
            }
        };
        n1.setJdbcEventLogger(logger);

        domain.addNode(n1);
        domain.addNode(n2);

        domain.shutdown();

        assertTrue(nodeShutdown[0]);
        assertTrue(nodeShutdown[1]);
    }

    public void testShutdownCache() {
        DataDomain domain = new DataDomain("X");

        final boolean[] cacheShutdown = new boolean[1];

        DataRowStore cache = new DataRowStore(
                "Y",
                Collections.EMPTY_MAP,
                new DefaultEventManager()) {

            @Override
            public void shutdown() {
                cacheShutdown[0] = true;
            }
        };

        domain.setSharedSnapshotCache(cache);
        domain.shutdown();

        assertTrue(cacheShutdown[0]);
    }
}
