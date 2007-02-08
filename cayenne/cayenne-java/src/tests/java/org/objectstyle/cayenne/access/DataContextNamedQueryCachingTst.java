/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.List;

import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class DataContextNamedQueryCachingTst extends CayenneTestCase {

    public void testDataContextSharedCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        DataContext context = createDataContext();

        String cacheKey = "ParameterizedQueryWithSharedCache";

        assertNull(context.getObjectStore().getCachedQueryResult(cacheKey));
        context.performQuery("ParameterizedQueryWithSharedCache", false);

        Object cached = getDomain().getSharedSnapshotCache().getCachedSnapshots(cacheKey);
        assertEquals(4, ((Collection) cached).size());

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery("ParameterizedQueryWithSharedCache", false);

        assertSame(cached, getDomain().getSharedSnapshotCache().getCachedSnapshots(
                cacheKey));

        // delete one record
        int[] counts = context.performNonSelectingQuery(new SQLTemplate(
                Artist.class,
                "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME) VALUES (5, 'XX')"));
        assertEquals(1, counts[0]);

        // refresh
        List objects1 = context.performQuery("ParameterizedQueryWithSharedCache", true);

        Object cached1 = getDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshots(cacheKey);
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

        String cacheKey = "ParameterizedQueryWithLocalCache";

        assertNull(context.getObjectStore().getCachedQueryResult(cacheKey));
        context.performQuery("ParameterizedQueryWithLocalCache", false);

        Object cached = context.getObjectStore().getCachedQueryResult(cacheKey);

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery("ParameterizedQueryWithLocalCache", false);

        assertSame(cached, context.getObjectStore().getCachedQueryResult(cacheKey));

        // refresh
        List fetchedCached1 = context.performQuery(
                "ParameterizedQueryWithLocalCache",
                true);

        Object cached1 = context.getObjectStore().getCachedQueryResult(cacheKey);
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
        String cacheKey = q1.getMetaData(context.getEntityResolver()).getCacheKey();

        assertNull(context.getObjectStore().getCachedQueryResult(cacheKey));
        context.performQuery(q1);

        Object cached = getDomain().getSharedSnapshotCache().getCachedSnapshots(cacheKey);

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery(q1);

        assertSame(cached, getDomain().getSharedSnapshotCache().getCachedSnapshots(
                cacheKey));

        // refresh
        q1.setForceNoCache(true);

        context.performQuery(q1);

        Object cached1 = getDomain()
                .getSharedSnapshotCache()
                .getCachedSnapshots(cacheKey);
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
        String cacheKey = q1.getMetaData(context.getEntityResolver()).getCacheKey();

        assertNull(context.getObjectStore().getCachedQueryResult(cacheKey));
        context.performQuery(q1);

        Object cached = context.getObjectStore().getCachedQueryResult(cacheKey);

        assertNotNull(
                "Failed to cache results of a NamedQuery that points to a caching query",
                cached);

        // get from cache
        context.performQuery(q1);

        assertSame(cached, context.getObjectStore().getCachedQueryResult(cacheKey));

        // refresh
        q1.setForceNoCache(true);

        context.performQuery(q1);

        Object cached1 = context.getObjectStore().getCachedQueryResult(cacheKey);
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
