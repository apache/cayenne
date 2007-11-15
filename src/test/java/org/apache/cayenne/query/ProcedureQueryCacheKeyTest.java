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

import org.apache.art.Artist;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.CayenneCase;

public class ProcedureQueryCacheKeyTest extends CayenneCase {

    public void testNoCache() {

        EntityResolver resolver = getDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.NO_CACHE, md1.getCachePolicy());
        assertNull(md1.getCacheKey());

        query.setName("XYZ");
        QueryMetadata md2 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.NO_CACHE, md2.getCachePolicy());
        assertNull(md2.getCacheKey());
    }

    public void testLocalCache() {

        EntityResolver resolver = getDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        query.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.LOCAL_CACHE, md1.getCachePolicy());
        assertNotNull(md1.getCacheKey());
    }

    public void testSharedCache() {

        EntityResolver resolver = getDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        query.setCachePolicy(QueryMetadata.SHARED_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.SHARED_CACHE, md1.getCachePolicy());
        assertNotNull(md1.getCacheKey());
    }

    public void testNamedQuery() {

        EntityResolver resolver = getDomain().getEntityResolver();

        ProcedureQuery query = new ProcedureQuery("ABC", Artist.class);

        query.setCachePolicy(QueryMetadata.SHARED_CACHE);
        query.setName("XYZ");

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.SHARED_CACHE, md1.getCachePolicy());
        assertEquals("XYZ", md1.getCacheKey());
    }

}
