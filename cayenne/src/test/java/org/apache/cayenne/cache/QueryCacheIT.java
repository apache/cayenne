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
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class QueryCacheIT extends RuntimeCase {

    @Inject
    private ObjectContext context1;
    
    @Inject
    private ObjectContext context2;

    @Test
    public void testLocalCache() {
        
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
    
    @Test(expected = UnsupportedOperationException.class)
    public void testLocalCacheStoresAnImmutableList() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).localCache();
        List<Artist> result1 = context1.performQuery(q);
        assertEquals(1, result1.size());
        
        // Mutate the returned list. This should not change the cache.
        result1.add(context1.newObject(Artist.class));
        List<Artist> result2 = context1.performQuery(q);
        assertEquals("the list stored in the local query cache cannot be mutated after being returned", 1, result2.size());

        result2.add(context1.newObject(Artist.class));
        List<Artist> result3 = context1.performQuery(q);
        assertEquals("the list stored in the local query cache cannot be mutated after being returned", 1, result3.size());
    }
    
    @Test
    public void testSharedCacheStoresAnImmutableList() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).sharedCache();
        List<Artist> result1 = context1.performQuery(q);
        assertEquals(1, result1.size());
        
        // Mutate the returned list. This should not change the cache.
        result1.add(context1.newObject(Artist.class));
        List<Artist> result2 = context1.performQuery(q);
        assertEquals("the list stored in the shared query cache cannot be mutated after being returned", 1, result2.size());
    }
    
    @Test
    public void testLocalCacheRerunDoesntClobberNewerInMemoryState() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).localCache(); // LOCAL CACHE
        Artist result1 = query.selectFirst(context1);
        assertEquals("should populate shared cache", "artist", result1.getArtistName());
        
        a.setArtistName("modified"); // change the name in memory, and on disk
        context1.commitChanges();
        
        Artist result2 = ObjectSelect.query(Artist.class).selectFirst(context1);
        assertEquals("should be no cache used", "modified", result2.getArtistName());
        
        Artist result3 = query.selectFirst(context1);
        assertEquals("should use shared cache, but shouldn't wipe up newer in-memory data", "modified", result3.getArtistName());
    }
    
    @Test
    public void testSharedCacheRerunDoesntClobberNewerInMemoryState() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).sharedCache(); // SHARED CACHE
        Artist result1 = query.selectFirst(context1);
        assertEquals("should populate shared cache", "artist", result1.getArtistName());
        
        a.setArtistName("modified"); // change the name in memory, and on disk
        context1.commitChanges();
        
        Artist result2 = ObjectSelect.query(Artist.class).selectFirst(context1);
        assertEquals("should be no cache used", "modified", result2.getArtistName());
        
        Artist result3 = query.selectFirst(context1);
        assertEquals("should use shared cache, but shouldn't wipe out newer in-memory data", "modified", result3.getArtistName());
    }
    
    @Test
    public void testSharedCacheWithPrefetchDoesntClobberNewerInMemoryState() {
        
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
        assertEquals("should populate shared cache", "artist", result1.getToArtist().getArtistName());
        
        // Modify the artist name in memory and on disk
        a.setArtistName("modified");
        context1.commitChanges();
        
        // Non-cached query should see the change
        Painting result2 = ObjectSelect.query(Painting.class)
                .prefetch(Painting.TO_ARTIST.disjoint())
                .selectFirst(context1);
        assertEquals("should be no cache used", "modified", result2.getToArtist().getArtistName());
        
        // Re-run cached query with prefetch - should still see the newer data
        Painting result3 = query.selectFirst(context1);
        assertEquals("should use shared cache with prefetch, but shouldn't wipe out newer in-memory data", 
                "modified", result3.getToArtist().getArtistName());
    }
    
    @Test
    public void testSharedCacheWithPrefetchDoesntClobberNewerPrefetchedObjectState() {
        
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
        assertEquals("should populate shared cache", "artist", artist1.getArtistName());
        
        // Modify the prefetched artist object directly (simulate in-memory change)
        artist1.setArtistName("modified");
        context1.commitChanges();

        // Re-run cached query - should NOT clobber the in-memory modification
        Painting result2 = query.selectFirst(context1);
        Artist artist2 = result2.getToArtist();
        
        // Should be the same object instances (from context)
        assertEquals("should be same painting object in context", result1, result2);
        assertEquals("should be same artist object in context", artist1, artist2);
        assertEquals("cached query should not clobber newer in-memory state of prefetched object", 
                "modified", artist2.getArtistName());
    }
}
