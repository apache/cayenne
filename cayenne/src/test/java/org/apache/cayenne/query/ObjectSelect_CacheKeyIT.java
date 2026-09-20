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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectSelect_CacheKeyIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void noCache() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class);

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.NO_CACHE, md1.getCacheStrategy());
        assertNull(md1.getCacheKey());

        QueryMetadata md2 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.NO_CACHE, md2.getCacheStrategy());
        assertNull(md2.getCacheKey());
    }

    @Test
    public void localCache() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .localCache();

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    @Test
    public void useLocalCache() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache();

        QueryMetadata md1 = q1.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
        assertNull(md1.getCacheGroup());

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class);
        q2.useLocalCache("g1");

        QueryMetadata md2 = q2.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.LOCAL_CACHE, md2.getCacheStrategy());
        assertNotNull(md2.getCacheKey());
        assertEquals("g1", md2.getCacheGroup());
    }

    @Test
    public void sharedCache() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .sharedCache();

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
    }

    @Test
    public void useSharedCache() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .sharedCache();

        QueryMetadata md1 = q1.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotNull(md1.getCacheKey());
        assertNull(md1.getCacheGroup());

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .sharedCache("g1");

        QueryMetadata md2 = q2.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md2.getCacheStrategy());
        assertNotNull(md2.getCacheKey());
        assertEquals("g1", md2.getCacheGroup());
    }

    @Test
    public void namedQuery() {

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .sharedCache();

        QueryMetadata md1 = query.getMetaData(env.entityResolver());
        assertEquals(QueryCacheStrategy.SHARED_CACHE, md1.getCacheStrategy());
        assertNotEquals("XYZ", md1.getCacheKey());
    }

    @Test
    public void uniqueKeyEntity() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache();

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache();

        ObjectSelect<Painting> q3 = ObjectSelect.query(Painting.class)
                .localCache();

        assertNotNull(q1.getMetaData(env.entityResolver()).getCacheKey());
        assertEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q2.getMetaData(env.entityResolver()).getCacheKey());

        assertNotEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q3.getMetaData(env.entityResolver()).getCacheKey());
    }

    @Test
    public void uniqueKeyQualifier() {

        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.matchExp("a", "b"));

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.matchExp("a", "b"));

        ObjectSelect<Artist> q3 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.matchExp("a", "c"));

        assertNotNull(q1.getMetaData(env.entityResolver()).getCacheKey());
        assertEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q2.getMetaData(env.entityResolver()).getCacheKey());

        assertNotEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q3.getMetaData(env.entityResolver()).getCacheKey());
    }

    @Test
    public void uniqueKeyFetchLimit() {

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

        assertNotNull(q1.getMetaData(env.entityResolver()).getCacheKey());
        assertEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q2.getMetaData(env.entityResolver()).getCacheKey());

        assertNotEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q3.getMetaData(env.entityResolver()).getCacheKey());
        assertNotEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q4.getMetaData(env.entityResolver()).getCacheKey());
    }

    @Test
    public void uniqueKeyHaving() {

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

        assertNotNull(q1.getMetaData(env.entityResolver()).getCacheKey());
        assertEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q2.getMetaData(env.entityResolver()).getCacheKey());

        assertNotEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q3.getMetaData(env.entityResolver()).getCacheKey());
        assertNotEquals(q1.getMetaData(env.entityResolver()).getCacheKey(), q4.getMetaData(env.entityResolver()).getCacheKey());
    }

    private String cacheKey(FluentSelect<?, ?> query) {
        return query.getMetaData(env.entityResolver()).getCacheKey();
    }

    private static ObjectSelect<Painting> paintingsOver(int price) {
        return ObjectSelect.query(Painting.class)
                .where(Painting.TO_ARTIST.eq(Artist.SELF.enclosing()))
                .and(ExpressionFactory.greaterExp("estimatedPrice", price));
    }

    private static ColumnSelect<String> artistNamesLike(String pattern) {
        return ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME).where(Artist.ARTIST_NAME.like(pattern));
    }

    @Test
    public void uniqueKeyExistsSubquery() {
        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(paintingsOver(5)));

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(paintingsOver(5)));

        ObjectSelect<Artist> differentQualifier = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(paintingsOver(500)));

        ObjectSelect<Artist> differentRoot = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(ObjectSelect.query(Gallery.class)));

        ObjectSelect<Artist> differentLimit = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(paintingsOver(5).limit(1)));

        ObjectSelect<Artist> notExists = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.notExists(paintingsOver(5)));

        assertNotNull(cacheKey(q1));
        assertEquals(cacheKey(q1), cacheKey(q2));

        assertNotEquals(cacheKey(q1), cacheKey(differentQualifier));
        assertNotEquals(cacheKey(q1), cacheKey(differentRoot));
        assertNotEquals(cacheKey(q1), cacheKey(differentLimit));
        assertNotEquals(cacheKey(q1), cacheKey(notExists));
    }

    @Test
    public void uniqueKeyInSubquery() {
        ObjectSelect<Painting> q1 = ObjectSelect.query(Painting.class)
                .localCache()
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).in(artistNamesLike("a%")));

        ObjectSelect<Painting> q2 = ObjectSelect.query(Painting.class)
                .localCache()
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).in(artistNamesLike("a%")));

        ObjectSelect<Painting> differentQualifier = ObjectSelect.query(Painting.class)
                .localCache()
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).in(artistNamesLike("b%")));

        ObjectSelect<Painting> differentColumn = ObjectSelect.query(Painting.class)
                .localCache()
                .where(Painting.TO_ARTIST.dot(Artist.ARTIST_NAME).in(ObjectSelect
                        .columnQuery(Artist.class, Artist.ARTIST_NAME.upper())
                        .where(Artist.ARTIST_NAME.like("a%"))));

        assertEquals(cacheKey(q1), cacheKey(q2));
        assertNotEquals(cacheKey(q1), cacheKey(differentQualifier));
        assertNotEquals(cacheKey(q1), cacheKey(differentColumn));
    }

    @Test
    public void uniqueKeyAllAndAnySubqueries() {
        ObjectSelect<Artist> all1 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.greaterExp("artistName", ExpressionFactory.all(artistNamesLike("a%"))));

        ObjectSelect<Artist> all2 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.greaterExp("artistName", ExpressionFactory.all(artistNamesLike("b%"))));

        ObjectSelect<Artist> any1 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.greaterExp("artistName", ExpressionFactory.any(artistNamesLike("a%"))));

        assertNotEquals(cacheKey(all1), cacheKey(all2));
        assertNotEquals(cacheKey(all1), cacheKey(any1));
    }

    @Test
    public void uniqueKeySubqueryInHavingAndColumnQuery() {
        ColumnSelect<String> q1 = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME)
                .localCache()
                .having(ExpressionFactory.exists(paintingsOver(5)));

        ColumnSelect<String> q2 = ObjectSelect.columnQuery(Artist.class, Artist.ARTIST_NAME)
                .localCache()
                .having(ExpressionFactory.exists(paintingsOver(500)));

        assertNotEquals(cacheKey(q1), cacheKey(q2));
    }

    @Test
    public void uniqueKeyNestedSubqueries() {
        ObjectSelect<Artist> q1 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(ObjectSelect.query(Painting.class)
                        .where(ExpressionFactory.exists(ObjectSelect.query(Gallery.class)
                                .where(Gallery.GALLERY_NAME.eq("g1"))))));

        ObjectSelect<Artist> q2 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(ObjectSelect.query(Painting.class)
                        .where(ExpressionFactory.exists(ObjectSelect.query(Gallery.class)
                                .where(Gallery.GALLERY_NAME.eq("g2"))))));

        assertNotEquals(cacheKey(q1), cacheKey(q2));
    }

    @Test
    public void cachedResultsOfDifferentSubqueries() throws Exception {
        TableHelper tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
        tArtist.insert(1, "artist1");
        tArtist.insert(2, "artist2");

        TableHelper tPainting = env.table("PAINTING", "PAINTING_ID", "PAINTING_TITLE", "ARTIST_ID", "ESTIMATED_PRICE");
        tPainting.insert(1, "cheap", 1, 10);
        tPainting.insert(2, "expensive", 2, 1000);

        ObjectContext context = env.context();

        List<Artist> over5 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(paintingsOver(5)))
                .select(context);
        assertEquals(2, over5.size());

        List<Artist> over500 = ObjectSelect.query(Artist.class)
                .localCache()
                .where(ExpressionFactory.exists(paintingsOver(500)))
                .select(context);
        assertEquals(1, over500.size());
        assertEquals("artist2", over500.get(0).getArtistName());
    }
}
