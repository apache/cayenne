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
package org.apache.cayenne.query;

import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.query.resolved.ResolvedQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SQLQueryCacheKeyIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void noCache() {

        SQLQuery<Artist> query = SQLQuery.query(Artist.class, "SELECT ME");

        ResolvedQuery md1 = query.resolve(env.mapping());
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.cacheStrategy());
        assertNull(cacheKey(md1));

        ResolvedQuery md2 = query.resolve(env.mapping());
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.cacheStrategy());
        assertNull(cacheKey(md2));
    }

    @Test
    public void localCache() {

        SQLQuery<Artist> query = SQLQuery.query(Artist.class, "SELECT ME");

        query.cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        ResolvedQuery md1 = query.resolve(env.mapping());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.cacheStrategy());
        assertNotNull(cacheKey(md1));
    }

    @Test
    public void sharedCache() {

        SQLQuery<Artist> query = SQLQuery.query(Artist.class, "SELECT ME");

        query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        ResolvedQuery md1 = query.resolve(env.mapping());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.cacheStrategy());
        assertNotNull(cacheKey(md1));
    }

    @Test
    public void namedQuery() {

        SQLQuery<Artist> query = SQLQuery.query(Artist.class, "SELECT ME");

        query.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        ResolvedQuery md1 = query.resolve(env.mapping());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.cacheStrategy());
        assertFalse("XYZ".equals(cacheKey(md1)));
    }

    @Test
    public void cacheFetchLimitAndOffset() {
        SQLQuery<Artist> q1 = SQLQuery.query(Artist.class, "SELECT ME");
        q1.offset(5);
        q1.limit(10);
        q1.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        
        SQLQuery<Artist> q2 = SQLQuery.query(Artist.class, "SELECT ME");
        q2.offset(5);
        q2.limit(10);
        q2.cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        assertEquals(cacheKey(q1.resolve(env.mapping())), cacheKey(q2.resolve(env.mapping())));
    }

    private static String cacheKey(ResolvedQuery resolved) {
        return resolved.cacheKey(env.valueObjectTypes());
    }
}
