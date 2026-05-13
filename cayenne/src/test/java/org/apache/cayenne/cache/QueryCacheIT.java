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
package org.apache.cayenne.cache;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class QueryCacheIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private ObjectContext context1;
    private ObjectContext context2;


    @BeforeEach
    public void setUp() {
        context1 = env.context();
        context2 = env.runtime().newContext();
    }

    @Test
    public void localCache() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).localCache();
        List<Artist> result1 = q.select(context1);
        List<Artist> result2 = q.select(context2);
        
        assertNotSame(
                result1.get(0).getObjectContext(), 
                result2.get(0).getObjectContext());
    }
    
    @Test
    public void localCacheStoresAnImmutableList() {

        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();

        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).localCache();
        List<Artist> result1 = context1.performQuery(q);
        assertEquals(1, result1.size());

        // Mutate the returned list. This should not change the cache.
        assertThrows(UnsupportedOperationException.class, () -> result1.add(context1.newObject(Artist.class)));
        List<Artist> result2 = context1.performQuery(q);
        assertEquals(1, result2.size(), "the list stored in the local query cache cannot be mutated after being returned");

        assertThrows(UnsupportedOperationException.class, () -> result2.add(context1.newObject(Artist.class)));
        List<Artist> result3 = context1.performQuery(q);
        assertEquals(1, result3.size(), "the list stored in the local query cache cannot be mutated after being returned");
    }
    
    @Test
    public void sharedCacheStoresAnImmutableList() {

        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();

        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).sharedCache();
        List<Artist> result1 = context1.performQuery(q);
        assertEquals(1, result1.size());

        // Mutate the returned list. This should not change the cache.
        try {
            result1.add(context1.newObject(Artist.class));
        } catch (UnsupportedOperationException ignored) {
            // list may be immutable depending on cache implementation
        }
        List<Artist> result2 = context1.performQuery(q);
        assertEquals(1, result2.size(), "the list stored in the shared query cache cannot be mutated after being returned");
    }
    
    @Test
    public void localCacheRerunDoesntClobberNewerInMemoryState() {

        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).localCache(); // LOCAL CACHE
        Artist result1 = query.selectFirst(context1);
        assertEquals("artist", result1.getArtistName(), "should populate shared cache");

        a.setArtistName("modified"); // change the name in memory, and on disk
        context1.commitChanges();

        Artist result2 = ObjectSelect.query(Artist.class).selectFirst(context1);
        assertEquals("modified", result2.getArtistName(), "should be no cache used");

        Artist result3 = query.selectFirst(context1);
        assertEquals("modified", result3.getArtistName(), "should use shared cache, but shouldn't wipe up newer in-memory data");
    }
    
    @Test
    public void sharedCacheRerunDoesntClobberNewerInMemoryState() {

        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).sharedCache(); // SHARED CACHE
        Artist result1 = query.selectFirst(context1);
        assertEquals("artist", result1.getArtistName(), "should populate shared cache");

        a.setArtistName("modified"); // change the name in memory, and on disk
        context1.commitChanges();

        Artist result2 = ObjectSelect.query(Artist.class).selectFirst(context1);
        assertEquals("modified", result2.getArtistName(), "should be no cache used");

        Artist result3 = query.selectFirst(context1);
        assertEquals("modified", result3.getArtistName(), "should use shared cache, but shouldn't wipe out newer in-memory data");
    }
    
    @Test
    public void sharedCacheWithPrefetchDoesntClobberNewerInMemoryState() {

        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");

        Painting p = context1.newObject(Painting.class);
        p.setPaintingTitle("painting");
        p.setToArtist(a);

        context1.commitChanges();

        // Query with shared cache AND prefetch
        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .sharedCache();

        Painting result1 = query.selectFirst(context1);
        assertEquals("artist", result1.getToArtist().getArtistName(), "should populate shared cache");

        // Modify the artist name in memory and on disk
        a.setArtistName("modified");
        context1.commitChanges();

        // Non-cached query should see the change
        Painting result2 = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .selectFirst(context1);
        assertEquals("modified", result2.getToArtist().getArtistName(), "should be no cache used");

        // Re-run cached query with prefetch - should still see the newer data
        Painting result3 = query.selectFirst(context1);
        assertEquals("modified", result3.getToArtist().getArtistName(),
                "should use shared cache with prefetch, but shouldn't wipe out newer in-memory data");
    }
    
    @Test
    public void sharedCacheWithPrefetchDoesntClobberNewerPrefetchedObjectState() {

        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");

        Painting p = context1.newObject(Painting.class);
        p.setPaintingTitle("painting");
        p.setToArtist(a);

        context1.commitChanges();

        // Query with shared cache AND prefetch to load the artist
        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .sharedCache();

        Painting result1 = query.selectFirst(context1);
        Artist artist1 = result1.getToArtist();
        assertEquals("artist", artist1.getArtistName(), "should populate shared cache");

        // Modify the prefetched artist object directly (simulate in-memory change)
        artist1.setArtistName("modified");
        context1.commitChanges();

        // Re-run cached query - should NOT clobber the in-memory modification
        Painting result2 = query.selectFirst(context1);
        Artist artist2 = result2.getToArtist();

        // Should be the same object instances (from context)
        assertEquals(result1, result2, "should be same painting object in context");
        assertEquals(artist1, artist2, "should be same artist object in context");
        assertEquals("modified", artist2.getArtistName(),
                "cached query should not clobber newer in-memory state of prefetched object");
    }
}
