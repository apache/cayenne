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

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.query.resolved.ResolvedProcedure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ProcedureQueryCacheKeyIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void noCache() {

        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        ProcedureQuery<Artist> query = ProcedureQuery.query("cayenne_tst_select_proc", Artist.class);

        ResolvedProcedure md1 = query.resolve(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.cacheStrategy());
        assertNull(md1.cacheKey());

        ResolvedProcedure md2 = query.resolve(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.cacheStrategy());
        assertNull(md2.cacheKey());
    }

    @Test
    public void localCache() {

        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        ProcedureQuery<Artist> query = ProcedureQuery.query("cayenne_tst_select_proc", Artist.class).cacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        ResolvedProcedure md1 = query.resolve(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.cacheStrategy());
        assertNotNull(md1.cacheKey());
    }

    @Test
    public void sharedCache() {

        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        ProcedureQuery<Artist> query = ProcedureQuery.query("cayenne_tst_select_proc", Artist.class).cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        ResolvedProcedure md1 = query.resolve(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.cacheStrategy());
        assertNotNull(md1.cacheKey());
    }

    @Test
    public void namedQuery() {

        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();

        ProcedureQuery<Artist> query = ProcedureQuery.query("cayenne_tst_select_proc", Artist.class).cacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        ResolvedProcedure md1 = query.resolve(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.cacheStrategy());
        assertEquals("proc:cayenne_tst_select_proc", md1.cacheKey());
    }

    @Test
    public void cacheFetchOffsetAndLimit() {
        EntityResolver resolver = env.runtime().getDataDomain().getEntityResolver();
        
        ProcedureQuery<Artist> q1 = ProcedureQuery.query("cayenne_tst_select_proc", Artist.class).cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        q1.offset(5).limit(3);
        
        ProcedureQuery<Artist> q2 = ProcedureQuery.query("cayenne_tst_select_proc", Artist.class).cacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        q2.offset(5).limit(3);
        
        assertEquals(q1.resolve(resolver).cacheKey(), q2.resolve(resolver).cacheKey());
    }
}
