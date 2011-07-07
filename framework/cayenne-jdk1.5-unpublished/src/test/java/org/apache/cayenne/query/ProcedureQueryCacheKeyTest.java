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

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class ProcedureQueryCacheKeyTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    public void testNoCache() {

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.getCacheStrategy());
        assertNull(md1.getCacheKey());

        query.setName("XYZ");
        QueryMetadata md2 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.getCacheStrategy());
        assertNull(md2.getCacheKey());
    }

    public void testLocalCache() {

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        query.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    public void testSharedCache() {

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    public void testNamedQuery() {

        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query.setName("XYZ");

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertEquals("proc:", md1.getCacheKey());
    }

    public void testCacheFetchOffsetAndLimit() {
        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();
        
        ProcedureQuery q1 = new ProcedureQuery("ABC", Artist.class);
        q1.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        q1.setFetchOffset(5);
        q1.setFetchLimit(3);
        
        ProcedureQuery q2 = new ProcedureQuery("ABC", Artist.class);
        q2.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        q2.setFetchOffset(5);
        q2.setFetchLimit(3);
        
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());
    }
}
