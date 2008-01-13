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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextNamedQueryCachingTest extends CayenneCase {

    public void testDataContextSharedCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        DataContext context = createDataContext();

        QueryMetadata cacheKey = new MockQueryMetadata() {

            @Override
            public String getCacheKey() {
                return "ParameterizedQueryWithSharedCache";
            }
        };

        assertNull(context.getQueryCache().get(cacheKey));
        context.performQuery("ParameterizedQueryWithSharedCache", false);

        Object cached = getDomain().getQueryCache().get(cacheKey);
        assertEquals(4, ((Collection) cached).size());

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery("ParameterizedQueryWithSharedCache", false);

        assertSame(cached, getDomain().getQueryCache().get(cacheKey));

        // delete one record
        int[] counts = context.performNonSelectingQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (5, 'XX')"));
        assertEquals(1, counts[0]);

        // refresh
        List objects1 = context.performQuery("ParameterizedQueryWithSharedCache", true);

        Object cached1 = getDomain().getQueryCache().get(cacheKey);
        assertNotNull(
                "Failed to cache results of refreshing NamedQuery that points to a caching query",
                cached1);

        assertNotSame("Failed to refresh", cached, cached1);
        assertEquals(5, ((Collection) cached1).size());
        assertEquals(5, objects1.size());
    }

    public void testDataContextLocalCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        DataContext context = createDataContext();

        QueryMetadata cacheKey = new MockQueryMetadata() {

            @Override
            public String getCacheKey() {
                return "ParameterizedQueryWithLocalCache";
            }
        };

        assertNull(context.getQueryCache().get(cacheKey));
        context.performQuery("ParameterizedQueryWithLocalCache", false);

        Object cached = context.getQueryCache().get(cacheKey);

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery("ParameterizedQueryWithLocalCache", false);

        assertSame(cached, context.getQueryCache().get(cacheKey));

        // refresh
        List fetchedCached1 = context.performQuery(
                "ParameterizedQueryWithLocalCache",
                true);

        Object cached1 = context.getQueryCache().get(cacheKey);
        assertNotNull(
                "Failed to cache results of refreshing NamedQuery that points to a caching query",
                cached1);

        assertNotSame("Failed to refresh", cached, cached1);
        assertSame(cached1, fetchedCached1);

        List fetchedCached2 = context.performQuery(
                "ParameterizedQueryWithLocalCache",
                false);
        assertSame(fetchedCached1, fetchedCached2);
    }

    public void testSharedCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        DataContext context = createDataContext();

        NamedQuery q1 = new NamedQuery("ParameterizedQueryWithSharedCache");
        QueryMetadata cacheKey = q1.getMetaData(context.getEntityResolver());

        assertNull(context.getQueryCache().get(cacheKey));
        context.performQuery(q1);

        Object cached = getDomain().getQueryCache().get(cacheKey);

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery(q1);

        assertSame(cached, getDomain().getQueryCache().get(cacheKey));

        // refresh
        q1.setForceNoCache(true);

        context.performQuery(q1);

        Object cached1 = getDomain().getQueryCache().get(cacheKey);
        assertNotNull(
                "Failed to cache results of refreshing NamedQuery that points to a caching query",
                cached1);

        assertNotSame("Failed to refresh", cached, cached1);
    }

    public void testLocalCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        DataContext context = createDataContext();

        NamedQuery q1 = new NamedQuery("ParameterizedQueryWithLocalCache");
        QueryMetadata cacheKey = q1.getMetaData(context.getEntityResolver());

        assertNull(context.getQueryCache().get(cacheKey));
        context.performQuery(q1);

        Object cached = context.getQueryCache().get(cacheKey);

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery(q1);

        assertSame(cached, context.getQueryCache().get(cacheKey));

        // refresh
        q1.setForceNoCache(true);

        context.performQuery(q1);

        Object cached1 = context.getQueryCache().get(cacheKey);
        assertNotNull(
                "Failed to cache results of refreshing NamedQuery that points to a caching query",
                cached1);

        assertNotSame("Failed to refresh", cached, cached1);
    }

    public void testLocalCacheWithParameters() throws Exception {
        deleteTestData();
        createTestData("prepare");

        NamedQuery q1 = new NamedQuery("ParameterizedQueryWithLocalCache", new String[] {
            "name"
        }, new Object[] {
            "AA%"
        });

        DataContext context = createDataContext();

        List objects1 = context.performQuery(q1);

        NamedQuery q2 = new NamedQuery("ParameterizedQueryWithLocalCache", new String[] {
            "name"
        }, new Object[] {
            "BB%"
        });

        List objects2 = context.performQuery(q2);

        assertFalse("Incorrect results retrieved from cache - "
                + "changed parameter set warrants a different cache key", objects1
                .equals(objects2));

        NamedQuery q3 = new NamedQuery("ParameterizedQueryWithLocalCache");

        List objects3 = context.performQuery(q3);

        assertFalse("Incorrect results retrieved from cache - "
                + "changed parameter set warrants a different cache key", objects1
                .equals(objects3));
        assertFalse("Incorrect results retrieved from cache - "
                + "changed parameter set warrants a different cache key", objects2
                .equals(objects3));

        blockQueries();

        try {
            // now rerun all queries and see that they are hitting the right cache...
            List objects11 = context.performQuery(q1);
            List objects21 = context.performQuery(q2);
            List objects31 = context.performQuery(q3);

            assertEquals(objects1, objects11);
            assertEquals(objects2, objects21);
            assertEquals(objects3, objects31);
        }
        finally {
            unblockQueries();
        }

    }

}
