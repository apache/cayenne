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
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.cache.MapQueryCacheFactory;
import org.apache.cayenne.cache.MockQueryCache;
import org.apache.cayenne.cache.MockQueryCacheFactory;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheFactory;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneCase;

/**
 * DataDomain unit tests.
 * 
 */
public class DataDomainTest extends CayenneCase {

    public void testName() throws Exception {
        DataDomain domain = new DataDomain("some name");
        assertEquals("some name", domain.getName());
        domain.setName("tst_name");
        assertEquals("tst_name", domain.getName());
    }

    public void testDataContextFactory() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        DataContextFactory dataContextFactory = new DataContextFactory() {

            public DataContext createDataContext(
                    DataChannel parent,
                    ObjectStore objectStore) {
                return null;
            }
        };
        domain.setDataContextFactory(null);
        assertNull(domain.getDataContextFactory());
        domain.setDataContextFactory(dataContextFactory);
        assertSame(dataContextFactory, domain.getDataContextFactory());
    }

    public void testCreateDataContextWithDefaultDataContextFactory() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        assertNotNull(domain.createDataContext());
    }

    public void testCreateDataContextWithNullDataContextFactory() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        domain.setDataContextFactory(null);
        assertNotNull(domain.createDataContext());
    }

    public void testCreateDataContextWithCustomDataContextFactory() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        final DataContext dataContext = new DataContext();
        DataContextFactory dataContextFactory = new DataContextFactory() {

            public DataContext createDataContext(
                    DataChannel parent,
                    ObjectStore objectStore) {
                return dataContext;
            }
        };
        domain.setDataContextFactory(dataContextFactory);
        assertEquals(dataContext, domain.createDataContext());
    }

    public void testNodes() throws java.lang.Exception {
        DataDomain domain = new DataDomain("dom1");
        assertEquals(0, domain.getDataNodes().size());
        domain.addNode(new DataNode("1"));
        assertEquals(1, domain.getDataNodes().size());
        domain.addNode(new DataNode("2"));
        assertEquals(2, domain.getDataNodes().size());
    }

    public void testNodeMaps() throws java.lang.Exception {
        DataDomain domain = new DataDomain("dom1");
        assertNull(domain.getMap("map"));

        DataNode node = new DataNode("1");
        node.addDataMap(new DataMap("map"));

        domain.addNode(node);
        assertNotNull(domain.getMap("map"));
    }

    public void testMaps() throws java.lang.Exception {
        DataDomain d1 = new DataDomain("dom1");

        DataMap m1 = new DataMap("m1");
        d1.addMap(m1);
        assertSame(m1, d1.getMap(m1.getName()));

        d1.removeMap(m1.getName());
        assertNull(d1.getMap(m1.getName()));
    }

    public void testReindexNodes() throws Exception {
        DataDomain domain = new DataDomain("dom1");
        DataMap map = new DataMap("map");
        DataNode node = new DataNode("1");

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

        domain.addMap(map);

        assertSame(entity, resolver.getObjEntity("TestEntity"));
    }

    public void testEntityResolver() {
        assertNotNull(getDomain().getEntityResolver());

        DataDomain domain = new DataDomain("dom1");
        assertNotNull(domain.getEntityResolver());
    }

    public void testCreateDataContextWithSharedCache() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.SHARED_CACHE_ENABLED_PROPERTY, Boolean.TRUE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isSharedCacheEnabled());

        DataContext c1 = domain.createDataContext();
        assertSame(c1.getObjectStore().getDataRowCache(), domain.getSharedSnapshotCache());

        DataContext c2 = domain.createDataContext(true);
        assertSame(c2.getObjectStore().getDataRowCache(), domain.getSharedSnapshotCache());

        DataContext c3 = domain.createDataContext(false);
        assertNotSame(c3.getObjectStore().getDataRowCache(), domain
                .getSharedSnapshotCache());
    }

    public void testCreateDataContextWithDedicatedCache() throws Exception {
        Map properties = new HashMap();
        properties
                .put(DataDomain.SHARED_CACHE_ENABLED_PROPERTY, Boolean.FALSE.toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isSharedCacheEnabled());
        assertNull(domain.getSharedSnapshotCache());

        DataContext c3 = domain.createDataContext(false);
        assertNotNull(c3.getObjectStore().getDataRowCache());
        assertNull(domain.getSharedSnapshotCache());
        assertNotSame(c3.getObjectStore().getDataRowCache(), domain
                .getSharedSnapshotCache());

        DataContext c1 = domain.createDataContext();
        assertNotNull(c1.getObjectStore().getDataRowCache());
        assertNull(domain.getSharedSnapshotCache());
        assertNotSame(c1.getObjectStore().getDataRowCache(), domain
                .getSharedSnapshotCache());

        // this should trigger shared cache creation
        DataContext c2 = domain.createDataContext(true);
        assertNotNull(c2.getObjectStore().getDataRowCache());
        assertNotNull(domain.getSharedSnapshotCache());
        assertSame(c2.getObjectStore().getDataRowCache(), domain.getSharedSnapshotCache());

        DataContext c4 = domain.createDataContext();
        assertNotSame(c4.getObjectStore().getDataRowCache(), c1
                .getObjectStore()
                .getDataRowCache());
    }

    public void testCreateDataContextValidation() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY, Boolean.TRUE
                .toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isValidatingObjectsOnCommit());

        DataContext c1 = domain.createDataContext(true);
        assertTrue(c1.isValidatingObjectsOnCommit());
    }

    public void testCreateDataContextNoValidation() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.VALIDATING_OBJECTS_ON_COMMIT_PROPERTY, Boolean.FALSE
                .toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isValidatingObjectsOnCommit());

        DataContext c1 = domain.createDataContext(true);
        assertFalse(c1.isValidatingObjectsOnCommit());
    }

    public void testDataDomainInternalTransactions() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY, Boolean.FALSE
                .toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertFalse(domain.isUsingExternalTransactions());

        Transaction transaction = domain.createTransaction();
        assertTrue(transaction instanceof InternalTransaction);
    }

    public void testDataDomainExternalTransactions() throws Exception {
        Map properties = new HashMap();
        properties.put(DataDomain.USING_EXTERNAL_TRANSACTIONS_PROPERTY, Boolean.TRUE
                .toString());

        DataDomain domain = new DataDomain("d1", properties);
        assertTrue(domain.isUsingExternalTransactions());

        Transaction transaction = domain.createTransaction();
        assertTrue(transaction instanceof ExternalTransaction);
    }

    public void testDataDomainDataContextFactory() {

        // null
        DataDomain d1 = new DataDomain("d1", new HashMap());
        assertNull(d1.getDataContextFactory());

        // not null
        DataDomain d2 = new DataDomain("d2", Collections.singletonMap(
                DataDomain.DATA_CONTEXT_FACTORY_PROPERTY,
                MockDataContextFactory.class.getName()));

        assertNotNull(d2.getDataContextFactory());
        assertTrue(d2.getDataContextFactory() instanceof MockDataContextFactory);

        // invalid

        try {
            new DataDomain("d2", Collections.singletonMap(
                    DataDomain.DATA_CONTEXT_FACTORY_PROPERTY,
                    Object.class.getName()));
            fail("Bogus DataContextFactrory went through unnoticed...");
        }
        catch (CayenneRuntimeException e) {
            // expected
        }
    }

    public void testQueryCache() {
        DataDomain domain = new DataDomain("X");
        QueryCache cache = domain.getQueryCache();
        assertNotNull(cache);
    }

    public void testQueryCacheFactory() {
        DataDomain domain = new DataDomain("X");
        QueryCacheFactory qcFactory = domain.getQueryCacheFactory();
        assertNotNull(qcFactory);
        assertTrue(qcFactory instanceof MapQueryCacheFactory);

        domain = new DataDomain("Y");
        MockQueryCacheFactory f2 = new MockQueryCacheFactory();
        domain.setQueryCacheFactory(f2);
        assertSame(f2, domain.getQueryCacheFactory());
        if (!(domain.getQueryCache() instanceof MockQueryCache)) {
            fail("Unknown query cache created: " + domain.getQueryCache());
        }
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
                new EventManager()) {

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
