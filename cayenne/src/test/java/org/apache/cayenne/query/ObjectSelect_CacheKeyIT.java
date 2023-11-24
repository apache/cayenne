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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.*;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ObjectSelect_CacheKeyIT extends RuntimeCase {

    @Inject
    private EntityResolver resolver;

    @Test
    public void testNoCache() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.getCacheStrategy());
        assertNull(md1.getCacheKey());

        QueryMetadata md2 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.getCacheStrategy());
        assertNull(md2.getCacheKey());
    }

    @Test
    public void testLocalCache() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .localCache();

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    @Test
    public void testUseLocalCache() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache();

        QueryMetadata md1 = q1.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
        assertNull(md1.getCacheGroup());

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class);
        q2.useLocalCache("g1");

        QueryMetadata md2 = q2.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md2.getCacheStrategy());
        assertNotNull(md2.getCacheKey());
        assertEquals("g1", md2.getCacheGroup());
    }

    @Test
    public void testSharedCache() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .sharedCache();

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    @Test
    public void testUseSharedCache() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .sharedCache();

        QueryMetadata md1 = q1.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
        assertNull(md1.getCacheGroup());

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .sharedCache("g1");

        QueryMetadata md2 = q2.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md2.getCacheStrategy());
        assertNotNull(md2.getCacheKey());
        assertEquals("g1", md2.getCacheGroup());
    }

    @Test
    public void testNamedQuery() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .sharedCache();

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotEquals("XYZ", md1.getCacheKey());
    }

    @Test
    public void testUniqueKeyEntity() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache();

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache();

        ObjectSelect<Painting> q3 = ObjectSelect.query(Painting.class)
                .localCache();

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertNotEquals(q1.getMetaData(resolver).getCacheKey(), q3.getMetaData(resolver).getCacheKey());
    }

    @Test
    public void testUniqueKeyQualifier() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.matchExp("a", "b"));

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.matchExp("a", "b"));

        ObjectSelect<Artist> q3 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.matchExp("a", "c"));

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertNotEquals(q1.getMetaData(resolver).getCacheKey(), q3.getMetaData(resolver).getCacheKey());
    }

    @Test
    public void testUniqueKeyFetchLimit() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache()
                .limit(5);

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache()
                .limit(5);

        ObjectSelect<Artist> q3 = ObjectSelect.query(Artist.class)
                .localCache()
                .limit(6);

        ObjectSelect<Artist> q4 = ObjectSelect.query(Artist.class)
                .localCache();

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertNotEquals(q1.getMetaData(resolver).getCacheKey(), q3.getMetaData(resolver).getCacheKey());
        assertNotEquals(q1.getMetaData(resolver).getCacheKey(), q4.getMetaData(resolver).getCacheKey());
    }

    @Test
    public void testUniqueKeyHaving() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache()
                .having(ExpressionFactory.expFalse());

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache()
                .having(ExpressionFactory.expFalse());

        ObjectSelect<Artist> q3 = ObjectSelect.query(Artist.class)
                .localCache()
                .having(ExpressionFactory.expTrue());

        ObjectSelect<Artist> q4 = ObjectSelect.query(Artist.class)
                .localCache();

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertNotEquals(q1.getMetaData(resolver).getCacheKey(), q3.getMetaData(resolver).getCacheKey());
        assertNotEquals(q1.getMetaData(resolver).getCacheKey(), q4.getMetaData(resolver).getCacheKey());
    }
}
