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
package org.apache.cayenne.query;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SelectQueryCacheKeyTest extends ServerCase {

    @Inject
    private EntityResolver resolver;

    public void testNoCache() {

        SelectQuery query = new SelectQuery(Artist.class);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.getCacheStrategy());
        assertNull(md1.getCacheKey());

        query.setName("XYZ");
        QueryMetadata md2 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.getCacheStrategy());
        assertNull(md2.getCacheKey());
    }

    public void testLocalCache() {

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);

        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    public void testUseLocalCache() {

        SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class);
        q1.useLocalCache();

        QueryMetadata md1 = q1.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
        assertEquals(0, md1.getCacheGroups().length);
        
        SelectQuery<Artist> q2 = new SelectQuery<Artist>(Artist.class);
        q2.useLocalCache("g1", "g2");

        QueryMetadata md2 = q2.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md2.getCacheStrategy());
        assertNotNull(md2.getCacheKey());
        assertEquals(2, md2.getCacheGroups().length);
    }

    public void testSharedCache() {

        SelectQuery<Artist> query = new SelectQuery<Artist>(Artist.class);

        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }
    
    public void testUseSharedCache() {

        SelectQuery<Artist> q1 = new SelectQuery<Artist>(Artist.class);
        q1.useSharedCache();

        QueryMetadata md1 = q1.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
        assertEquals(0, md1.getCacheGroups().length);
        
        SelectQuery<Artist> q2 = new SelectQuery<Artist>(Artist.class);
        q2.useSharedCache("g1", "g2");

        QueryMetadata md2 = q2.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md2.getCacheStrategy());
        assertNotNull(md2.getCacheKey());
        assertEquals(2, md2.getCacheGroups().length);
    }

    public void testNamedQuery() {

        SelectQuery query = new SelectQuery(Artist.class);

        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query.setName("XYZ");

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertFalse("XYZ".equals(md1.getCacheKey()));
    }

    public void testUniqueKeyEntity() {

        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        SelectQuery q3 = new SelectQuery(Painting.class);
        q3.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(q3.getMetaData(resolver).getCacheKey()));
    }

    public void testUniqueKeyEntityQualifier() {

        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q1.setQualifier(ExpressionFactory.matchExp("a", "b"));

        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q2.setQualifier(ExpressionFactory.matchExp("a", "b"));

        SelectQuery q3 = new SelectQuery(Artist.class);
        q3.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q3.setQualifier(ExpressionFactory.matchExp("a", "c"));

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(q3.getMetaData(resolver).getCacheKey()));
    }

    public void testUniqueKeyEntityFetchLimit() {

        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q1.setFetchLimit(5);

        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q2.setFetchLimit(5);

        SelectQuery q3 = new SelectQuery(Artist.class);
        q3.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        q3.setFetchLimit(6);

        SelectQuery q4 = new SelectQuery(Artist.class);
        q4.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2.getMetaData(resolver).getCacheKey());

        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(q3.getMetaData(resolver).getCacheKey()));
        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(q4.getMetaData(resolver).getCacheKey()));
    }
}
