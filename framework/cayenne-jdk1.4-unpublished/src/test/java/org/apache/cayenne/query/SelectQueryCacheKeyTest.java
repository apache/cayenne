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
import org.apache.art.Painting;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.unit.CayenneCase;

public class SelectQueryCacheKeyTest extends CayenneCase {

    public void testNoCache() {

        EntityResolver resolver = getDomain().getEntityResolver();

        SelectQuery query = new SelectQuery(Artist.class);

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

        SelectQuery query = new SelectQuery(Artist.class);

        query.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.LOCAL_CACHE, md1.getCachePolicy());
        assertNotNull(md1.getCacheKey());
    }

    public void testSharedCache() {

        EntityResolver resolver = getDomain().getEntityResolver();

        SelectQuery query = new SelectQuery(Artist.class);

        query.setCachePolicy(QueryMetadata.SHARED_CACHE);

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.SHARED_CACHE, md1.getCachePolicy());
        assertNotNull(md1.getCacheKey());
    }

    public void testNamedQuery() {

        EntityResolver resolver = getDomain().getEntityResolver();

        SelectQuery query = new SelectQuery(Artist.class);

        query.setCachePolicy(QueryMetadata.SHARED_CACHE);
        query.setName("XYZ");

        QueryMetadata md1 = query.getMetaData(resolver);
        assertEquals(QueryMetadata.SHARED_CACHE, md1.getCachePolicy());
        assertEquals("XYZ", md1.getCacheKey());
    }

    public void testUniqueKeyEntity() {

        EntityResolver resolver = getDomain().getEntityResolver();

        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        SelectQuery q3 = new SelectQuery(Painting.class);
        q3.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());

        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(
                q3.getMetaData(resolver).getCacheKey()));
    }

    public void testUniqueKeyEntityQualifier() {

        EntityResolver resolver = getDomain().getEntityResolver();

        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q1.setQualifier(ExpressionFactory.matchExp("a", "b"));

        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q2.setQualifier(ExpressionFactory.matchExp("a", "b"));

        SelectQuery q3 = new SelectQuery(Artist.class);
        q3.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q3.setQualifier(ExpressionFactory.matchExp("a", "c"));

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());

        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(
                q3.getMetaData(resolver).getCacheKey()));
    }

    public void testUniqueKeyEntityFetchLimit() {

        EntityResolver resolver = getDomain().getEntityResolver();

        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q1.setFetchLimit(5);

        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q2.setFetchLimit(5);

        SelectQuery q3 = new SelectQuery(Artist.class);
        q3.setCachePolicy(QueryMetadata.LOCAL_CACHE);
        q3.setFetchLimit(6);

        SelectQuery q4 = new SelectQuery(Artist.class);
        q4.setCachePolicy(QueryMetadata.LOCAL_CACHE);

        assertNotNull(q1.getMetaData(resolver).getCacheKey());
        assertEquals(q1.getMetaData(resolver).getCacheKey(), q2
                .getMetaData(resolver)
                .getCacheKey());

        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(
                q3.getMetaData(resolver).getCacheKey()));
        assertFalse(q1.getMetaData(resolver).getCacheKey().equals(
                q4.getMetaData(resolver).getCacheKey()));
    }
}
