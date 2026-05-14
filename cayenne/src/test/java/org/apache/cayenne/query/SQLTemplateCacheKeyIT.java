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
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SQLTemplateCacheKeyIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void noCache() {

        SQLTemplate query = new SQLTemplate(Artist.class, "SELECT ME");

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.getCacheStrategy());
        assertNull(md1.getCacheKey());

        QueryMetadata md2 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.getCacheStrategy());
        assertNull(md2.getCacheKey());
    }

    @Test
    public void localCache() {

        SQLTemplate query = new SQLTemplate(Artist.class, "SELECT ME");

        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    @Test
    public void sharedCache() {

        SQLTemplate query = new SQLTemplate(Artist.class, "SELECT ME");

        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    @Test
    public void namedQuery() {

        SQLTemplate query = new SQLTemplate(Artist.class, "SELECT ME");

        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertFalse("XYZ".equals(md1.getCacheKey()));
    }

    @Test
    public void cacheFetchLimitAndOffset() {
        SQLTemplate q1 = new SQLTemplate(Artist.class, "SELECT ME");
        q1.setFetchOffset(5);
        q1.setFetchLimit(10);
        q1.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        
        SQLTemplate q2 = new SQLTemplate(Artist.class, "SELECT ME");
        q2.setFetchOffset(5);
        q2.setFetchLimit(10);
        q2.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        assertEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q2
                .getMetaData(env.entityResolver())
                .getCacheKey());
    }
}
