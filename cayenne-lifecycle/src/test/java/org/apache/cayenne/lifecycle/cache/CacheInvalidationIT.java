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

package org.apache.cayenne.lifecycle.cache;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.lifecycle.db.E1;
import org.apache.cayenne.lifecycle.unit.CacheInvalidationCase;
import org.apache.cayenne.query.ObjectSelect;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.0
 */
public class CacheInvalidationIT extends CacheInvalidationCase {

    @Ignore("MapQueryCache doesn't store cache groups in different caches, so it can't run this test for now.")
    @Test
    public void testInvalidate_Custom() throws Exception {
        ObjectContext context = runtime.newContext();

        // no explicit cache group must still work - it lands inside default cache called 'cayenne.default.cache'
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


        E1 e1 = context.newObject(E1.class);
        context.commitChanges();
        runtime.getDataDomain().getQueryCache().removeGroup("g1");

        // inserted via Cayenne... "g1" should get auto refreshed...
        assertEquals(0, g0.selectCount(context));
        assertEquals(3, g1.selectCount(context));
        assertEquals(0, g2.selectCount(context));

        context.deleteObject(e1);
        context.commitChanges();

        // deleted via Cayenne... "g1" should get auto refreshed
        assertEquals(0, g0.selectCount(context));
        assertEquals(2, g1.selectCount(context));
        assertEquals(0, g2.selectCount(context));
    }
}
