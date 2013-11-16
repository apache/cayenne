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
package org.apache.cayenne.cache;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.apache.cayenne.query.QueryMetadata;

public class EhCacheQueryCache_WithConfigTest extends TestCase {

    protected CacheManager cacheManager;

    @Override
    protected void setUp() throws Exception {
        URL config = getClass().getResource("test-ehcache.xml");
        assertNotNull(config);
        cacheManager = new CacheManager(config);
    }

    @Override
    protected void tearDown() throws Exception {
        cacheManager.shutdown();
    }

    public void testRemoveGroup_WithFactory_WithCacheGroups() {

        EhCacheQueryCache cache = new EhCacheQueryCache(cacheManager);

        Object[] lists = new Object[] { new ArrayList<Object>(), new ArrayList<Object>(), new ArrayList<Object>() };
        QueryCacheEntryFactory factory = mock(QueryCacheEntryFactory.class);
        when(factory.createObject()).thenReturn(lists[0], lists[1], lists[2]);

        QueryMetadata md = mock(QueryMetadata.class);
        when(md.getCacheKey()).thenReturn("k1");
        when(md.getCacheGroups()).thenReturn(new String[] { "cg1" });

        assertEquals(lists[0], cache.get(md, factory));
        assertEquals(lists[0], cache.get(md, factory));

        Cache c1 = cache.cacheManager.getCache("cg1");
        assertEquals(201, c1.getCacheConfiguration().getTimeToLiveSeconds());

        // remove non-existing
        cache.removeGroup("cg0");
        assertEquals(lists[0], cache.get(md, factory));

        Cache c2 = cache.cacheManager.getCache("cg1");
        assertSame(c1, c2);
        assertEquals(201, c2.getCacheConfiguration().getTimeToLiveSeconds());

        cache.removeGroup("cg1");
        assertEquals(lists[1], cache.get(md, factory));

        // make sure the cache still has all the configured settings after
        // 'removeGroup'
        Cache c3 = cache.cacheManager.getCache("cg1");
        assertSame(c1, c3);
        assertEquals(201, c3.getCacheConfiguration().getTimeToLiveSeconds());
    }
}
