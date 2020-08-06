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

package org.apache.cayenne.cache.invalidation;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.invalidation.db.E1;
import org.apache.cayenne.cache.invalidation.db.E2;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.query.ObjectSelect;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */
public class CacheInvalidationCacheGroupsHandlerIT extends CacheInvalidationCase {

    private AtomicInteger removeGroupUntypedCounter;
    private AtomicInteger removeGroupTypedCounter;

    @Before
    public void resetCounters() {
        removeGroupUntypedCounter = new AtomicInteger(0);
        removeGroupTypedCounter = new AtomicInteger(0);
    }

    @Override
    protected Module extendInvalidationModule() {
        return binder -> {
            // do nothing module
        };
    }

    @Override
    protected Module buildCustomModule() {
        // Proxy query cache that will count methods calls
        final QueryCache cache = new MapQueryCache() {
            @Override
            public void removeGroup(String groupKey) {
                removeGroupUntypedCounter.incrementAndGet();
                super.removeGroup(groupKey);
            }

            @Override
            public void removeGroup(String groupKey, Class<?> keyType, Class<?> valueType) {
                removeGroupTypedCounter.incrementAndGet();
                super.removeGroup(groupKey, keyType, valueType);
            }
        };

        return binder -> binder.bind(QueryCache.class).toInstance(cache);
    }

    @Test
    public void invalidateE1() throws Exception {
        ObjectContext context = runtime.newContext();

        ObjectSelect<E1> g0 = ObjectSelect.query(E1.class).localCache();
        ObjectSelect<E1> g1 = ObjectSelect.query(E1.class).localCache("g1");
        ObjectSelect<E1> g2 = ObjectSelect.query(E1.class).localCache("g2");

        assertEquals(0, g0.selectCount(context));
        assertEquals(0, g1.selectCount(context));
        assertEquals(0, g2.selectCount(context));

        e1.insert(1).insert(2);

        // inserted via SQL... query results are still cached...
        assertEquals(0, g0.selectCount(context));
        assertEquals(0, g1.selectCount(context));
        assertEquals(0, g2.selectCount(context));


        context.newObject(E1.class);
        context.commitChanges();

        assertEquals(2, removeGroupUntypedCounter.get());
        assertEquals(0, removeGroupTypedCounter.get());
        // inserted via Cayenne... "g1" and "g2" should get auto refreshed...
        assertEquals(0, g0.selectCount(context));
        assertEquals(3, g1.selectCount(context));
        assertEquals(3, g2.selectCount(context));
    }

    @Test
    public void invalidateE2() throws Exception {
        ObjectContext context = runtime.newContext();

        ObjectSelect<E2> g0 = ObjectSelect.query(E2.class).localCache();
        ObjectSelect<E2> g1 = ObjectSelect.query(E2.class).localCache("g1");
        ObjectSelect<E2> g2 = ObjectSelect.query(E2.class).localCache("g2");
        ObjectSelect<E2> g3 = ObjectSelect.query(E2.class).localCache("g3");
        ObjectSelect<E2> g5 = ObjectSelect.query(E2.class).localCache("g5");

        assertEquals(0, g0.selectCount(context));
        assertEquals(0, g1.selectCount(context));
        assertEquals(0, g2.selectCount(context));
        assertEquals(0, g3.selectCount(context));
        assertEquals(0, g5.selectCount(context));

        e2.insert(1).insert(2);

        // inserted via SQL... query results are still cached...
        assertEquals(0, g0.selectCount(context));
        assertEquals(0, g1.selectCount(context));
        assertEquals(0, g2.selectCount(context));
        assertEquals(0, g3.selectCount(context));
        assertEquals(0, g5.selectCount(context));


        context.newObject(E2.class);
        context.commitChanges();

        // Typed remove will actually call untyped version, thus 4 + 2
        assertEquals(4 + 2, removeGroupUntypedCounter.get());
        assertEquals(2, removeGroupTypedCounter.get());

        // inserted via Cayenne... "g1" and "g2" should get auto refreshed...
        assertEquals(0, g0.selectCount(context));
        assertEquals(3, g1.selectCount(context));
        assertEquals(3, g2.selectCount(context));
        assertEquals(3, g3.selectCount(context));
        assertEquals(3, g5.selectCount(context));
    }
}
